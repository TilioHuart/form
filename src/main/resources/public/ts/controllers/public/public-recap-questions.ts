import {idiom, ng, notify, template} from "entcore";
import {
    Form,
    FormElement,
    FormElements,
    Responses
} from "../../models";
import { pocService } from "../../services";
import {Mix} from "entcore-toolkit";
import {PublicUtils} from "../../utils/public";

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

export const publicRecapQuestionsController = ng.controller('PublicRecapQuestionsController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.formElements = new FormElements();
    vm.form = new Form();
    vm.allResponsesInfos = new Map();
    vm.responses = new Responses();
    vm.display = {
        lightbox: {
            sending: false
        }
    };

    vm.$onInit = async () : Promise<void> => {
        syncWithStorageData();
        vm.allResponsesInfos.forEach((value) => {vm.responses.all = vm.responses.all.concat(value.responses.all)});
        formatResponsesAnswer();

    //     vm.form = $scope.form;
    //     vm.form.nb_elements = (await formElementService.countFormElements(vm.form.id)).count;
    //     vm.distribution = $scope.distribution;
    //     vm.historicPosition = $scope.historicPosition;
    //     await vm.formElements.sync(vm.form.id);
    //     await vm.responses.syncByDistribution(vm.distribution.id);
    //
    //     // Get right elements to display
    //     if (vm.historicPosition.length > 0) {
    //         vm.formElements.all = vm.formElements.all.filter(e => vm.historicPosition.indexOf(e.position) >= 0);
    //     }
    //     else {
    //         let responseQuestionIds = vm.responses.all.map(r => r.question_id);
    //         vm.formElements.all = vm.formElements.all.filter(e =>
    //             (responseQuestionIds.indexOf(e.id) > 0) ||
    //             (e instanceof Section && e.questions.all.map(q => q.id).filter(id => responseQuestionIds.indexOf(id) >= 0).length > 0)
    //         );
    //         vm.historicPosition = vm.formElements.all.map(e => e.position);
    //         vm.historicPosition.sort( (a, b) => a - b);
    //     }
    //
    //     // Get files responses for files question
    //     let fileQuestions = vm.formElements.getAllQuestions().all.filter(q => q.question_type === Types.FILE);
    //     for (let fileQuestion of fileQuestions) {
    //         let response = vm.responses.all.filter(r => r.question_id === fileQuestion.id)[0];
    //         if (response) {
    //             await response.files.sync(response.id);
    //         }
    //     }
    //     $scope.safeApply();
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
            notify.error(idiom.translate('formulaire.warning.send.missing.responses.missing'));
        }
    };

    vm.doSend = async () : Promise<void> => {
        await pocService.sendResponses(vm.formKey, vm.distributionKey, vm.responses);

        template.close('lightbox');
        vm.display.lightbox.sending = false;
        notify.success(idiom.translate('formulaire.success.responses.save'));
        window.setTimeout(function () {
            // TODO clean what's necessary
            sessionStorage.clear();
            template.open('main', 'containers/public/thanks');
        }, 1000);
    };

    // Utils

    const syncWithStorageData = () : void => {
        let data = JSON.parse(sessionStorage.getItem('data'));

        vm.form = Mix.castAs(Form, data.form);
        vm.formKey = data.formKey;
        vm.distributionKey = data.distributionKey;
        vm.nbFormElements = data.nbFormElements;
        vm.historicPosition = data.historicPosition;
        PublicUtils.formatStorageData(data, vm.formElements, vm.allResponsesInfos);
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

    // const saveResponses = async () : Promise<boolean> => {
    //     console.log("saving responses");
    //     return true;
    //
    //     let isSavingOk = false;
    //
    //     if (!vm.loading) {
    //     	if (vm.formElement instanceof Question) {
    //     		isSavingOk = await saveQuestionResponse(vm.formElement, vm.responses.all[0], vm.selectedIndexList[0], vm.responsesChoicesList[0]);
    //     	}
    //     	else if (vm.formElement instanceof Section) {
    //     		for (let question of vm.formElement.questions.all) {
    //     			let section_position = question.section_position - 1;
    //     			let response = vm.responses.all[section_position];
    //     			let selectedIndex = vm.selectedIndexList[section_position];
    //     			let responsesChoices = vm.responsesChoicesList[section_position];
    //     			await saveQuestionResponse(question, response, selectedIndex, responsesChoices);
    //     		}
    //     		isSavingOk = true;
    //     	}
    //     }
    //
    //     return isSavingOk;
    // };

    // const saveQuestionResponse = async (question: Question, response?: Response, selectedIndex?: boolean[], responsesChoices?: Responses) : Promise<boolean> => {
    // 	if (question.question_type === Types.MULTIPLEANSWER && selectedIndex && responsesChoices) {
    // 		let responsesToDelete = new Responses();
    // 		let choiceCreated = false;
    // 		for (let i = 0; i < question.choices.all.length; i++) {
    // 			let checked = selectedIndex[i];
    // 			let j = 0;
    // 			let found = false;
    // 			while (!found && j < responsesChoices.all.length) {
    // 				found = question.choices.all[i].id === responsesChoices.all[j].choice_id;
    // 				j++;
    // 			}
    // 			if (!found && checked) {
    // 				let newResponse = new Response(question.id, question.choices.all[i].id,
    // 					question.choices.all[i].value, vm.distribution.id);
    // 				await responseService.create(newResponse);
    // 				choiceCreated = true;
    // 			}
    // 			else if (found && !checked) {
    // 				responsesToDelete.all.push(responsesChoices.all[j - 1]);
    // 			}
    // 		}
    //
    // 		let emptyChoice = responsesChoices.all.filter(r => !r.choice_id);
    // 		if (emptyChoice.length > 0 && choiceCreated) { responsesToDelete.all.push(emptyChoice[0]); }
    //
    // 		if (responsesToDelete.all.length > 0) {
    // 			await responseService.delete(vm.form.id, responsesToDelete.all);
    // 		}
    // 		if ((responsesChoices.all.length <= 0 || responsesToDelete.all.length === responsesChoices.all.length) && !choiceCreated) {
    // 			await responseService.create(new Response(question.id, null, null, vm.distribution.id));
    // 		}
    // 		return true;
    // 	}
    // 	if ((question.question_type === Types.SINGLEANSWER || question.question_type === Types.SINGLEANSWERRADIO) && response) {
    // 		if (!response.choice_id) {
    // 			response.answer = "";
    // 		} else {
    // 			for (let choice of question.choices.all) {
    // 				if (response.choice_id == choice.id) {
    // 					response.answer = choice.value;
    // 				}
    // 			}
    // 		}
    // 	}
    // 	await responseService.save(response, question.question_type);
    // 	return true;
    // };
}]);