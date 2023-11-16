import {currentLanguage, idiom, ng, notify} from "entcore";
import {
    Distribution, Files,
    Form,
    FormElement,
    FormElements,
    Question,
    QuestionChoice,
    Response,
    Responses,
    Section,
    Types
} from "../models";
import {responseFileService, responseService} from "../services";
import {FORMULAIRE_BROADCAST_EVENT, FORMULAIRE_EMIT_EVENT} from "@common/core/enums";
import {FormElementType} from "@common/core/enums/form-element-type";
import {FormElementUtils, UtilsUtils} from "@common/utils";
import {Constants, Fields} from "@common/core/constants";

interface ViewModel {
    formElements: FormElements;
    formElement: FormElement;
    distribution: Distribution;
    selectedIndexList: any;
    responsesChoicesList: any;
    filesList: any;
    form: Form;
    nbFormElements: number;
    longestPath: number;
    loading : boolean;
    historicPosition: number[];
    currentResponses: Map<Question, Responses>;
    currentFiles: Map<Question, Files>;
    isProcessing: boolean;

    $onInit() : Promise<void>;
    prev() : Promise<void>;
    prevGuard() : void;
    next() : Promise<void>;
    nextGuard() : void;
    saveAndQuit() : Promise<void>;
    saveAndQuitGuard() : void;
    getHtmlDescription(description: string) : string;
}

