import {idiom, model, ng, notify, template} from "entcore";
import {
    Distribution,
    DistributionStatus, Form,
    Question, QuestionChoices,
    Response, ResponseFiles,
    Responses,
    Types
} from "../models";
import {distributionService, formService, questionService, responseFileService, responseService} from "../services";
import {FORMULAIRE_BROADCAST_EVENT} from "../core/enums";

interface ViewModel {
    question: Question;
    response: Response;
    responses: Responses;
    distribution: Distribution;
    form: Form;
    nbQuestions: number;
    last: boolean;
    selectedIndex: Array<boolean>;
    files: File[];
    display: {
        lightbox: {
            sending: boolean
        }
    };

    prev() : Promise<void>;
    next() : Promise<void>;
    saveAndQuit() : Promise<void>;
    send() : Promise<void>;
    doSend() : Promise<void>;
    checkMultiEtab() : boolean;
    getStructures() : string[];
    displayDefaultOption() : string;
}

export const questionResponderController = ng.controller('QuestionResponderController', ['$scope', '$rootScope',
    function ($scope, $rootScope) {

    const vm: ViewModel = this;
    vm.question = new Question();
    vm.response = new Response();
    vm.responses = new Responses();
    vm.distribution = new Distribution();
    vm.form = new Form();
    vm.nbQuestions = 1;
    vm.last = false;
    vm.selectedIndex = new Array<boolean>();
    vm.files = [];
    vm.display = {
        lightbox: {
            sending: false
        }
    };

    const init = async () : Promise<void> => {
        vm.form = $scope.form;
        vm.distribution = $scope.distribution;
        vm.question = $scope.question;
        vm.question.choices = new QuestionChoices();
        vm.nbQuestions = $scope.form.nb_questions;
        vm.distribution = $scope.getDataIf200(await distributionService.get(vm.question.form_id));
        vm.last = vm.question.position === vm.nbQuestions;
        if (vm.question.question_type === Types.MULTIPLEANSWER || vm.question.question_type === Types.SINGLEANSWER) {
            await vm.question.choices.sync(vm.question.id);
        }
        if (vm.question.question_type === Types.MULTIPLEANSWER) {
            await vm.responses.syncMine(vm.question.id, vm.distribution.id);
            vm.selectedIndex = new Array<boolean>(vm.question.choices.all.length);
            for (let i = 0; i < vm.selectedIndex.length; i++) {
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
            let responses = $scope.getDataIf200(await responseService.listMineByDistribution(vm.question.id, vm.distribution.id));
            if (responses.length > 0) {
                vm.response = responses[0];
            }
            if (!!!vm.response.question_id) { vm.response.question_id = vm.question.id; }
            if (!!!vm.response.distribution_id) { vm.response.distribution_id = vm.distribution.id; }
        }
        if (vm.question.question_type === Types.TIME) { formatTime() }
        if (vm.question.question_type === Types.FILE && !!vm.response.id) {
            vm.files = [];
            let responseFiles = new ResponseFiles();
            await responseFiles.sync(vm.response.id);
            for (let i = 0; i < responseFiles.all.length; i++) {
                let responseFile = responseFiles.all[i];
                if (!!responseFile.id) {
                    let file = new File([responseFile.id], responseFile.filename);
                    vm.files.push(file);
                }
            }
            $scope.$broadcast(FORMULAIRE_BROADCAST_EVENT.DISPLAY_FILES, vm.files);
        }

        $scope.safeApply();
    };

    vm.prev = async () : Promise<void> => {
        if (await saveResponses()) {
            let prevPosition: number = vm.question.position - 1;

            if (prevPosition > 0) {
                $scope.redirectTo(`/form/${vm.question.form_id}/question/${prevPosition}`);
            }
        }
    };

    vm.next = async () : Promise<void> => {
        if (await saveResponses()) {
            let nextPosition: number = vm.question.position + 1;

            if (nextPosition <= vm.nbQuestions) {
                $scope.redirectTo(`/form/${vm.question.form_id}/question/${nextPosition}`);
            }
        }
    };

    vm.saveAndQuit = async () : Promise<void> => {
        if (await saveResponses()) {
            if (vm.distribution.status == DistributionStatus.TO_DO) {
                vm.distribution.status = DistributionStatus.IN_PROGRESS;
                await distributionService.update(vm.distribution);
            }
            notify.success(idiom.translate('formulaire.success.responses.save'));
            window.setTimeout(function () { $scope.redirectTo(`/list/responses`); }, 1000);
        }
    };

    vm.send = async () : Promise<void> => {
        await saveResponses();
        if (await checkMandatoryQuestions()) {
            vm.distribution.structure = model.me.structureNames[0];
            template.open('lightbox', 'lightbox/responses-confirm-sending');
            vm.display.lightbox.sending = true;
        }
        else {
            notify.error(idiom.translate('formulaire.warning.send.missing.responses.missing'));
        }
    };

    vm.doSend = async () : Promise<void> => {
        vm.distribution.status = DistributionStatus.FINISHED;
        vm.distribution.structure = !!vm.distribution.structure ? vm.distribution.structure : model.me.structureNames[0];
        await responseService.fillResponses(vm.form.id,  vm.distribution.id);
        await distributionService.update(vm.distribution);
        template.close('lightbox');
        vm.display.lightbox.sending = false;
        notify.success(idiom.translate('formulaire.success.responses.save'));
        window.setTimeout(function () { $scope.redirectTo(`/list/responses`); }, 1000);
    };

    vm.checkMultiEtab = () : boolean => {
        return model.me.structureNames.length > 1 && !vm.form.anonymous;
    };

    vm.getStructures = () : string[] => {
        return model.me.structureNames;
    };

    vm.displayDefaultOption = () : string => {
        return idiom.translate('formulaire.options.select');
    };


    const saveResponses = async () : Promise<boolean> => {
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
                    let newResponse = new Response(vm.question.id, vm.question.choices.all[i].id,
                        vm.question.choices.all[i].value, vm.distribution.id);
                    await responseService.create(newResponse);
                }
                else if (found && !checked) {
                    await responseService.delete(vm.responses.all[j-1].id);
                }
            }
            return true;
        }
        if (vm.question.question_type == Types.SINGLEANSWER) {
            if (!!!vm.response.choice_id) {
                vm.response.answer = "";
            }
            else {
                for (let choice of vm.question.choices.all) {
                    if (vm.response.choice_id == choice.id) {
                        vm.response.answer = choice.value;
                    }
                }
            }
        }
        vm.response = $scope.getDataIf200(await responseService.save(vm.response, vm.question.question_type));
        if (vm.question.question_type === Types.FILE) {
            return (await saveFiles());
        }
        return true;
    };

    const saveFiles = async () : Promise<boolean> => {
        if (vm.files.length > 10) {
            notify.info(idiom.translate('formulaire.response.file.tooMany'));
            return false;
        }
        else {
            await responseFileService.deleteAll(vm.response.id);

            for (let i = 0; i < vm.files.length; i++) {
                let filename = vm.files[i].name;
                if (!!vm.files[i].type && !$scope.form.anonymous) {
                    filename = model.me.firstName + model.me.lastName + "_" + filename;
                }
                let file = new FormData();
                file.append("file", vm.files[i], filename);
                await responseFileService.create(vm.response.id, file);
            }

            vm.response.answer = idiom.translate('formulaire.response.file.send');
            vm.response = $scope.getDataIf200(await responseService.update(vm.response));
            return true;
        }
    };

    const formatTime = () : void => {
        if (!!vm.response.answer) {
            vm.response.answer = new Date("January 01 1970 " + vm.response.answer);
        }
    };

    const checkMandatoryQuestions = async () : Promise<boolean> => {
        try {
            let questions = $scope.getDataIf200(await questionService.list(vm.question.form_id));
            questions = questions.filter(question => question.mandatory === true);
            for (let question of questions) {
                let responses = $scope.getDataIf200(await responseService.listMineByDistribution(question.id, vm.distribution.id));
                responses = responses.filter(response => !!response.answer);
                if (responses.length <= 0) {
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

    $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_CONTROLLER, () => { init() });
}]);