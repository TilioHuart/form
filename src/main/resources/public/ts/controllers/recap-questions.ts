import {idiom, model, ng, notify, template} from "entcore";
import {
    Distribution,
    DistributionStatus, Form,
    Question, QuestionChoice, Questions,
    Responses,
    Types
} from "../models";
import {distributionService, questionService, responseFileService, responseService} from "../services";
import {FORMULAIRE_BROADCAST_EVENT} from "../core/enums";

interface ViewModel {
    questions: Questions;
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
    getStringResponse(question: Question) : string;
    isSelectedChoice(question: Question, choice: QuestionChoice) : boolean;
    getResponseFileNames(question: Question) : string[];
    saveAndQuit() : Promise<void>;
    send() : Promise<void>;
    doSend() : Promise<void>;
    checkMultiEtab() : boolean;
    getStructures() : string[];
}

export const recapQuestionsController = ng.controller('RecapQuestionsController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.questions = new Questions();
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
        vm.form.nb_questions = $scope.getDataIf200(await questionService.countQuestions(vm.form.id)).count;
        vm.distribution = $scope.distribution;
        await vm.questions.sync(vm.form.id);
        await vm.responses.syncByDistribution(vm.distribution.id);
        let fileQuestions = vm.questions.all.filter(q => q.question_type === Types.FILE);
        for (let fileQuestion of fileQuestions) {
            let response = vm.responses.all.filter(r => r.question_id === fileQuestion.id)[0];
            if(response){
                await response.files.sync(response.id);
            }
        }
        $scope.safeApply();
    };

    // Global functions

    vm.prev = async () : Promise<void> => {
        $scope.redirectTo(`/form/${vm.form.id}/${vm.distribution.id}/question/${vm.form.nb_questions}`);
    };

    // Display helper functions

    vm.getStringResponse = (question) : string => {
        let responses = vm.responses.all.filter(r => r.question_id === question.id);
        let missingResponse = "<em>" + idiom.translate('formulaire.response.missing') + "</em>";
        if (responses && responses.length > 0) {
            let answer = responses[0].answer.toString();
            return answer ? answer : missingResponse;
        }
        return missingResponse;
    };

    vm.isSelectedChoice = (question, choice) : boolean => {
        let selectedChoices: any = vm.responses.all.filter(r => r.question_id === question.id).map(r => r.choice_id);
        return selectedChoices.includes(choice.id);
    };

    vm.getResponseFileNames = (question) : string[] => {
        let missingResponse = "<em>" + idiom.translate('formulaire.response.missing') + "</em>";
        let responses = vm.responses.all.filter(r => r.question_id === question.id);
        if (responses && responses.length === 1 && responses[0].files.all.length > 0) {
            return responses[0].files.all.map(rf => rf.filename.substring(rf.filename.indexOf("_") + 1));
        }
        return [missingResponse];
    };

    // Sending actions

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
        vm.distribution.status = DistributionStatus.FINISHED;
        vm.distribution.structure = !!vm.distribution.structure ? vm.distribution.structure : model.me.structureNames[0];
        await responseService.fillResponses(vm.form.id, vm.distribution.id);
        if (vm.distribution.original_id) {
            let questionFileIds: any = vm.questions.all.filter(q => q.question_type === Types.FILE).map(q => q.id);
            let responseFiles = vm.responses.all.filter(r => questionFileIds.includes(r.question_id));
            for (let responseFile of responseFiles) {
                await responseFileService.deleteAll(responseFile.original_id);
            }
            await distributionService.replace(vm.distribution);
        }
        else {
            await distributionService.update(vm.distribution);
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
        let mandatoryQuestions = vm.questions.filter(question => question.mandatory === true);
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