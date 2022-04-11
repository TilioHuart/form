import {idiom, model, ng, notify, template} from "entcore";
import {Distribution, DistributionStatus, Form, FormElements, Question, Responses, Section, Types} from "../models";
import {distributionService, formElementService, responseFileService, responseService} from "../services";
import {FORMULAIRE_BROADCAST_EVENT} from "../core/enums";

interface ViewModel {
    formElements: FormElements;
    responses: Responses;
    distribution: Distribution;
    form: Form;
    historicPosition: number[];
    display: {
        lightbox: {
            sending: boolean
        }
    };

    $onInit() : Promise<void>;
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
        vm.historicPosition = $scope.historicPosition;
        await vm.formElements.sync(vm.form.id);
        await vm.responses.syncByDistribution(vm.distribution.id);

        // Get right elements to display
        if (vm.historicPosition.length > 0) {
            vm.formElements.all = vm.formElements.all.filter(e => vm.historicPosition.indexOf(e.position) >= 0);
        }
        else {
            let responseQuestionIds = vm.responses.all.map(r => r.question_id);
            vm.formElements.all = vm.formElements.all.filter(e =>
                (responseQuestionIds.indexOf(e.id) > 0) ||
                (e instanceof Section && e.questions.all.map(q => q.id).filter(id => responseQuestionIds.indexOf(id) >= 0).length > 0)
            );
            vm.historicPosition = vm.formElements.all.map(e => e.position);
            vm.historicPosition.sort( (a, b) => a - b);
        }

        // Get files responses for files question
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

        if (distrib.original_id) {
            let questionFileIds: any = vm.formElements.all.filter(q => q instanceof Question && q.question_type === Types.FILE).map(q => q.id);
            let responseFiles = vm.responses.all.filter(r => questionFileIds.includes(r.question_id));
            for (let responseFile of responseFiles) {
                await responseFileService.deleteAll(responseFile.original_id);
            }
            await distributionService.replace(distrib);
        }
        else {
            let responsesToClean = new Responses();
            let validatedElements = new FormElements();
            validatedElements.all = vm.formElements.all.filter(e => vm.historicPosition.indexOf(e.position) >= 0);
            let validatedQuestionIds = validatedElements.getAllQuestions().all.map(q => q.id);
            responsesToClean.all = vm.responses.all.filter(r => validatedQuestionIds.indexOf(r.question_id) < 0);
            await responseService.delete(vm.form.id, responsesToClean.all);
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
        let mandatoryQuestions = vm.formElements.getAllQuestions().all.filter(q => q.mandatory);
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