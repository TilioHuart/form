import {idiom, model, ng, notify, template} from "entcore";
import {
    Distribution,
    DistributionStatus,
    Form,
    FormElements,
    Question,
    Responses,
    Section,
    Types,
    FormElement,
    Response, QuestionChoice
} from "../models";
import {distributionService, formElementService, responseFileService, responseService} from "../services";
import {FORMULAIRE_BROADCAST_EVENT} from "@common/core/enums";
import {Res} from "awesome-typescript-loader/dist/checker/protocol";

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
            vm.formElements.all = vm.formElements.all.filter((e: FormElement) => vm.historicPosition.indexOf(e.position) >= 0);
        }
        else {
            let responseQuestionIds: any = vm.responses.all.map((r: Response) => r.question_id);
            vm.formElements.all = vm.formElements.all.filter((e: FormElement) => shouldDisplayElement(e, responseQuestionIds));
            vm.historicPosition = vm.formElements.all.map((e: FormElement) => e.position);
            vm.historicPosition.sort( (a, b) => a - b);
        }

        // Get files responses for files question
        let fileQuestions: Question[] = vm.formElements.getAllQuestions().all.filter((q: Question) => q.question_type === Types.FILE);
        for (let fileQuestion of fileQuestions) {
            let response: Response = vm.responses.all.filter((r: Response) => r.question_id === fileQuestion.id)[0];
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
            await cleanResponses();
            await distributionService.replace(distrib);
        }
        else {
            await cleanResponses();
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
        let mandatoryQuestions: Question[] = vm.formElements.getAllQuestions().all.filter((q: Question) => q.mandatory);
        for (let question of mandatoryQuestions) {
            if (question.question_type === Types.MATRIX) {
                for (let child of question.children.all) {
                    if (vm.responses.all.filter((r: Response) => r.question_id === child.id && r.answer).length <= 0) {
                        return false;
                    }
                }
            }
            else if (vm.responses.all.filter((r: Response) => r.question_id === question.id && r.answer).length <= 0) {
                return false;
            }
        }
        return true;
    };

    const cleanResponses = async () : Promise<void> => {
        let responsesToClean: Responses = new Responses();
        let validatedElements: FormElements = new FormElements();
        validatedElements.all = vm.formElements.all.filter((e: FormElement) => vm.historicPosition.indexOf(e.position) >= 0);
        let validatedQuestionIds: number[] = validatedElements.getAllQuestionsAndChildren().all.map((q: Question) => q.id);
        responsesToClean.all = vm.responses.all.filter((r: Response) => validatedQuestionIds.indexOf(r.question_id) < 0);
        await responseService.delete(vm.form.id, responsesToClean.all);
    };

    const shouldDisplayElement = (e: FormElement, responseQuestionIds: any) : boolean => {
        if (responseQuestionIds.includes(e.id)) {
            return true;
        }

        if (e instanceof Question) {
            let childrenIds: any = e.children.all.map((q: Question) => q.id);
            return responseQuestionIds.some((id: number) => childrenIds.includes(id));
        }
        else if (e instanceof Section) {
            return e.questions.all.filter((q: Question) => shouldDisplayElement(q, responseQuestionIds)).length > 0;
        }
    };

    $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_RECAP_QUESTIONS, () => { vm.$onInit() });
}]);