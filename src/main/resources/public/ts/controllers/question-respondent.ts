import {ng} from "entcore";
import {Response} from "../models";
import {questionService} from "../services";

interface ViewModel {
    response: Response;
    last: boolean;

    init(): void;
    prev(): void;
    save(): void;
    next(): void;
    checkNext(): boolean;
}

export const questionRespondentController = ng.controller('QuestionRespondentController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;

    vm.init = async (): Promise<void> => {
        vm.response = new Response();
        await vm.response.get($scope.question.id);
        vm.response.question_id = $scope.question.id;
        let { data } = await questionService.getByPosition($scope.form.id, $scope.question.position + 1);
        vm.last = data.id == null;
        $scope.safeApply();
    };

    vm.prev = async () => {
        await vm.response.send();
        $scope.redirectTo(`/form/${$scope.form.id}/question/${$scope.question.position - 1}`);
        $scope.safeApply();
        let question = await questionService.getByPosition($scope.form.id, $scope.question.position - 1);
        $scope.question = question.data;
        vm.init();
    }

    vm.save = async () => {
        await vm.response.send();
        $scope.redirectTo(`/list`);
        $scope.safeApply();
    }

    vm.next = async () => {
        await vm.response.send();
        $scope.redirectTo(`/form/${$scope.form.id}/question/${$scope.question.position + 1}`);
        $scope.safeApply();
        let question = await questionService.getByPosition($scope.form.id, $scope.question.position + 1);
        $scope.question = question.data;
        vm.init();
    }

    vm.init();
}]);