import {idiom, ng, notify, template} from 'entcore';
import {Form, Question, QuestionChoice, Questions, Types} from "../models";
import {formService, questionChoiceService, questionService} from "../services";
import {DateUtils} from "../utils/date";

interface ViewModel {
    types: typeof Types;
    form: Form;
    questions: Questions;
    newQuestion: Question;
    dontSave: boolean;
    display: {
        lightbox: {
            newQuestion: boolean,
            delete: boolean,
            undo: boolean
        }
    };

    switchAll(value: boolean): void;
    createNewQuestion(): void;
    doCreateNewQuestion(code: number): void;
    saveQuestions(displaySuccess?: boolean): void;
    duplicateQuestion(): void;
    deleteQuestion(): void;
    doDeleteQuestion(): void;
    undoQuestionChanges(): void;
    doUndoQuestionChanges(): void;
    createNewChoice(question: Question): void;
    deleteChoice(question: Question, index: number): Promise<void>;
    displayLastSave(): string;
    displayTypeName(typeInfo: string): string;
    displayTypeIcon(code: number): string;
}


export const formEditorController = ng.controller('FormEditorController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
        vm.types = Types;
    vm.form = new Form();
    vm.questions = new Questions();
    vm.newQuestion = new Question();
    vm.dontSave = false;
    vm.display = {
        lightbox: {
            newQuestion: false,
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

    vm.switchAll = (value:boolean) : void => {
        value ? vm.questions.selectAll() : vm.questions.deselectAll();
    };

    vm.createNewQuestion = () => {
        vm.dontSave = true;
        template.open('lightbox', 'lightbox/question-new');
        vm.display.lightbox.newQuestion = true;
        $scope.safeApply();
    };

    vm.doCreateNewQuestion = async (code: number) => {
        vm.newQuestion.question_type = code;
        vm.newQuestion.position = vm.questions.all.length + 1;
        await questionService.save(vm.newQuestion);
        await vm.questions.sync(vm.form.id);
        vm.display.lightbox.newQuestion = false;
        template.close('lightbox');
        vm.dontSave = false;
        $scope.safeApply();
    };

    vm.saveQuestions = async (displaySuccess:boolean = false) => {
        try {
            for (let question of vm.questions.all) {
                await questionService.save(question);
                for (let choice of question.choices.all) {
                    if (!!choice.value) await questionChoiceService.save(choice);
                }
            }
            if (displaySuccess) { notify.success(idiom.translate('formulaire.success.form.save')); }
            let response = await formService.get(vm.form.id);
            if (response.status) { vm.form = response.data }
            await vm.questions.sync(vm.form.id);
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };


    // Question functions

    vm.duplicateQuestion = async () => {
        try {
            vm.dontSave = true;
            for (let i = vm.questions.selected[0].position; i < vm.questions.all.length; i++) {
                vm.questions.all[i].position++;
                await questionService.save(vm.questions.all[i]);
            }
            vm.questions.selected[0].position++;
            await questionService.create(vm.questions.selected[0]);
            template.close('lightbox');
            vm.display.lightbox.delete = false;
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
            await questionService.delete(vm.questions.selected[0].id);
            template.close('lightbox');
            vm.display.lightbox.delete = false;
            notify.success(idiom.translate('formulaire.success.question.delete'));
            await vm.questions.sync(vm.form.id);
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
            if (question.title != "" || question.statement != "" || question.mandatory != false) {
                vm.dontSave = true;
                template.open('lightbox', 'lightbox/question-confirm-undo');
                vm.display.lightbox.undo = true;
            }
            else {
                vm.doDeleteQuestion();
            }
        }
    };

    vm.doUndoQuestionChanges = async () => {
        await vm.questions.sync(vm.form.id);
        template.close('lightbox');
        vm.display.lightbox.undo = true;
        vm.dontSave = false;
        $scope.safeApply();
    };

    // Choices functions

    vm.createNewChoice = (question: Question) : void => {
        question.choices.all.push(new QuestionChoice(question.id));
        $scope.safeApply();
    };

    vm.deleteChoice = async (question: Question, index: number) : Promise<void> => {
        if (!!question.choices.all[index].id) {
            await questionChoiceService.delete(question.choices.all[index].id);
        }

        let temp = question.choices.all;
        question.choices.all = [];
        for (let i = 0; i < temp.length; i++) {
            if (i != index) question.choices.all.push(temp[i]);
        }

        $scope.safeApply();
    };


    // Display functions

    vm.displayLastSave = () : string => {
        let localDateTime = DateUtils.localise(vm.form.date_modification);
        let date = DateUtils.format(localDateTime, DateUtils.FORMAT["DAY-MONTH-YEAR"]);
        let time = DateUtils.format(localDateTime, DateUtils.FORMAT["HOUR-MINUTES"]);
        return date + idiom.translate('formulaire.at') + time;
    };

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
                return "/formulaire/public/img/icons/question_type/long-answer.svg";
            case 2 :
                return "/formulaire/public/img/icons/question_type/short-answer.svg";
            case 3 :
                return "/formulaire/public/img/icons/question_type/free-text.svg";
            case 4 :
                return "/formulaire/public/img/icons/question_type/unic-answer.svg";
            case 5 :
                return "/formulaire/public/img/icons/question_type/multiple-answer.svg";
            case 6 :
                return "/formulaire/public/img/icons/question_type/date.svg";
            case 7 :
                return "/formulaire/public/img/icons/question_type/time.svg";
            case 8 :
                return "/formulaire/public/img/icons/question_type/file.svg";
        }
    };


    // Utils

    const onClickQuestion = async (event) : Promise<void> => {
        if (!vm.dontSave && $scope.currentPage === 'openForm') {
            let questionId: number = isInFocusable(event.target);
            if (!!questionId && questionId > 0) {
                let question = vm.questions.all.filter(question => question.id == questionId)[0];
                if (!question.selected) {
                    if (vm.questions.selected.length > 0) {
                        await vm.saveQuestions();
                    }
                    // Reselection of the question because the sync has removed the selections
                    vm.questions.all.filter(question => question.id == questionId)[0].selected = true;
                }
            }
            else if (isInCheckZone(event.target)) {
                let wrongQuestions = vm.questions.filter(question => !!!question.title); // TODO check more than just titles later
                if (wrongQuestions.length > 0) {
                    notify.error(idiom.translate('formulaire.question.save.missing.field'));
                }
                await vm.saveQuestions();
            }
            else {
                await vm.saveQuestions();
            }
            $scope.safeApply();
        }
    };

    const isInCheckZone = (el): boolean => {
        if (!!!el) { return true; }
        else if (el.classList && el.classList.contains("dontCheck")) { return false; }
        return isInCheckZone(el.parentNode);
    };

    const isInFocusable = (el): number => {
        if (!!!el) { return -1; }
        else if (el.classList && el.classList.contains("focusable")) { return el.id; }
        return isInFocusable(el.parentNode);
    };

    init();

    document.onclick = e => { onClickQuestion(e); };

    $scope.$on('init', init());
}]);