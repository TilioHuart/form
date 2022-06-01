import {idiom, ng, notify, template} from "entcore";
import {
    Form,
    FormElement,
    FormElements,
    Response,
    Responses,
    Types
} from "@common/models";
import { publicService } from "../services";
import {Mix} from "entcore-toolkit";
import {PublicUtils} from "../utils";

interface ViewModel {
    formKey: string;
    distributionKey: string;
    formElements: FormElements;
    allResponsesInfos: Map<FormElement, { responses: Responses, selectedIndexList: any, responsesChoicesList: any }>;

    formElement: FormElement;

    form: Form;
    nbFormElements: number;
    loading : boolean;
    historicPosition: number[];

    responses: Responses;
    display: {
        lightbox: {
            sending: boolean
        }
    };

    $onInit() : Promise<void>;
    send() : Promise<void>;
    doSend() : Promise<void>;
}

export const recapQuestionsController = ng.controller('RecapQuestionsController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.form = new Form();
    vm.formElements = new FormElements();
    vm.allResponsesInfos = new Map();
    vm.responses = new Responses();
    vm.display = {
        lightbox: {
            sending: false
        }
    };

    vm.$onInit = async () : Promise<void> => {
        syncWithStorageData();
        vm.formElements.all = vm.formElements.all.filter(e => vm.historicPosition.indexOf(e.position) >= 0);
        let allQuestions = vm.formElements.getAllQuestions().all;
        let questionIdsToDisplay = allQuestions.map(q => q.id);
        vm.allResponsesInfos.forEach((value) => {
            for (let i = 0; i < value.responses.all.length; i++) {
                let response = value.responses.all[i];

                if (questionIdsToDisplay.indexOf(response.question_id) >= 0) {
                    let correspondingQuestion = allQuestions.filter(q => q.id === response.question_id)[0];

                    if (correspondingQuestion.question_type === Types.MULTIPLEANSWER) {
                        let questionChoices = correspondingQuestion.choices.all.sort((a, b) => a.id - b.id);

                        for (let j = 0; j < value.selectedIndexList[i].length; j++) {
                            if (value.selectedIndexList[i][j]) {
                                vm.responses.all.push(new Response(correspondingQuestion.id, questionChoices[j].id));
                            }
                        }
                    }
                    else {
                        vm.responses.all.push(response);
                    }
                }
            }
        });
        formatResponsesAnswer();

        $scope.safeApply();
    };

    // Global functions

    vm.send = async () : Promise<void> => {
        let validatedQuestionIds = getQuestionIdsFromPositionHistoric();
        vm.responses.all = vm.responses.all.filter(r => validatedQuestionIds.indexOf(r.question_id) >= 0);

        if (await checkMandatoryQuestions(validatedQuestionIds)) {
            template.open('lightbox', 'lightbox/responses-confirm-sending');
            vm.display.lightbox.sending = true;
        }
        else {
            notify.error(idiom.translate('formulaire.public.warning.send.missing.responses.missing'));
        }
    };

    vm.doSend = async () : Promise<void> => {
        await publicService.sendResponses(vm.formKey, vm.distributionKey, vm.responses);

        template.close('lightbox');
        vm.display.lightbox.sending = false;
        notify.success(idiom.translate('formulaire.public.success.responses.save'));
        window.setTimeout(function () {
            sessionStorage.clear();
            template.open('main', 'containers/end/thanks');
        }, 1000);
    };

    // Utils

    const syncWithStorageData = () : void => {
        vm.form = Mix.castAs(Form, JSON.parse(sessionStorage.getItem('form')));
        vm.formKey = JSON.parse(sessionStorage.getItem('formKey'));
        vm.distributionKey = JSON.parse(sessionStorage.getItem('distributionKey'));
        vm.nbFormElements = JSON.parse(sessionStorage.getItem('nbFormElements'));
        vm.historicPosition = JSON.parse(sessionStorage.getItem('historicPosition'));
        let dataFormElements = JSON.parse(sessionStorage.getItem('formElements'));
        let dataResponsesInfos = JSON.parse(sessionStorage.getItem('allResponsesInfos'));
        PublicUtils.formatStorageData(dataFormElements, vm.formElements, dataResponsesInfos, vm.allResponsesInfos);
    };

    const formatResponsesAnswer = () : void => {
        let allChoices = (vm.formElements.getAllQuestions().all.map(q => q.choices.all) as any).flat();
        for (let response of vm.responses.all) {
            if (response.choice_id && response.choice_id > 0) {
                response.answer = allChoices.filter(c => c.id === response.choice_id)[0].value;
            }
        }
    };

    const getQuestionIdsFromPositionHistoric = () : number[] => {
        let validatedElements = new FormElements();
        validatedElements.all = vm.formElements.all.filter(e => vm.historicPosition.indexOf(e.position) >= 0);
        return validatedElements.getAllQuestions().all.map(q => q.id);
    }

    const checkMandatoryQuestions = async (validatedQuestionIds: number[]) : Promise<boolean> => {
        let mandatoryQuestions = vm.formElements.getAllQuestions().all.filter(q => q.mandatory && validatedQuestionIds.indexOf(q.id) >= 0);
        for (let question of mandatoryQuestions) {
            let responses = vm.responses.all.filter(r => r.question_id === question.id && r.answer);
            if (responses.length <= 0) {
                return false;
            }
        }
        return true;
    };
}]);