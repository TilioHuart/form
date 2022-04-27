import {idiom, ng, notify, template} from 'entcore';
import {Form, FormElement, FormElements, Question, Response, Responses, Section} from "../../models";
import {pocService} from "../../services";
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "../../core/enums";
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

	$onInit(): Promise<void>;
	prev() : void;
	next() : void;
}

export const publicRespondQuestionController = ng.controller('PublicRespondQuestionController', ['$scope',
	function ($scope) {

	const vm: ViewModel = this;
	vm.formElements = new FormElements();
	vm.form = new Form();
	vm.nbFormElements = 1;
	vm.allResponsesInfos = new Map();

	vm.$onInit = async () : Promise<void> => {
		vm.loading = true;
		vm.formKey = $scope.formKey;
		vm.historicPosition = [1];

		// Get cookies --> Check if distrib finished

		if (!sessionStorage.getItem('data')) {
			vm.form.setFromJson(await pocService.getPublicFormByKey(vm.formKey));
			vm.form.formatFormElements(vm.formElements);

			vm.distributionKey = vm.form.getDistributionKey();
			vm.formElement = vm.formElements.all[0];
			initFormElementResponses();
			updateStorage();
		}
		else {
			syncWithStorageData();
			vm.formElement = vm.formElements.all[0];
		}

		vm.nbFormElements = vm.formElements.all.length;

		window.setTimeout(() => vm.loading = false,500);
		$scope.safeApply();
	};

	vm.prev = () : void => {
		let prevPosition = vm.historicPosition[vm.historicPosition.length - 2];
		if (prevPosition > 0) {
			vm.formElement = vm.formElements.all[prevPosition - 1];
			vm.historicPosition.pop();
			initFormElementResponses();
			window.scrollTo(0, 0);
			$scope.safeApply();
		}
	};

	vm.next = () : void => {
		let nextPosition = getNextPositionIfValid();
		if (nextPosition && nextPosition <= vm.nbFormElements) {
			vm.formElement = vm.formElements.all[nextPosition - 1];
			vm.historicPosition.push(vm.formElement.position);
			initFormElementResponses();
			updateStorage();
			window.scrollTo(0, 0);
			$scope.safeApply();
		}
		else if (nextPosition !== undefined) {
			updateStorage();
			template.open('main', 'containers/public/recap-questions');
		}
	};

	// Utils

	const getNextPositionIfValid = () : number => {
		let nextPosition: number = vm.formElement.position + 1;
		let conditionalQuestion = null;
		let response = null;

		if (vm.formElement instanceof Question && vm.formElement.conditional) {
			conditionalQuestion = vm.formElement;
			response = vm.allResponsesInfos.get(vm.formElement).responses.all[0];
		}
		else if (vm.formElement instanceof Section) {
			let conditionalQuestions = vm.formElement.questions.all.filter(q => q.conditional);
			if (conditionalQuestions.length === 1) {
				conditionalQuestion = conditionalQuestions[0];
				response = vm.allResponsesInfos.get(vm.formElement).responses.all[conditionalQuestion.section_position - 1];
			}
		}

		if (conditionalQuestion && response && !response.choice_id) {
			notify.info('formulaire.response.next.invalid');
			nextPosition = undefined;
		}
		else if (conditionalQuestion && response) {
			let choices = conditionalQuestion.choices.all.filter(c => c.id === response.choice_id);
			let sectionId = choices.length === 1 ? choices[0].next_section_id : null;
			let filteredSections = vm.formElements.getSections().all.filter(s => s.id === sectionId);
			let targetSection = filteredSections.length === 1 ? filteredSections[0] : null;
			nextPosition = targetSection ? targetSection.position : null;
		}

		return nextPosition;
	};

	const initFormElementResponses = () : void => {
		if (!vm.allResponsesInfos.has(vm.formElement)) {
			let responses = new Responses();
			let selectedIndexList = [];
			let responsesChoicesList = [];

			let nbQuestions = vm.formElement instanceof Question ? 1 : (vm.formElement as Section).questions.all.length;
			for (let i = 0; i < nbQuestions; i++) {
				responses.all.push(new Response());
				let question = vm.formElement instanceof Question ? vm.formElement : (vm.formElement as Section).questions.all[i];
				selectedIndexList.push(new Array<boolean>(question.choices.all.length));
				responsesChoicesList.push(new Responses());
			}

			let responseInfos = {
				responses: responses,
				selectedIndexList: selectedIndexList,
				responsesChoicesList: responsesChoicesList
			};
			vm.allResponsesInfos.set(vm.formElement, responseInfos);
		}

		$scope.safeApply();
		$scope.$broadcast(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.REFRESH_QUESTION);
	};

	const updateStorage = () : void => {
		let data = {
			formKey: vm.formKey,
			distributionKey: vm.distributionKey,
			form: vm.form,
			formElements: vm.formElements,
			nbFormElements: vm.nbFormElements,
			historicPosition: vm.historicPosition,
			allResponsesInfos: vm.allResponsesInfos
		};
		sessionStorage.setItem('data', JSON.stringify(data));
	};

	const syncWithStorageData = () : void => {
		let data = JSON.parse(sessionStorage.getItem('data'));

		vm.form = Mix.castAs(Form, data.form);
		vm.formKey = data.formKey;
		vm.distributionKey = data.distributionKey;
		vm.nbFormElements = data.nbFormElements;
		vm.historicPosition = data.historicPosition;
		PublicUtils.formatStorageData(data, vm.formElements, vm.allResponsesInfos);
	};

}]);