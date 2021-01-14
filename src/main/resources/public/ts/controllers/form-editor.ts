import {idiom, ng, template} from 'entcore';
import {Form, Question, Questions, QuestionTypes} from "../models";
import {questionService} from "../services";

interface ViewModel {
    form: Form;
    questions: Questions;
    types: QuestionTypes;
    editedQuestion: Question;
    lightbox: {
        newQuestion: boolean
    };

    openNewQuestion(): void;
    createNewQuestion(): void;
    displayTypeName(string): string;
}


export const formEditorController = ng.controller('FormEditorController', ['$scope', 'QuestionService',
    function ($scope) {

    const vm: ViewModel = this;
    vm.form = new Form();
    vm.questions = new Questions();
    vm.types = new QuestionTypes();
    vm.editedQuestion = new Question();
    vm.lightbox = {
        newQuestion: false,
    };

    const init = async (): Promise<void> => {
        vm.form = $scope.edit.form;
        await vm.questions.sync(vm.form.id);
        vm.types = $scope.questionTypes;
    };

    // Functions

    vm.openNewQuestion = () => {
        template.open('lightbox', 'lightbox/new-question');
        vm.lightbox.newQuestion = true;
        $scope.safeApply();
    };

    vm.createNewQuestion = async () => {
        let response = await questionService.create(vm.editedQuestion);
        $scope.safeApply();
    };

    vm.displayTypeName = (typeName: string) : string => {
        return idiom.translate('formulaire.question.type.' + typeName);
    };

    init();
}]);