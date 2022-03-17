import {idiom, model, ng, notify, template} from "entcore";
import {
    Distribution,
    DistributionStatus, Form, FormElements,
    Question,
    Responses,
    Types
} from "../models";
import {
    distributionService,
    formElementService,
    responseFileService,
    responseService
} from "../services";
import {FORMULAIRE_BROADCAST_EVENT, FORMULAIRE_EMIT_EVENT} from "../core/enums";

interface ViewModel {
    formElements: FormElements;
    responses: Responses;
    distribution: Distribution;
    form: Form;
    display: {
        lightbox: {
            sending: boolean
        }
    };

    $onInit() : Promise<void>;
    prev() : Promise<void>;
    saveAndQuit() : Promise<void>;
    send() : Promise<void>;
    doSend() : Promise<void>;
    checkMultiEtab() : boolean;
    getStructures() : string[];
}

export const recapQuestionsController = ng.controller('RecapQuestionsController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.formElements = new FormElements();
    vm.responses = new Responses();
    vm.distribution = new Distribution();
    vm.form = new Form();
    vm.display = {
        lightbox: {
            sending: false
        }
    };

    vm.$onInit = async () : Promise<void> => {
        vm.form = $scope.form;
        vm.form.nb_elements = (await formElementService.countFormElements(vm.form.id)).count;
        vm.distribution = $scope.distribution;
        await vm.formElements.sync(vm.form.id);
        await vm.responses.syncByDistribution(vm.distribution.id);
        let fileQuestions = vm.formElements.getAllQuestions().all.filter(q => q.question_type === Types.FILE);
        for (let fileQuestion of fileQuestions) {
            let response = vm.responses.all.filter(r => r.question_id === fileQuestion.id)[0];
            if (response) {
                await response.files.sync(response.id);
            }
        }
        $scope.safeApply();
    };

    // Global functions

    vm.prev = async () : Promise<void> => {
        $scope.$emit(FORMULAIRE_EMIT_EVENT.REDIRECT, {path: `/form/${vm.form.id}/${vm.distribution.id}`});
    };

    vm.send = async () : Promise<void> => {
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
        let distrib = vm.distribution;
        distrib.status = DistributionStatus.FINISHED;
        distrib.structure = distrib.structure ? distrib.structure : model.me.structureNames[0];
        await responseService.fillResponses(vm.form.id, vm.distribution.id);
        if (distrib.original_id) {
            let questionFileIds: any = vm.formElements.all.filter(q => q instanceof Question && q.question_type === Types.FILE).map(q => q.id);
            let responseFiles = vm.responses.all.filter(r => questionFileIds.includes(r.question_id));
            for (let responseFile of responseFiles) {
                await responseFileService.deleteAll(responseFile.original_id);
            }
            await distributionService.replace(distrib);
        }
        else {
            await distributionService.update(distrib);
        }
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

    const checkMandatoryQuestions = async () : Promise<boolean> => {
        let mandatoryQuestions = vm.formElements.all.filter(q => q instanceof Question && q.mandatory === true);
        for (let question of mandatoryQuestions) {
            let responses = vm.responses.all.filter(r => r.question_id === question.id && r.answer);
            if (responses.length <= 0) {
                return false;
            }
        }
        return true;
    };

    $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_RECAP_QUESTIONS, () => { vm.$onInit() });
}]);