import {ng} from 'entcore';
import {Form, Questions} from "../models";
import {questionService} from "../services";

interface ViewModel {
    form: Form;
    questions: Questions;
}


export const formEditorController = ng.controller('FormEditorController', ['$scope', 'QuestionService',
    function ($scope) {

    const vm: ViewModel = this;
    vm.form = new Form();
    vm.questions = new Questions();

    const init = async (): Promise<void> => {
    };

    init();
}]);