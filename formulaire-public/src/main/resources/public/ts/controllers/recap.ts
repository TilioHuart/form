import {idiom, ng, notify, template} from "entcore";
import {
    Form,
    FormElement,
    FormElements, Question, QuestionChoice,
    Response,
    Responses,
    Types
} from "@common/models";
import {Mix} from "entcore-toolkit";
import {PublicUtils} from "@common/utils";

interface ViewModel {
    formKey: string;
    distributionKey: string;
    formElement: FormElement;
    form: Form;
    formElements: FormElements;
    allResponsesInfos: Map<FormElement, { responses: any, selectedIndexList: any, responsesChoicesList: any }>;
    responses: Responses;
    nbFormElements: number;
    loading : boolean;
    historicPosition: number[];

    $onInit() : Promise<void>;
    goCaptcha(): Promise<void>;
}

export const recapController = ng.controller('RecapController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.form = new Form();
    vm.formElements = new FormElements();
    vm.allResponsesInfos = new Map();
    vm.responses = new Responses();

    vm.$onInit = async () : Promise<void> => {
        syncWithStorageData();
        vm.formElements.all = vm.formElements.all.filter((e: FormElement) => vm.historicPosition.indexOf(e.position) >= 0);
        let allQuestions: Question[] = vm.formElements.getAllQuestionsAndChildren().all;
        let questionIdsToDisplay: number[] = allQuestions.map((q: Question) => q.id).concat(allQuestions.map((q: Question) => q.matrix_id));
        vm.allResponsesInfos.forEach((value) => {
            for (let i = 0; i < value.responses.length; i++) {
                let response = value.responses[i];

                if (response instanceof Responses && questionIdsToDisplay.some(id => response.all.map((r: Response) => r.id).includes(id))) { // MATRIX response
                    vm.responses.all = vm.responses.all.concat(response.all);
                }
                else if (response instanceof Response && questionIdsToDisplay.indexOf(response.question_id) >= 0) { // Classic response
                    let correspondingQuestion: Question = allQuestions.filter((q: Question) => q.id === response.question_id)[0];

                    if (correspondingQuestion.question_type === Types.MULTIPLEANSWER) {
                        let questionChoices: QuestionChoice[] = correspondingQuestion.choices.all.sort((a, b) => a.id - b.id);

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

    vm.goCaptcha = async () : Promise<void> => {
        let validatedQuestionIds = getQuestionIdsFromPositionHistoric();
        vm.responses.all = vm.responses.all.filter(r => validatedQuestionIds.indexOf(r.question_id) >= 0);

        if (await checkMandatoryQuestions(validatedQuestionIds)) {
            sessionStorage.setItem('responses', JSON.stringify(vm.responses));
            template.open('main', 'containers/captcha');
        }
        else {
            notify.error(idiom.translate('formulaire.public.warning.send.missing.responses.missing'));
        }
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
        let validatedElements: FormElements = new FormElements();
        validatedElements.all = vm.formElements.all.filter((e: FormElement) => vm.historicPosition.indexOf(e.position) >= 0);
        return validatedElements.getAllQuestionsAndChildren().all.map(q => q.id);
    }

    const checkMandatoryQuestions = async (validatedQuestionIds: number[]) : Promise<boolean> => {
        let mandatoryQuestions: Question[] = vm.formElements.getAllQuestions().all.filter((q: Question) => q.mandatory && validatedQuestionIds.indexOf(q.id) >= 0);
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
}]);