export const respondQuestionController = ng.controller('RespondQuestionController', ['$scope', '$sce',
    function ($scope, $sce) {

    const vm: ViewModel = this;
    vm.formElements = new FormElements();
    vm.distribution = new Distribution();
    vm.form = new Form();
    vm.nbFormElements = 1;
    vm.longestPath = 1;
    vm.loading = true;
    vm.currentResponses = new Map();
    vm.currentFiles = new Map();

    vm.$onInit = async () : Promise<void> => {
        await initRespondQuestionController();
    };
    const initRespondQuestionController = async () : Promise<void> => {
        vm.loading = true;
        vm.form = $scope.form;
        vm.distribution = $scope.distribution;
        await vm.formElements.sync(vm.form.id);
        vm.formElement = vm.formElements.all[$scope.responsePosition - 1];
        vm.nbFormElements = vm.formElements.all.length;
        vm.historicPosition = $scope.historicPosition.length > 0 ? $scope.historicPosition : [1];
        vm.longestPath = vm.historicPosition.length + FormElementUtils.findLongestPathInFormElement(vm.formElement.id, vm.formElements) - 1;
        await initFormElementResponses();
        window.setTimeout(() => vm.loading = false,500);
        $scope.safeApply();
    }

    const initFormElementResponses = async (): Promise<void> => {
        let nbQuestions: number = vm.formElement instanceof Question ? 1 : (vm.formElement as Section).questions.all.length;
        for (let i = 0; i < nbQuestions; i++) {
            //Check if it's a question or a section
            let question: Question = vm.formElement instanceof Question ? vm.formElement : (vm.formElement as Section).questions.all[i];
            let questionResponses: Responses = new Responses();

            if (question.isTypeMultipleRep()) {
                for (let choice of question.choices.all) {
                    if (question.children.all.length > 0) {
                        for (let child of question.children.all) {
                            questionResponses.all.push(new Response(child.id, choice.id, choice.value, vm.distribution.id));
                        }
                    } else if (!choice.is_custom) {
                        questionResponses.all.push(new Response(question.id, choice.id, choice.value, vm.distribution.id));
                    } else {
                        questionResponses.all.push(new Response(question.id, choice.id, null, vm.distribution.id));
                    }
                }
            } else if (question.question_type === Types.CURSOR && question.specific_fields) {
                let newResponse: Response = new Response(question.id, null, question.specific_fields.cursor_min_val, vm.distribution.id)
                //Check if the answer is a number
                newResponse.answer = !Number.isFinite(newResponse.answer) ? question.specific_fields.cursor_min_val : newResponse.answer;
                questionResponses.all.push(newResponse);
            } else if (question.isRanking()) {
                let questionChoices: QuestionChoice[] = question.choices.all;
                for (let i: number = 0; i < questionChoices.length; i++) {
                    let questionChoice: QuestionChoice = questionChoices[i];
                    questionResponses.all.push(new Response(question.id, questionChoice.id, questionChoice.value, vm.distribution.id, questionChoice.position));
                }
            } else {
                questionResponses.all.push(new Response(question.id, null, null, vm.distribution.id));
            }

            vm.currentResponses.set(question, questionResponses);
            if (!vm.currentFiles.has(question)) vm.currentFiles.set(question, new Files());
        }
        await fillResponses();
        $scope.safeApply();
    };

    const fillResponses = async (): Promise<void> => {
        //If in edit
        if (!this.distribution)
            return;


        //Check if formElement is a Question or a Section
        const questions: Question[] = vm.formElement instanceof Question ? [vm.formElement] : (vm.formElement as Section).questions.all;
        const allResponses: Responses = new Responses();
        await allResponses.syncMineByQuestionIds(questions.map((q: Question) => q.id), vm.distribution.id);

        //Group responses by question id
        const responsesByQuestionId: Map<Question, Responses> = new Map();
        for (const question of questions) {
            let responses: Responses = new Responses();
            responses.all = allResponses.all.filter((response: Response) => response.question_id === question.id);
            responsesByQuestionId.set(question, responses);
        }

        const promises = Array.from(responsesByQuestionId.entries()).map(
            ([question, responses]: [Question, Responses]) => processResponses(question, responses)
        );

        await Promise.all(promises);
    }

    const processResponses = async (question: Question, newResponses: Responses) => {
        //Contain blank responses initialised in initFormElementResponses for question we are processing
        let blankResponses: Responses = vm.currentResponses.get(question);

        if (question.isTypeMultipleRep()) {
            blankResponses.all.forEach((response: Response) => {
                // If the response is in the new responses, it means user answered it, so we set it to selected
                if (newResponses.all.some((newResponse: Response) => newResponse.choice_id == response.choice_id)) {
                    let newResponse: Response = newResponses.all.find((dataResponse: Response) => dataResponse.choice_id == response.choice_id);
                    newResponse.selected = true;
                    let index: number = blankResponses.all.indexOf(response);
                    blankResponses.all[index] = newResponse;
                }
            })
            // replace blankResponses by modified ones
            vm.currentResponses.set(question, blankResponses);
        } else {
            if (newResponses.all.length >= 1) {
                if (question.question_type == Types.TIME) {
                    newResponses.all[0].answer = new Date(Fields.JANUARY_01_1970 + newResponses.all[0].answer);
                } else if (question.question_type == Types.CURSOR) {
                    let answer: number = Number.parseInt(newResponses.all[0].answer.toString());
                    newResponses.all[0].answer = Number.isNaN(answer) ? this.question.specific_fields.cursor_min_val : answer;
                }
                // replace blankResponses by the one from the user
                blankResponses.all = newResponses.all;
                vm.currentResponses.set(question, blankResponses);
            }
        }
        blankResponses.hasLoaded = true;
    };


    vm.prev = async () : Promise<void> => {
        let prevPosition: number = vm.historicPosition[vm.historicPosition.length - 2];
        vm.isProcessing = true;
        if (prevPosition > 0) {
            unloadLastResponses();
            await saveResponses();
            vm.formElement = vm.formElements.all[prevPosition - 1];
            vm.historicPosition.pop();
            vm.longestPath = vm.historicPosition.length + FormElementUtils.findLongestPathInFormElement(vm.formElement.id, vm.formElements) - 1;
            goToFormElement();
        }
        vm.isProcessing = false;
    };

    vm.prevGuard = () => {
        vm.prev().then();
    };

    vm.next = async () : Promise<void> => {
        let nextPosition: number = getNextPositionIfValid();
        vm.isProcessing = true;
        if (nextPosition && nextPosition <= vm.nbFormElements) {
            unloadLastResponses();
            await saveResponses();
            vm.formElement = vm.formElements.all[nextPosition - 1];
            vm.historicPosition.push(vm.formElement.position);
            vm.longestPath = vm.historicPosition.length + FormElementUtils.findLongestPathInFormElement(vm.formElement.id, vm.formElements) - 1;
            vm.isProcessing = false;
            goToFormElement();
        }
        else if (nextPosition !== undefined) {
            await saveResponses();
            let data = {
                path: `/form/${vm.form.id}/${vm.distribution.id}/questions/recap`,
                historicPosition: vm.historicPosition
            };
            vm.isProcessing = false;
            $scope.$emit(FORMULAIRE_EMIT_EVENT.REDIRECT, data);
        }
        vm.isProcessing = false;
    };

    vm.nextGuard = () => {
        vm.next().then();
    };

    const goToFormElement = () : void => {
        initFormElementResponses();
        window.scrollTo(0, 0);
        $scope.safeApply();
    };

    const getNextPositionIfValid = () : number => {
        let nextPosition: number = vm.formElement.position + 1;
        let conditionalQuestion: Question = null;
        let response: Response = null;

        if (vm.formElement instanceof Question && vm.formElement.conditional) {
            conditionalQuestion = vm.formElement;
            response = vm.currentResponses.get(conditionalQuestion).all[0];
        }
        else if (vm.formElement instanceof Section) {
            let conditionalQuestions = vm.formElement.questions.all.filter((q: Question) => q.conditional);
            if (conditionalQuestions.length === 1) {
                conditionalQuestion = conditionalQuestions[0];
                response = vm.currentResponses.get(conditionalQuestion).all[0];
            }
        }

        if (conditionalQuestion && response && !response.choice_id) {
            notify.info('formulaire.response.next.invalid');
            nextPosition = undefined;
        }
        else if (conditionalQuestion && response) {
            let choices: QuestionChoice[] = conditionalQuestion.choices.all.filter((c: QuestionChoice) => c.id === response.choice_id);
            let nextElementId: number = choices.length === 1 ? choices[0].next_form_element_id : null;
            let nextElementType: FormElementType = choices.length === 1 ? choices[0].next_form_element_type : null;
            let filteredElements: FormElement[] = vm.formElements.all.filter((e: FormElement) => e.id === nextElementId && e.form_element_type == nextElementType);
            let targetedElement: FormElement = filteredElements.length === 1 ? filteredElements[0] : null;
            nextPosition = targetedElement ? targetedElement.position : null;
        }
        else if (vm.formElement instanceof Section && vm.formElement.questions.all.filter((q: Question) => q.conditional).length == 0) {
            nextPosition = vm.formElement.getNextFormElementPosition(vm.formElements);
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

    vm.getHtmlDescription = (description: string) : string => {
        return !!description ? $sce.trustAsHtml(description) : null;
    }

    const saveResponses = async () : Promise<boolean> => {
        let isSavingOk: boolean = false;

        if (!vm.loading) {
            if (vm.formElement instanceof Question) {
                isSavingOk = await saveQuestionResponses(vm.formElement);
            }
            else if (vm.formElement instanceof Section) {
                isSavingOk = true;
                for (let question of vm.formElement.questions.all) {
                    isSavingOk = isSavingOk && await saveQuestionResponses(question);
                }
            }
        }

        return isSavingOk;
    };

    const saveQuestionResponses = async (question: Question) : Promise<boolean> => {
        let responses: Responses = vm.currentResponses.get(question);
        let files: Files = vm.currentFiles.get(question);

        if (question.isTypeMultipleRep() || question.isRanking()) {
            await responseService.deleteByQuestionAndDistribution(question.id, vm.distribution.id);
            if (responses.selected.length > 0) {
                for (let response of responses.selected) {
                    await responseService.create(response);
                }
            }
            else if (question.children.all.length > 0) {
                for (let child of question.children.all) {
                    await responseService.create(new Response(child.id, null, null, vm.distribution.id));
                }
            }
            else if (question.isRanking()) { // In case of question type ranking, we need to add a choice index to each Response
                let promises: any = [];
                for (let resp of responses.all) {
                    promises.push(responseService.create(new Response(question.id, resp.choice_id, resp.answer, vm.distribution.id,  resp.choice_position)));
                }
                await Promise.all(promises);
            }
            else {
                await responseService.create(new Response(question.id, null, null, vm.distribution.id));
            }
            return true;
        }

        if (responses.all[0].choice_id) {
            let matchingChoices: QuestionChoice[] = question.choices.all.filter((c: QuestionChoice) => c.id === responses.all[0].choice_id);
            if (matchingChoices.length == 1) {
                if (!matchingChoices[0].is_custom) {
                    responses.all[0].answer = matchingChoices[0].value;
                    responses.all[0].custom_answer = null;
                }
                else {
                    responses.all[0].answer = null;
                }
            }
        }
        responses.all[0] = await responseService.save(responses.all[0], question.question_type);

        if (question.question_type === Types.FILE && files) {
            return (await saveFiles(question, responses.all[0], files));
        }

        return true;
    };

    const saveFiles = async (question: Question, response: Response, files: Files) : Promise<boolean> => {
        if (files.all.length > Constants.MAX_FILES_SAVE) {
            notify.info(idiom.translate('formulaire.response.file.tooMany'));
            return false;
        }
        else {
            vm.currentFiles.set(question, files);
            await responseFileService.deleteAll(response.id);
            for (let i = 0; i < files.all.length; i++) {
                let filename = files.all[i].name;
                if (files.all[i].type && !$scope.form.anonymous) {
                    filename = UtilsUtils.getOwnerNameWithUnderscore() + filename;
                }
                let file = new FormData();
                file.append("file", files.all[i], filename);
                await responseFileService.create(response.id, file);
            }
            response.answer = files.all.length > 0 ? idiom.translate('formulaire.response.file.send') : "";
            await responseService.update(response);
            return true;
        }
    };

    const unloadLastResponses = () : void => {
        if (vm.formElement instanceof Question) {
            vm.currentResponses.get(vm.formElement).hasLoaded = false;
        }
        else if (vm.formElement instanceof Section) {
            for (let question of vm.formElement.questions.all) {
                vm.currentResponses.get(question).hasLoaded = false;
            }
        }
    }

    $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_RESPOND_QUESTION, () => { initRespondQuestionController() });
}]);