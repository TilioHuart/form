import {idiom, model, ng, notify} from "entcore";
import {
    Distribution, Form, FormElement,
    Question, QuestionChoices,
    Response, ResponseFiles,
    Responses, Section,
    Types
} from "../models";
import {responseFileService, responseService} from "../services";
import {FORMULAIRE_BROADCAST_EVENT, FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "../core/enums";

interface ViewModel {
    formElement: FormElement;
    responses: Responses;
    distribution: Distribution;
    selectedIndexList: any;
    responsesChoicesList: any;
    filesList: any;
    form: Form;
    nbFormElements: number;
    loading : boolean;

    $onInit() : Promise<void>;
    prev() : Promise<void>;
    prevGuard() : void;
    next() : Promise<void>;
    nextGuard() : void;
    goToRecap() : Promise<void>;
    saveAndQuit() : Promise<void>;
    saveAndQuitGuard() : void;
}

export const respondQuestionController = ng.controller('RespondQuestionController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.responses = new Responses();
    vm.distribution = new Distribution();
    vm.form = new Form();
    vm.nbFormElements = 1;
    vm.loading = true;

    vm.$onInit = async () : Promise<void> => {
        vm.loading = true;
        vm.form = $scope.form;
        vm.distribution = $scope.distribution;
        vm.formElement = $scope.formElement;
        vm.nbFormElements = $scope.form.nbFormElements;
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

        window.setTimeout(() => vm.loading = false,500);
        $scope.safeApply();
    };

    vm.prev = async () : Promise<void> => {
        if (await saveResponses()) {
            let prevPosition: number = vm.formElement.position - 1;
            if (prevPosition > 0) {
                $scope.redirectTo(`/form/${vm.form.id}/${vm.distribution.id}/question/${prevPosition}`);
            }
        }
    };

    vm.prevGuard = () => {
        vm.prev();
    };

    vm.next = async () : Promise<void> => {
        if (await saveResponses()) {
            let nextPosition: number = vm.formElement.position + 1;
            if (nextPosition <= vm.nbFormElements) {
                $scope.redirectTo(`/form/${vm.form.id}/${vm.distribution.id}/question/${nextPosition}`);
            }
            else {
                $scope.redirectTo(`/form/${vm.form.id}/${vm.distribution.id}/questions/recap`);
            }

        }
    };

    vm.nextGuard = () => {
        vm.next();
    };

    vm.goToRecap = async () : Promise<void> => {
        if (await saveResponses()) {
            $scope.redirectTo(`/form/${vm.form.id}/${vm.distribution.id}/questions/recap`);
        }
    };

    vm.saveAndQuit = async () : Promise<void> => {
        if (await saveResponses()) {
            // if (vm.distribution.status == DistributionStatus.TO_DO) {
            //     vm.distribution.status = DistributionStatus.IN_PROGRESS;
            //     await distributionService.update(vm.distribution);
            // }
            notify.success(idiom.translate('formulaire.success.responses.save'));
            window.setTimeout(function () { $scope.redirectTo(`/list/responses`); }, 1000);
        }
    };

    vm.saveAndQuitGuard = () => {
        vm.saveAndQuit();
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
                } else if (found && !checked) {
                    await responseService.delete(responsesChoices.all[j - 1].id);
                }
            }
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