import {idiom, ng, notify, template} from "entcore";
import {Form, FormElement, FormElements, Question, Response, Responses, Types} from "@common/models";
import {Mix} from "entcore-toolkit";
import {PublicUtils} from "@common/utils";

interface ViewModel {
    formKey: string;
    distributionKey: string;
    formElement: FormElement;
    form: Form;
    formElements: FormElements;
    allResponsesInfos: Map<FormElement, Map<Question, Responses>>;
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
        await initRecapController();
    };

    const initRecapController = async () : Promise<void> => {
        syncWithStorageData();
        vm.formElements.all = vm.formElements.all.filter((e: FormElement) => vm.historicPosition.indexOf(e.position) >= 0);
        let questionIdsToDisplay: number[] = getQuestionIdsFromPositionHistoric();
        vm.allResponsesInfos.forEach((questionsResponsesMap: Map<Question, Responses>) => {
            questionsResponsesMap.forEach((responses: Responses, question: Question) => {
                if (questionIdsToDisplay.some(id => (responses.all.map((r: Response) => r.question_id) as any).includes(id))) {
                    vm.responses.all = vm.responses.all.concat(question.isTypeMultipleRep() ? responses.selected : responses.all);
                }
            });
        });
        formatResponsesAnswer();

        $scope.safeApply();
    }

    // Global functions

    vm.goCaptcha = async () : Promise<void> => {
        let validatedQuestionIds: number[] = getQuestionIdsFromPositionHistoric();
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
        vm.distributionKey = JSON.parse(sessionStorage.getItem('distributionKey'));
        vm.form = Mix.castAs(Form, JSON.parse(sessionStorage.getItem('form')));
        vm.formKey = JSON.parse(sessionStorage.getItem('formKey'));
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
        let questions = vm.formElements.getAllQuestionsAndChildren();
        return questions.all.map((q: Question) => q.id).concat(questions.all.map((q: Question) => q.matrix_id));
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