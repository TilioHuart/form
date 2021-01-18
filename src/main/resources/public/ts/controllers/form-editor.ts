import {idiom, ng, notify, template} from 'entcore';
import {Form, Question, Questions} from "../models";
import {formService, questionService} from "../services";
import {DateUtils} from "../utils/date";

interface ViewModel {
    form: Form;
    questions: Questions;
    editedQuestion: Question;
    display: {
        lightbox: {
            newQuestion: boolean
        }
    };

    openNewQuestion(): void;
    createNewQuestion(): void;
    saveQuestions(): void;
    displayLastSave(): string;
    displayTypeName(string): string;
}


export const formEditorController = ng.controller('FormEditorController', ['$scope', 'QuestionService',
    function ($scope) {

    const vm: ViewModel = this;
    vm.form = new Form();
    vm.questions = new Questions();
    vm.editedQuestion = new Question();
    vm.display = {
        lightbox: {
            newQuestion: false
        }
    };

    const init = async (): Promise<void> => {
        vm.form = $scope.edit.form;
        await vm.questions.sync(vm.form.id);
        $scope.safeApply();
    };

    // Functions

    vm.openNewQuestion = () => {
        template.open('lightbox', 'lightbox/question-new');
        vm.display.lightbox.newQuestion = true;
        $scope.safeApply();
    };

    vm.createNewQuestion = async () => {
        let response = await questionService.save(vm.editedQuestion);
        await vm.questions.sync(vm.form.id);
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

    init();
}]);