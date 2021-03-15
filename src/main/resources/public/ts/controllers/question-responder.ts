import {idiom, model, ng, notify, template} from "entcore";
import {
    Distribution,
    DistributionStatus,
    Question, QuestionChoices,
    Response,
    Responses,
    Types
} from "../models";
import {distributionService, questionService, responseFileService, responseService} from "../services";

interface ViewModel {
    types: typeof Types;
    question: Question;
    response: Response;
    responses: Responses;
    distribution: Distribution;
    nbQuestions: number;
    last: boolean;
    selectedIndex: Array<boolean>;
    display: {
        lightbox: {
            sending: boolean
        }
    };
    files: File[];

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
    vm.responses = new Responses();
    vm.distribution = new Distribution();
    vm.nbQuestions = 1;
    vm.last = false;
    vm.selectedIndex = new Array<boolean>();
    vm.display = {
        lightbox: {
            sending: false
        }
    };
    vm.files = [];

    const init = async (): Promise<void> => {
        vm.question = $scope.question;
        vm.question.choices = new QuestionChoices();
        vm.nbQuestions = $scope.form.nbQuestions;
        vm.last = vm.question.position === vm.nbQuestions;
        await vm.question.choices.sync(vm.question.id);
        if (vm.question.question_type === Types.MULTIPLEANSWER) {
            await vm.responses.syncMine(vm.question.id);
            vm.selectedIndex = new Array<boolean>(vm.nbQuestions);
            for (let i = 0; i < vm.question.choices.all.length; i++) {
                let check = false;
                let j = 0;
                while (!check && j < vm.responses.all.length) {
                    check = vm.question.choices.all[i].id === vm.responses.all[j].choice_id;
                    j++;
                }
                vm.selectedIndex[i] = check;
            }
        }
        else {
            vm.response = new Response();
            let responses = $scope.getDataIf200(await responseService.listMine(vm.question.id));
            if (responses.length > 0) {
                vm.response = responses[0];
            }
            if (!!!vm.response.question_id) { vm.response.question_id = vm.question.id; }
        }
        if (vm.question.question_type === Types.TIME) { formatTime() }
        if (vm.question.question_type === Types.FILE && !!vm.response.id) {
            let responseFile = $scope.getDataIf200(await responseFileService.get(vm.response.id));
            if (!!responseFile.id) {
                let file = new File([responseFile.id], responseFile.filename);
                vm.files.push(file);
            }
        }
        vm.distribution = $scope.getDataIf200(await distributionService.get(vm.question.form_id));

        $scope.safeApply();
    };

    vm.prev = async (): Promise<void> => {
        await saveResponses();
        let prevPosition: number = vm.question.position - 1;

        if (prevPosition > 0) {
            $scope.redirectTo(`/form/${vm.question.form_id}/question/${prevPosition}`);
            $scope.safeApply();
            let question = await questionService.getByPosition(vm.question.form_id, prevPosition);
            $scope.question = question.data;
            init();
        }
    };

    vm.next = async (): Promise<void> => {
        await saveResponses();
        let nextPosition: number = vm.question.position + 1;

        if (nextPosition <= vm.nbQuestions) {
            $scope.redirectTo(`/form/${vm.question.form_id}/question/${nextPosition}`);
            $scope.safeApply();
            let question = await questionService.getByPosition(vm.question.form_id, nextPosition);
            $scope.question = question.data;
            init();
        }
    };

    vm.saveAndQuit = async (): Promise<void> => {
        await saveResponses();
        if (vm.distribution.status == DistributionStatus.TO_DO) {
            vm.distribution.status = DistributionStatus.IN_PROGRESS;
            await distributionService.update(vm.distribution);
        }
        notify.success(idiom.translate('formulaire.success.responses.save'));
        $scope.redirectTo(`/list/responses`);
        $scope.safeApply();
    };

    vm.send = async (): Promise<void> => {
        await saveResponses();
        if (await checkMandatoryQuestions()) {
            template.open('lightbox', 'lightbox/responses-confirm-sending');
            vm.display.lightbox.sending = true;
        }
        else {
            notify.error(idiom.translate('formulaire.warning.send.missing.responses.missing'));
        }
    };

    vm.doSend = async (): Promise<void> => {
        vm.distribution.status = DistributionStatus.FINISHED;
        await distributionService.update(vm.distribution);
        template.close('lightbox');
        vm.display.lightbox.sending = false;
        notify.success(idiom.translate('formulaire.success.responses.save'));
        $scope.redirectTo(`/list/responses`);
        $scope.safeApply();
    };

    const saveResponses = async (): Promise<void> => {
        if (vm.question.question_type === Types.MULTIPLEANSWER) {
            for (let i = 0; i < vm.question.choices.all.length; i++) {
                let checked = vm.selectedIndex[i];
                let j = 0;
                let found = false;
                while (!found && j < vm.responses.all.length) {
                    found = vm.question.choices.all[i].id === vm.responses.all[j].choice_id;
                    j++;
                }
                if (!found && checked) {
                    let newResponse = new Response(vm.question.id, vm.question.choices.all[i].id, vm.question.choices.all[i].value);
                    await responseService.create(newResponse);
                }
                else if (found && !checked) {
                    await responseService.delete(vm.responses.all[j-1].id);
                }
            }
        }
        else {
            if (vm.question.question_type === Types.FILE && vm.files.length > 0) {
                let filename = model.me.firstName + model.me.lastName + "_" + vm.files[0].name;
                let file = new FormData();
                file.append("file", vm.files[0], filename);
                await responseFileService.update(vm.response.id, file);
                vm.response.answer = idiom.translate('formulaire.response.file.send');
            }
            vm.response = $scope.getDataIf200(await responseService.save(vm.response, vm.question.question_type));
        }
    };

    const formatTime = (): void => {
        if (!!vm.response.answer) {
            vm.response.answer = new Date("January 01 1970 " + vm.response.answer);
        }
    };

    const checkMandatoryQuestions = async (): Promise<boolean> => {
        try {
            let questions = $scope.getDataIf200(await questionService.list(vm.question.form_id));
            questions = questions.filter(question => question.mandatory === true);
            for (let question of questions) {
                let responses = $scope.getDataIf200(await responseService.listMine(question.id));
                responses = responses.filter(response => !!!response.answer);
                if (responses.length > 0) {
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