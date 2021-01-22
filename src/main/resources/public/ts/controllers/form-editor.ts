import {$, idiom, ng, notify, template} from 'entcore';
import {Form, Question, Questions} from "../models";
import {formService, questionService} from "../services";
import {DateUtils} from "../utils/date";

interface ViewModel {
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

    switchAll(boolean): void;
    createNewQuestion(): void;
    doCreateNewQuestion(number): void;
    saveQuestions(boolean?): void;
    duplicateQuestion(): void;
    deleteQuestion(): void;
    doDeleteQuestion(): void;
    undoQuestionChanges(): void;
    doUndoQuestionChanges(): void;
    displayLastSave(): string;
    displayTypeName(string): string;
    displayTypeIcon(number): string;
}


export const formEditorController = ng.controller('FormEditorController', ['$scope', 'QuestionService',
    function ($scope) {

    const vm: ViewModel = this;
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
        vm.form = $scope.edit.form;
        await vm.questions.sync(vm.form.id);
        vm.newQuestion.form_id = vm.form.id;
        $scope.safeApply();
    };


    // Global functions

    vm.switchAll = (value:boolean) : void => {
        value ? vm.questions.selectAll() : vm.questions.deselectAll();
    };

    vm.createNewQuestion = () => {
        template.open('lightbox', 'lightbox/question-new');
        vm.display.lightbox.newQuestion = true;
        $scope.safeApply();
    };

    vm.doCreateNewQuestion = async (code: number) => {
        vm.newQuestion.question_type = code;
        vm.newQuestion.position = vm.questions.all.length + 1;
        let response = await questionService.save(vm.newQuestion);
        await vm.questions.sync(vm.form.id);
        vm.display.lightbox.newQuestion = false;
        template.close('lightbox');
        $scope.safeApply();
    };

    vm.saveQuestions = async (displaySuccess:boolean = false) => {
        try {
            for (let question of vm.questions.all) {
                let response = await questionService.save(question);
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
                let response = await questionService.save(vm.questions.all[i]);
            }
            vm.questions.selected[0].position++;
            let response = await questionService.create(vm.questions.selected[0]);
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
            let response = await questionService.delete(vm.questions.selected[0].id);
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
        if (!vm.dontSave) {
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
            } else {
                await vm.saveQuestions();
            }
            $scope.safeApply();
        }
    };

    const isInFocusable = (el): number => {
        if (!!!el) { return -1; }
        else if (el.classList && el.classList.contains("focusable")) { return el.id; }
        return isInFocusable(el.parentNode);
    };

    init();

    document.onclick = e => { onClickQuestion(e); };
}]);