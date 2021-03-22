import {idiom, ng, notify, template, angular} from 'entcore';
import {Form, Question, QuestionChoice, Questions, Types} from "../models";
import {formService, questionChoiceService, questionService} from "../services";
import {Direction, FORMULAIRE_QUESTION_EMIT_EVENT} from "../core/enums";

interface ViewModel {
    types: typeof Types;
    form: Form;
    questions: Questions;
    newQuestion: Question;
    dontSave: boolean;
    direction: typeof Direction;
    display: {
        lightbox: {
            newQuestion: boolean,
            reorganization: boolean,
            delete: boolean,
            undo: boolean
        }
    };

    createNewQuestion(): void;
    doCreateNewQuestion(code: number): void;
    organizeQuestions(): void;
    doOrganizeQuestions(): Promise<void>;
    saveAll(): Promise<void>;
    return(): void;
    duplicateQuestion(): void;
    deleteQuestion(): void;
    doDeleteQuestion(): void;
    cancelOrganizeQuestions(): void;
    undoQuestionChanges(): void;
    doUndoQuestionChanges(): void;
    createNewChoice(question: Question): void;
    deleteChoice(question: Question, index: number): Promise<void>;
    displayTypeName(typeInfo: string): string;
    displayTypeIcon(code: number): string;
    reOrder(): void;
    moveQuestion(index: number, direction: string): void;
}


