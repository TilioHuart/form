import {idiom, ng, notify, template} from 'entcore';
import {Form, Question, Questions, QuestionTypes} from "../models";
import {formService, questionService} from "../services";
import {DateUtils} from "../utils/date";

interface ViewModel {
    form: Form;
    questionTypes: QuestionTypes;
    questions: Questions;
    newQuestion: Question;
    display: {
        lightbox: {
            newQuestion: boolean
        }
    };

    openNewQuestion(): void;
    createNewQuestion(number): void;
    saveQuestions(): void;
    displayLastSave(): string;
    displayTypeName(string): string;
    displayTypeIcon(number): string;
}


export const formEditorController = ng.controller('FormEditorController', ['$scope', 'QuestionService',
    function ($scope) {

    const vm: ViewModel = this;
    vm.form = new Form();
    vm.questionTypes = new QuestionTypes();
    vm.questions = new Questions();
    vm.newQuestion = new Question();
    vm.display = {
        lightbox: {
            newQuestion: false
        }
    };

    const init = async (): Promise<void> => {
        vm.form = $scope.edit.form;
        await vm.questionTypes.sync();
        await vm.questions.sync(vm.form.id);
        vm.newQuestion.form_id = vm.form.id;
        $scope.safeApply();
    };

    // Functions

    vm.openNewQuestion = () => {
        template.open('lightbox', 'lightbox/question-new');
        vm.display.lightbox.newQuestion = true;
        $scope.safeApply();
    };

    vm.createNewQuestion = async (code: number) => {
        vm.newQuestion.question_type = code;
        vm.newQuestion.position = vm.questions.all.length;
        let response = await questionService.save(vm.newQuestion);
        await vm.questions.sync(vm.form.id);
        vm.display.lightbox.newQuestion = false;
        template.close('lightbox');
        $scope.safeApply();
    };

    vm.saveQuestions = async () => {
        try {
            for (let question of vm.questions.all) {
                let response = await questionService.save(question);
            }
            notify.success(idiom.translate('formulaire.success.form.save'));
            let response = await formService.get(vm.form.id);
            if (response.status) { vm.form = response.data };
            await vm.questions.sync(vm.form.id);
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };

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
                return "/formulaire/public/img/icons/text-long.svg";
            case 2 :
                return "/formulaire/public/img/icons/text-short.svg";
            case 3 :
                return "/formulaire/public/img/icons/text.svg";
            case 4 :
                return "/formulaire/public/img/icons/order-bool-descending.svg";
            case 5 :
                return "/formulaire/public/img/icons/order-bool-ascending-variant.svg";
            case 6 :
                return "/formulaire/public/img/icons/calendar-today.svg";
            case 7 :
                return "/formulaire/public/img/icons/clock-outline.svg";
            case 8 :
                return "/formulaire/public/img/icons/tray-arrow-down.svg";
        }
    }

    init();
}]);