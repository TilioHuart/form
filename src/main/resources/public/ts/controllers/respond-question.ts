import {idiom, model, ng, notify} from "entcore";
import {
    Distribution, Form, FormElement,
    Question, QuestionChoices,
    Response, ResponseFiles,
    Responses, Section,
    Types
} from "../models";
import {responseFileService, responseService} from "../services";
import {FORMULAIRE_BROADCAST_EVENT} from "../core/enums";

interface ViewModel {
    formElement: FormElement;
    response: Response;
    responses: Responses;
    distribution: Distribution;
    form: Form;
    nbFormElements: number;
    selectedIndex: boolean[];
    loading : boolean;
    files: File[];

    $onInit() : Promise<void>;
    prev() : Promise<void>;
    prevGuard() : void;
    next() : Promise<void>;
    nextGuard() : void;
    goToRecap() : Promise<void>;
    saveAndQuit() : Promise<void>;
    saveAndQuitGuard() : void;
    isQuestion(formElement: FormElement) : boolean;
}

export const respondQuestionController = ng.controller('RespondQuestionController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.formElement = new Question();
    vm.response = new Response();
    vm.responses = new Responses();
    vm.distribution = new Distribution();
    vm.form = new Form();
    vm.nbFormElements = 1;
    vm.selectedIndex = [];
    vm.loading = true;
    vm.files = [];

    vm.$onInit = async () : Promise<void> => {
        vm.form = $scope.form;
        vm.distribution = $scope.distribution;
        vm.formElement = $scope.formElement;
        vm.nbFormElements = $scope.form.nbFormElements;

        if (vm.formElement instanceof Question) {
            vm.formElement.choices = new QuestionChoices();
            if (vm.formElement.question_type === Types.MULTIPLEANSWER
                || vm.formElement.question_type === Types.SINGLEANSWER
                || vm.formElement.question_type === Types.SINGLEANSWERRADIO) {
                await vm.formElement.choices.sync(vm.formElement.id);
            }
            if (vm.formElement.question_type === Types.MULTIPLEANSWER) {
                await vm.responses.syncMine(vm.formElement.id, vm.distribution.id);
                vm.selectedIndex = new Array<boolean>(vm.formElement.choices.all.length);
                for (let i = 0; i < vm.selectedIndex.length; i++) {
                    let check = false;
                    let j = 0;
                    while (!check && j < vm.responses.all.length) {
                        check = vm.formElement.choices.all[i].id === vm.responses.all[j].choice_id;
                        j++;
                    }
                    vm.selectedIndex[i] = check;
                }
            }
            else {
                vm.response = new Response();
                let responses = await responseService.listMineByDistribution(vm.formElement.id, vm.distribution.id);
                if (responses.length > 0) {
                    vm.response = responses[0];
                }
                if (!vm.response.question_id) { vm.response.question_id = vm.formElement.id; }
                if (!vm.response.distribution_id) { vm.response.distribution_id = vm.distribution.id; }
            }
            if (vm.formElement.question_type === Types.TIME) { formatTime() }
            if (vm.formElement.question_type === Types.FILE) {
                vm.files = [];
                if (vm.response.id) {
                    let responseFiles = new ResponseFiles();
                    await responseFiles.sync(vm.response.id);
                    for (let repFile of responseFiles.all) {
                        if (repFile.id) {
                            let file = new File([repFile.id], repFile.filename);
                            vm.files.push(file);
                        }
                    }
                }
                $scope.$broadcast(FORMULAIRE_BROADCAST_EVENT.DISPLAY_FILES, vm.files);
            }
        }

        vm.loading = false;
        window.setTimeout(() => vm.loading = true,500);
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

    vm.isQuestion = (formElement) : boolean => {
        return formElement instanceof Question;
    };

    const saveResponses = async () : Promise<boolean> => {
        if(vm.loading){
            if (vm.formElement instanceof Question) {
                if (vm.formElement.question_type === Types.MULTIPLEANSWER) {
                    for (let i = 0; i < vm.formElement.choices.all.length; i++) {
                        let checked = vm.selectedIndex[i];
                        let j = 0;
                        let found = false;
                        while (!found && j < vm.responses.all.length) {
                            found = vm.formElement.choices.all[i].id === vm.responses.all[j].choice_id;
                            j++;
                        }
                        if (!found && checked) {
                            let newResponse = new Response(vm.formElement.id, vm.formElement.choices.all[i].id,
                                vm.formElement.choices.all[i].value, vm.distribution.id);
                            await responseService.create(newResponse);
                        } else if (found && !checked) {
                            await responseService.delete(vm.responses.all[j - 1].id);
                        }
                    }
                    return true;
                }
                if (vm.formElement.question_type === Types.SINGLEANSWER || vm.formElement.question_type === Types.SINGLEANSWERRADIO) {
                    if (!vm.response.choice_id) {
                        vm.response.answer = "";
                    } else {
                        for (let choice of vm.formElement.choices.all) {
                            if (vm.response.choice_id == choice.id) {
                                vm.response.answer = choice.value;
                            }
                        }
                    }
                }
                vm.response = await responseService.save(vm.response, vm.formElement.question_type);
                if (vm.formElement.question_type === Types.FILE) {
                    return (await saveFiles());
                }
                return true;
            }
            else if (vm.formElement instanceof Section) {
                // TODO save each response to each vm.formElement.questions
                return true;
            }
        }
        return false;
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
                if (vm.files[i].type && !$scope.form.anonymous) {
                    filename = model.me.firstName + model.me.lastName + "_" + filename;
                }
                let file = new FormData();
                file.append("file", vm.files[i], filename);
                await responseFileService.create(vm.response.id, file);
            }
            vm.response.answer = idiom.translate('formulaire.response.file.send');
            vm.response = await responseService.update(vm.response);
            return true;
        }
    };

    const formatTime = () : void => {
        if (vm.response.answer) {
            vm.response.answer = new Date("January 01 1970 " + vm.response.answer);
        }
    };

    $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_RESPOND_QUESTION, () => { vm.$onInit() });
}]);