export const formEditorController = ng.controller('FormEditorController', ['$scope', '$rootScope',
    function ($scope, $rootScope) {

        const vm: ViewModel = this;
        vm.types = Types;
        vm.form = new Form();
        vm.questions = new Questions();
        vm.newQuestion = new Question();
        vm.dontSave = false;
        vm.direction = Direction;
        vm.display = {
            lightbox: {
                newQuestion: false,
                reorganization: false,
                delete: false,
                undo: false
            }
        };

        const init = async (): Promise<void> => {
            vm.form = $scope.form;
            await vm.questions.sync(vm.form.id);
            vm.newQuestion.form_id = vm.form.id;
            vm.dontSave = false;
            $scope.safeApply();
        };


        // Global functions

        vm.createNewQuestion = () => {
            template.open('lightbox', 'lightbox/question-new');
            vm.display.lightbox.newQuestion = true;
            $scope.safeApply();
        };

        vm.doCreateNewQuestion = async (code: number) => {
            vm.dontSave = true;
            vm.newQuestion = new Question();
            vm.newQuestion.form_id = vm.form.id;
            vm.newQuestion.question_type = code;
            vm.newQuestion.position = vm.questions.all.length + 1;
            if (vm.newQuestion.question_type === Types.MULTIPLEANSWER || vm.newQuestion.question_type === Types.SINGLEANSWER) {
                for (let i = 0; i < 3; i++) {
                    vm.newQuestion.choices.all.push(new QuestionChoice());
                }
            }
            await vm.questions.all.push(vm.newQuestion);
            vm.display.lightbox.newQuestion = false;
            template.close('lightbox');
            vm.dontSave = false;
            $scope.safeApply();
        };

        vm.organizeQuestions = () : void => {
            template.open('lightbox', 'lightbox/questions-reorganization');
            vm.display.lightbox.reorganization = true;
            $scope.safeApply();
        };

        vm.doOrganizeQuestions = async () : Promise<void> => {
            // Reorganization stuff
            await saveQuestions();
            vm.display.lightbox.reorganization = false;
            template.close('lightbox');
            $scope.safeApply();
        };

        vm.cancelOrganizeQuestions = async () : Promise<void> => {
            await vm.questions.sync(vm.form.id);
            vm.display.lightbox.reorganization = false;
            template.close('lightbox');
            $scope.safeApply();
        };

        vm.saveAll = async () : Promise<void> => {
            vm.dontSave = true;
            let wrongQuestions = vm.questions.filter(question => !!!question.title); // TODO check more than just titles later
            if (wrongQuestions.length > 0) {
                notify.error(idiom.translate('formulaire.question.save.missing.field'));
            }
            await saveQuestions(wrongQuestions.length <= 0);
            vm.dontSave = false;
        };

        vm.return = () : void => {
            vm.dontSave = true;
            let wrongQuestions = vm.questions.filter(question => !!!question.title); // TODO check more than just titles later
            if (wrongQuestions.length > 0) {
                notify.error(idiom.translate('formulaire.question.save.missing.field'));
                vm.dontSave = false;
            } else {
                $scope.redirectTo('/list/mine');
            }
        };

        // Question functions

        vm.duplicateQuestion = async () => {
            try {
                vm.dontSave = true;
                let question = vm.questions.selected[0];
                await questionService.save(question);
                for (let i = question.position; i < vm.questions.all.length; i++) {
                    vm.questions.all[i].position++;
                    await questionService.save(vm.questions.all[i]);
                }
                let duplicata = question;
                duplicata.position++;
                let newQuestion = $scope.getDataIf200(await questionService.create(duplicata));
                if (question.question_type === Types.SINGLEANSWER || question.question_type === Types.MULTIPLEANSWER) {
                    for (let choice of question.choices.all) {
                        if (!!choice.value) {
                            await questionChoiceService.create(new QuestionChoice(newQuestion.id, choice.value));
                        }
                    }
                }
                notify.success(idiom.translate('formulaire.success.question.duplicate'));
                await vm.questions.sync(vm.form.id);
                vm.dontSave = false;
                $scope.safeApply();
            }
            catch (e) {
                throw e;
            }
        };

        vm.deleteQuestion = () => {
            vm.dontSave = true;
            template.open('lightbox', 'lightbox/question-confirm-delete');
            vm.display.lightbox.delete = true;
        };

        vm.doDeleteQuestion = async () => {
            try {
                let question = vm.questions.selected[0];
                if (!!question.id) {
                    await questionService.delete(question.id);
                }
                template.close('lightbox');
                vm.display.lightbox.delete = false;
                notify.success(idiom.translate('formulaire.success.question.delete'));
                await vm.questions.sync(vm.form.id);
                rePositionQuestions();
                vm.dontSave = false;
                $scope.safeApply();
            }
            catch (e) {
                throw e;
            }
        };

        vm.undoQuestionChanges = async () => {
            if (vm.questions.selected.length > 0) {
                let question = vm.questions.selected[0];
                if (!!question.title || !!question.statement || question.mandatory || question.choices.all.length > 0) {
                    vm.dontSave = true;
                    template.open('lightbox', 'lightbox/question-confirm-undo');
                    vm.display.lightbox.undo = true;
                }
                await vm.questions.sync(vm.form.id);
                $scope.safeApply();
            }
        };

        vm.doUndoQuestionChanges = async () => {
            await vm.questions.sync(vm.form.id);
            template.close('lightbox');
            vm.display.lightbox.undo = true;
            vm.dontSave = false;
            $scope.safeApply();
        };


        // Display functions

        vm.displayTypeName = (typeInfo: string|number) : string => {
            if (typeof typeInfo === "string") {
                return idiom.translate('formulaire.question.type.' + typeInfo);
            }
            else if (typeof typeInfo === "number") {
                let name = $scope.getTypeNameByCode(typeInfo);
                return idiom.translate('formulaire.question.type.' + name);
            }
            else {
                return "ERROR_TEXT";
            }
        };

        vm.displayTypeIcon = (code: number) : string => {
            switch (code) {
                case 1 :
                    return "/formulaire/public/img/question_type/long-answer.svg";
                case 2 :
                    return "/formulaire/public/img/question_type/short-answer.svg";
                case 3 :
                    return "/formulaire/public/img/question_type/free-text.svg";
                case 4 :
                    return "/formulaire/public/img/question_type/unic-answer.svg";
                case 5 :
                    return "/formulaire/public/img/question_type/multiple-answer.svg";
                case 6 :
                    return "/formulaire/public/img/question_type/date.svg";
                case 7 :
                    return "/formulaire/public/img/question_type/time.svg";
                case 8 :
                    return "/formulaire/public/img/question_type/file.svg";
            }
        };

        vm.reOrder = (): void => {
            let finished = true;
            angular.forEach(vm.questions.all, function (question) {
                if (question.position != parseFloat(question.index) + 1) {
                    question.position = parseFloat(question.index) + 1;
                }
                if (!!!question.position) { finished = false }
            });
            if (finished) {
                rePositionQuestions();
            }
        };

        vm.moveQuestion = (index: number, direction: string): void => {
            switch (direction) {
                case Direction.UP: {
                    vm.questions.all[index].position--;
                    vm.questions.all[index - 1].position++;
                    break;
                }
                case Direction.DOWN: {
                    vm.questions.all[index].position++;
                    vm.questions.all[index + 1].position--;
                    break;
                }
                default:
                    notify.error(idiom.translate('formulaire.error.question.reorganization'));
                    break;
            }
            rePositionQuestions();
            $scope.safeApply();
        };

        // Utils

        const rePositionQuestions = () => {
            vm.questions.all.sort((a, b) => a.position - b.position);
            for (let i = 0; i < vm.questions.all.length; i++) {
                vm.questions.all[i].position = i + 1;
            }
            $scope.safeApply();
        };

        const saveQuestions = async (displaySuccess:boolean = false) => {
            try {
                rePositionQuestions();
                for (let question of vm.questions.all) {
                    if (!!!question.title && !!!question.statement && !question.mandatory && question.choices.all.length <= 0) {
                        if (!!question.id) {
                            question = $scope.getDataIf200(await questionService.get(question.id));
                        }
                    }
                    else {
                        let newId = $scope.getDataIf200(await questionService.save(question)).id;
                        question.id = newId;
                        let registeredChoices = [];
                        for (let choice of question.choices.all) {
                            if (!!choice.value && !registeredChoices.find(c => c === choice.value) ) {
                                choice.question_id = newId;
                                choice.id = $scope.getDataIf200(await questionChoiceService.save(choice)).id;
                                registeredChoices.push(choice.value);
                            }
                        }
                    }
                }
                if (displaySuccess) { notify.success(idiom.translate('formulaire.success.form.save')); }
                vm.form.setFromJson($scope.getDataIf200(await formService.get(vm.form.id)));
                vm.questions.deselectAll();
                $scope.safeApply();
            }
            catch (e) {
                throw e;
            }
        };

        const onClickQuestion = async (event) : Promise<void> => {
            if (!vm.dontSave && $scope.currentPage === 'openForm') {
                let questionPos: number = isInFocusable(event.target);
                if (!!questionPos && questionPos > 0) {
                    let question = vm.questions.all.filter(question => question.position == questionPos)[0];
                    if (!question.selected) {
                        if (vm.questions.selected.length > 0) {
                            await saveQuestions();
                        }
                        // Reselection of the question because the sync has removed the selections
                        vm.questions.all.filter(question => question.position == questionPos)[0].selected = true;
                    }
                }
                else if(!isInDontSave(event.target)) {
                    await saveQuestions();
                }
                $scope.safeApply();
            }
        };

        const isInShowErrorZone = (el): boolean => {
            if (!!!el) { return true; }
            else if (el.classList && el.classList.contains("dontShowError")) { return false; }
            return isInShowErrorZone(el.parentNode);
        };

        const isInFocusable = (el): number => {
            if (!!!el) { return -1; }
            else if (el.classList && el.classList.contains("focusable")) { return el.id; }
            return isInFocusable(el.parentNode);
        };

        const isInDontSave = (el): boolean => {
            if (!!!el) { return false; }
            else if (el.classList && el.classList.contains("dontSave")) { return true; }
            return isInDontSave(el.parentNode);
        };

        init();

        document.onclick = e => { onClickQuestion(e); };

        $rootScope.$on( "$routeChangeSuccess", function(event, next, current) {
            window.location.reload();
        });

        $scope.$on(FORMULAIRE_QUESTION_EMIT_EVENT.DUPLICATE, () => { vm.duplicateQuestion() });
        $scope.$on(FORMULAIRE_QUESTION_EMIT_EVENT.DELETE, () => { vm.deleteQuestion() });
        $scope.$on(FORMULAIRE_QUESTION_EMIT_EVENT.UNDO, () => { vm.undoQuestionChanges() });
    }]);