import {idiom, ng, notify, template} from "entcore";
import {Distribution, DistributionStatus, Question, QuestionType, Response, Types} from "../models";
import {distributionService, formService, questionService} from "../services";
import {responseService} from "../services/ResponseService";
import {DateUtils} from "../utils/date";

interface ViewModel {
    types: typeof Types;
    question: Question;
    response: Response;
    distribution: Distribution;
    nbQuestions: number;
    last: boolean;
    display: {
        lightbox: {
            sending: boolean
        }
    };

    prev(): Promise<void>;
    next(): Promise<void>;
    saveAndQuit(): Promise<void>;
    send(): Promise<void>;
    doSend(): Promise<void>;
}

export const questionResponderController = ng.controller('QuestionResponderController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.types = Types;
    vm.question = new Question();
    vm.response = new Response();
    vm.distribution = new Distribution();
    vm.nbQuestions = 1;
    vm.last = false;
    vm.display = {
        lightbox: {
            sending: false
        }
    };

    const init = async (): Promise<void> => {
        vm.question = $scope.question;
        vm.nbQuestions = $scope.form.nbQuestions;
        vm.last = vm.question.position == vm.nbQuestions;
        vm.response = $scope.getDataIf200(await responseService.get(vm.question.id));
        if (!!!vm.response.question_id) { vm.response.question_id = vm.question.id; }
        vm.distribution = $scope.getDataIf200(await distributionService.get(vm.question.form_id));

        if (vm.question.question_type === Types.DATE) { formatDate() }
        if (vm.question.question_type === Types.TIME) { formatTime() }

        $scope.safeApply();
    };

    vm.prev = async (): Promise<void> => {
        await responseService.save(vm.response);
        let prevPosition: number = vm.question.position - 1;

        if (prevPosition > 0) {
            $scope.redirectTo(`/form/${vm.question.form_id}/question/${prevPosition}`);
            $scope.safeApply();
            $scope.form = $scope.getDataIf200(await formService.get(vm.question.form_id));
            $scope.form.nbQuestions = $scope.getDataIf200(await questionService.countQuestions(vm.question.form_id)).count;
            let question = await questionService.getByPosition(vm.question.form_id, prevPosition);
            $scope.question = question.data;
            init();
        }
    };

    vm.next = async (): Promise<void> => {
        await responseService.save(vm.response);
        let nextPosition: number = vm.question.position + 1;

        if (nextPosition <= vm.nbQuestions) {
            $scope.redirectTo(`/form/${vm.question.form_id}/question/${nextPosition}`);
            $scope.safeApply();
            $scope.form =  $scope.getDataIf200(await formService.get(vm.question.form_id));
            $scope.form.nbQuestions = $scope.getDataIf200(await questionService.countQuestions(vm.question.form_id)).count;
            let question = await questionService.getByPosition(vm.question.form_id, nextPosition);
            $scope.question = question.data;
            init();
        }
    };

    vm.saveAndQuit = async (): Promise<void> => {
        await responseService.save(vm.response);
        if (vm.distribution.status == DistributionStatus.TO_DO) {
            vm.distribution.status = DistributionStatus.IN_PROGRESS;
            await distributionService.update(vm.distribution);
        }
        notify.success(idiom.translate('formulaire.success.responses.save'));
        $scope.redirectTo(`/list/responses`);
        $scope.safeApply();
    };

    vm.send = async (): Promise<void> => {
        await responseService.save(vm.response);
        if (await checkMandatoryQuestions()) {
            template.open('lightbox', 'lightbox/responses-confirm-sending');
            vm.display.lightbox.sending = true;
        }
        else {
            notify.error(idiom.translate('formulaire.warning.send.missing.responses.missing'));
        }
    };

    vm.doSend = async (): Promise<void> => {
        await responseService.save(vm.response);
        vm.distribution.status = DistributionStatus.FINISHED;
        await distributionService.update(vm.distribution);
        template.close('lightbox');
        vm.display.lightbox.sending = false;
        notify.success(idiom.translate('formulaire.success.responses.save'));
        $scope.redirectTo(`/list/responses`);
        $scope.safeApply();
    };

    const formatDate = (): void => {
        vm.response.answer = DateUtils.format(vm.response.answer, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
    };

    const formatTime = (): void => {
        vm.response.answer = new Date(vm.response.answer.toString());
    };

    const checkMandatoryQuestions = async (): Promise<boolean> => {
        try {
            let questions = $scope.getDataIf200(await questionService.list(vm.question.form_id));
            questions = questions.filter(question => question.mandatory === true);
            for (let question of questions) {
                let response = $scope.getDataIf200(await responseService.get(question.id));
                if (!!!response.answer) {
                    return false;
                }
            }
            return true;
        }
        catch (e) {
            throw e;
        }
    };

    init();
}]);