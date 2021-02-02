import {idiom, ng, notify} from "entcore";
import {Distribution, DistributionStatus, Question, Response} from "../models";
import {distributionService, questionService} from "../services";
import {responseService} from "../services/ResponseService";

interface ViewModel {
    question: Question;
    response: Response;
    distribution: Distribution;
    nbQuestions: number;
    last: boolean;

    prev(): void;
    next(): void;
    save(): void;
    send(): void;
    checkNext(): boolean;
}

export const questionResponderController = ng.controller('QuestionResponderController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.question = new Question();
    vm.response = new Response();
    vm.distribution = new Distribution();
    vm.nbQuestions = 1;
    vm.last = false;

    const init = async (): Promise<void> => {
        vm.question = $scope.question;
        vm.response = $scope.getDataIf200(await responseService.get(vm.question.id));
        if (!!!vm.response.question_id) { vm.response.question_id = vm.question.id; }
        vm.distribution = $scope.getDataIf200(await distributionService.get(vm.question.form_id));
        vm.nbQuestions = $scope.form.nbQuestions;
        vm.last = vm.question.position == vm.nbQuestions;

        $scope.safeApply();
    };

    vm.prev = async () => {
        await responseService.save(vm.response);
        let prevPosition: number = vm.question.position - 1;

        if (prevPosition > 0) {
            $scope.redirectTo(`/form/${vm.question.form_id}/question/${prevPosition}`);
            $scope.safeApply();
            let question = await questionService.getByPosition(vm.question.form_id, prevPosition);
            $scope.question = question.data;
            init();
        }
    };

    vm.next = async () => {
        await responseService.save(vm.response);
        let nextPosition: number = vm.question.position + 1;

        if (nextPosition <= vm.nbQuestions) {
            $scope.redirectTo(`/form/${vm.question.form_id}/question/${nextPosition}`);
            $scope.safeApply();
            let question = await questionService.getByPosition(vm.question.form_id, nextPosition);
            $scope.question = question.data;
            init();
        }
    };

    vm.save = async () => {
        await responseService.save(vm.response);
        if (vm.distribution.status == DistributionStatus.TO_DO) {
            vm.distribution.status = DistributionStatus.IN_PROGRESS;
            await distributionService.update(vm.distribution);
        }
        notify.success(idiom.translate('formulaire.success.responses.save'));
        // TODO go back to list forms responses & sync
    };

    vm.send = async () => {
        await responseService.save(vm.response);
        vm.distribution.status = DistributionStatus.FINISHED;
        await distributionService.update(vm.distribution);
        await notify.success(idiom.translate('formulaire.success.responses.save'));
        $scope.redirectTo(`/list/responses`);
        $scope.safeApply();
    };

    init();
}]);