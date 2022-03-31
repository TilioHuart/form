import {idiom, model, ng, notify} from "entcore";
import {Distribution, Form, FormElement, FormElements, Question, Response, Responses, Section, Types} from "../models";
import {responseFileService, responseService} from "../services";
import {FORMULAIRE_BROADCAST_EVENT, FORMULAIRE_EMIT_EVENT, FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "../core/enums";

interface ViewModel {
    formElements: FormElements;
    formElement: FormElement;
    responses: Responses;
    distribution: Distribution;
    selectedIndexList: any;
    responsesChoicesList: any;
    filesList: any;
    form: Form;
    nbFormElements: number;
    loading : boolean;
    historicPosition: number[];

    $onInit() : Promise<void>;
    prev() : Promise<void>;
    prevGuard() : void;
    next() : Promise<void>;
    nextGuard() : void;
    saveAndQuit() : Promise<void>;
    saveAndQuitGuard() : void;
}

export const respondQuestionController = ng.controller('RespondQuestionController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.formElements = new FormElements();
    vm.responses = new Responses();
    vm.distribution = new Distribution();
    vm.form = new Form();
    vm.nbFormElements = 1;
    vm.loading = true;

    vm.$onInit = async () : Promise<void> => {
        vm.loading = true;
        vm.form = $scope.form;
        vm.distribution = $scope.distribution;
        await vm.formElements.sync(vm.form.id);
        vm.formElement = vm.formElements.all[$scope.responsePosition - 1];
        vm.nbFormElements = vm.formElements.all.length;
        vm.historicPosition = $scope.historicPosition;

        await initFormElementResponses();

        window.setTimeout(() => vm.loading = false,500);
        $scope.safeApply();
    };

    const initFormElementResponses = async () : Promise<void> => {
        vm.responses = new Responses();
        vm.selectedIndexList = [];
        vm.responsesChoicesList = [];
        vm.filesList = [];

        let nbQuestions = vm.formElement instanceof Question ? 1 : (vm.formElement as Section).questions.all.length;
        for (let i = 0; i < nbQuestions; i++) {
            vm.responses.all.push(new Response());
            let question = vm.formElement instanceof Question ? vm.formElement : (vm.formElement as Section).questions.all[i];
            vm.selectedIndexList.push(new Array<boolean>(question.choices.all.length));
            vm.responsesChoicesList.push(new Responses());
            vm.filesList.push(new Array<File>());
        }

        $scope.safeApply();
        $scope.$broadcast(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.REFRESH_QUESTION);
    };

    vm.prev = async () : Promise<void> => {
        if (await saveResponses()) {
            let prevPosition = vm.historicPosition[vm.historicPosition.length - 2];
            if (prevPosition > 0) {
                vm.formElement = vm.formElements.all[prevPosition - 1];
                vm.historicPosition.pop();
                await initFormElementResponses();
                window.scrollTo(0, 0);
                $scope.safeApply();
            }
        }
    };

    vm.prevGuard = () => {
        vm.prev().then();
    };

    vm.next = async () : Promise<void> => {
        if (await saveResponses()) {
            let nextPosition = getNextPosition();
            if (nextPosition && nextPosition <= vm.nbFormElements) {
                vm.formElement = vm.formElements.all[nextPosition - 1];
                vm.historicPosition.push(vm.formElement.position);
                await initFormElementResponses();
                window.scrollTo(0, 0);
                $scope.safeApply();
            }
            else {
                let data = {
                    path: `/form/${vm.form.id}/${vm.distribution.id}/questions/recap`,
                    historicPosition: vm.historicPosition
                };
                $scope.$emit(FORMULAIRE_EMIT_EVENT.REDIRECT, data);
            }
        }

    };

    vm.nextGuard = () => {
        vm.next().then();
    };

    const getNextPosition = () : number => {
        let nextPosition: number = vm.formElement.position + 1;
        let conditionalQuestion = null;
        let response = null;

        if (vm.formElement instanceof Question && vm.formElement.conditional) {
            conditionalQuestion = vm.formElement;
            response = vm.responses.all[0];
        }
        else if (vm.formElement instanceof Section) {
            let conditionalQuestions = vm.formElement.questions.all.filter(q => q.conditional);
            if (conditionalQuestions.length === 1) {
                conditionalQuestion = conditionalQuestions[0];
                response = vm.responses.all[conditionalQuestion.section_position - 1];
            }
        }

        if (conditionalQuestion && response) {
            let choices = conditionalQuestion.choices.all.filter(c => c.id === response.choice_id);
            let sectionId = choices.length === 1 ? choices[0].next_section_id : null;
            let filteredSections = vm.formElements.getSections().all.filter(s => s.id === sectionId);
            let targetSection = filteredSections.length === 1 ? filteredSections[0] : null;
            nextPosition = targetSection ? targetSection.position : null;
        }

        return nextPosition;
    };

    vm.saveAndQuit = async () : Promise<void> => {
        if (await saveResponses()) {
            notify.success(idiom.translate('formulaire.success.responses.save'));
            window.setTimeout(function () { $scope.redirectTo(`/list/responses`); }, 1000);
        }
    };

    vm.saveAndQuitGuard = () => {
        vm.saveAndQuit().then();
    };

    const saveResponses = async () : Promise<boolean> => {
        let isSavingOk = false;

        if (!vm.loading) {
            if (vm.formElement instanceof Question) {
                isSavingOk = await saveQuestionResponse(vm.formElement, vm.responses.all[0], vm.selectedIndexList[0], vm.responsesChoicesList[0], vm.filesList[0]);
            }
            else if (vm.formElement instanceof Section) {
                for (let question of vm.formElement.questions.all) {
                    let section_position = question.section_position - 1;
                    let response = vm.responses.all[section_position];
                    let selectedIndex = vm.selectedIndexList[section_position];
                    let responsesChoices = vm.responsesChoicesList[section_position];
                    let files = vm.filesList[section_position];
                    await saveQuestionResponse(question, response, selectedIndex, responsesChoices, files);
                }
                isSavingOk = true;
            }
        }

        if (isSavingOk) {
            $scope.$broadcast(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.DESTROY_FILE_PICKER);
            return true;
        }
        else {
            return false;
        }
    };

    const saveQuestionResponse = async (question: Question, response?: Response, selectedIndex?: boolean[], responsesChoices?: Responses, files?: File[]) : Promise<boolean> => {
        if (question.question_type === Types.MULTIPLEANSWER && selectedIndex && responsesChoices) {
            let responsesToDelete = new Responses();
            for (let i = 0; i < question.choices.all.length; i++) {
                let checked = selectedIndex[i];
                let j = 0;
                let found = false;
                while (!found && j < responsesChoices.all.length) {
                    found = question.choices.all[i].id === responsesChoices.all[j].choice_id;
                    j++;
                }
                if (!found && checked) {
                    let newResponse = new Response(question.id, question.choices.all[i].id,
                        question.choices.all[i].value, vm.distribution.id);
                    await responseService.create(newResponse);
                }
                else if (found && !checked) {
                    responsesToDelete.all.push(responsesChoices.all[j - 1]);
                }
            }

            if (responsesToDelete.all.length > 0) { await responseService.delete(vm.form.id, responsesToDelete.all); }
            return true;
        }
        if ((question.question_type === Types.SINGLEANSWER || question.question_type === Types.SINGLEANSWERRADIO) && response) {
            if (!response.choice_id) {
                response.answer = "";
            } else {
                for (let choice of question.choices.all) {
                    if (response.choice_id == choice.id) {
                        response.answer = choice.value;
                    }
                }
            }
        }
        response = await responseService.save(response, question.question_type);
        if (question.question_type === Types.FILE && files) {
            return (await saveFiles(response, files));
        }
        return true;
    };

    const saveFiles = async (response: Response, files: File[]) : Promise<boolean> => {
        if (files.length > 10) {
            notify.info(idiom.translate('formulaire.response.file.tooMany'));
            return false;
        }
        else {
            await responseFileService.deleteAll(response.id);
            for (let i = 0; i < files.length; i++) {
                let filename = files[i].name;
                if (files[i].type && !$scope.form.anonymous) {
                    filename = model.me.firstName + model.me.lastName + "_" + filename;
                }
                let file = new FormData();
                file.append("file", files[i], filename);
                await responseFileService.create(response.id, file);
            }
            response.answer = idiom.translate('formulaire.response.file.send');
            response = await responseService.update(response);
            return true;
        }
    };

    $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_RESPOND_QUESTION, () => { vm.$onInit() });
}]);