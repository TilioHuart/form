import {moment, ng, notify, template} from 'entcore';
import {
	Form,
	FormElement,
	FormElements,
	Question,
	QuestionChoice, Questions,
	Response,
	Responses,
	Section,
	Types
} from "@common/models";
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";
import {Mix} from "entcore-toolkit";
import {PublicUtils} from "@common/utils";

interface ViewModel {
	formKey: string;
	formElements: FormElements;
	allResponsesInfos: Map<FormElement, { responses: any, selectedIndexList: any, responsesChoicesList: any }>;

	formElement: FormElement;

	form: Form;
	nbFormElements: number;
	historicPosition: number[];

	$onInit(): Promise<void>;
	prev() : void;
	next() : void;
}

export const respondQuestionController = ng.controller('RespondQuestionController', ['$scope',
	function ($scope) {

	const vm: ViewModel = this;
	vm.form = new Form();
	vm.formElements = new FormElements();
	vm.nbFormElements = 1;
	vm.allResponsesInfos = new Map();

	vm.$onInit = async () : Promise<void> => {
		syncWithStorageData();
		let formElementPosition = vm.historicPosition[vm.historicPosition.length - 1];
		vm.formElement = vm.formElements.all[formElementPosition - 1];
		initFormElementResponses();

		$scope.safeApply();
	};

	vm.prev = () : void => {
		formatResponses();
		let prevPosition = vm.historicPosition[vm.historicPosition.length - 2];
		if (prevPosition > 0) {
			vm.formElement = vm.formElements.all[prevPosition - 1];
			vm.historicPosition.pop();
			goToFormElement();
		}
	};

	vm.next = () : void => {
		formatResponses();
		let nextPosition = getNextPositionIfValid();
		if (nextPosition && nextPosition <= vm.nbFormElements) {
			vm.formElement = vm.formElements.all[nextPosition - 1];
			vm.historicPosition.push(vm.formElement.position);
			goToFormElement();
		}
		else if (nextPosition !== undefined) {
			updateStorage();
			template.open('main', 'containers/recap');
		}
	};

	// Utils

	const goToFormElement = () : void => {
		initFormElementResponses();
		updateStorage();
		window.scrollTo(0, 0);
		$scope.safeApply();
	};

	const getNextPositionIfValid = () : number => {
		let nextPosition: number = vm.formElement.position + 1;
		let conditionalQuestion: Question = null;
		let response: Response = null;

		if (vm.formElement instanceof Question && vm.formElement.conditional) {
			conditionalQuestion = vm.formElement;
			response = vm.allResponsesInfos.get(vm.formElement).responses[0];
		}
		else if (vm.formElement instanceof Section) {
			let conditionalQuestions = vm.formElement.questions.all.filter((q: Question) => q.conditional);
			if (conditionalQuestions.length === 1) {
				conditionalQuestion = conditionalQuestions[0];
				response = vm.allResponsesInfos.get(vm.formElement).responses[conditionalQuestion.section_position - 1];
			}
		}

		if (conditionalQuestion && response && !response.choice_id) {
			notify.info('formulaire.response.next.invalid');
			nextPosition = undefined;
		}
		else if (conditionalQuestion && response) {
			let choices: QuestionChoice[] = conditionalQuestion.choices.all.filter((c: QuestionChoice) => c.id === response.choice_id);
			let sectionId: number = choices.length === 1 ? choices[0].next_section_id : null;
			let filteredSections: Section[] = vm.formElements.getSections().all.filter((s: Section) => s.id === sectionId);
			let targetSection: Section = filteredSections.length === 1 ? filteredSections[0] : null;
			nextPosition = targetSection ? targetSection.position : null;
		}

		return nextPosition;
	};

	const formatResponses = () : void => {
		let questions: Question[] = vm.formElement instanceof Section ? vm.formElement.questions.all : [vm.formElement as Question];
		let responses = vm.allResponsesInfos.get(vm.formElement).responses;

		for (let i = 0; i < questions.length; i++) {
			let question: Question = questions[i];
			let questionResponses: Response[] = responses[i] instanceof Response ? [responses[i]] : responses[i].all;

			for (let response of questionResponses) {
				if (!response.answer) {
					response.answer = "";
				}
				else {
					let questionType: number = question.question_type;
					if (questionType === Types.TIME && typeof response.answer != "string") {
						response.answer = moment(response.answer).format("HH:mm");
					} else if (questionType === Types.DATE && typeof response.answer != "string") {
						response.answer = moment(response.answer).format("DD/MM/YYYY");
					}
				}
			}
		}
	};

	const initFormElementResponses = () : void => {
		if (!vm.allResponsesInfos.has(vm.formElement)) {
			let responses = [];
			let selectedIndexList = [];
			let responsesChoicesList = [];

			let nbQuestions = vm.formElement instanceof Question ? 1 : (vm.formElement as Section).questions.all.length;
			for (let i = 0; i < nbQuestions; i++) {
				let question: Question = vm.formElement instanceof Question ? vm.formElement : (vm.formElement as Section).questions.all[i];

				if (question.question_type === Types.MATRIX) {
					let questionResponse: Responses = new Responses();
					for (let child of question.children.all) {
						questionResponse.all.push(new Response());
					}
					responses.push(questionResponse);
				}
				else {
					responses.push(new Response());
				}

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
		sessionStorage.setItem('formKey', JSON.stringify(vm.formKey));
		sessionStorage.setItem('form', JSON.stringify(vm.form));
		sessionStorage.setItem('formElements', JSON.stringify(vm.formElements));
		sessionStorage.setItem('nbFormElements', JSON.stringify(vm.nbFormElements));
		sessionStorage.setItem('historicPosition', JSON.stringify(vm.historicPosition));
		sessionStorage.setItem('allResponsesInfos', JSON.stringify(vm.allResponsesInfos));
	};

	const syncWithStorageData = () : void => {
		vm.form = Mix.castAs(Form, JSON.parse(sessionStorage.getItem('form')));
		vm.formKey = JSON.parse(sessionStorage.getItem('formKey'));
		vm.nbFormElements = JSON.parse(sessionStorage.getItem('nbFormElements'));
		vm.historicPosition = JSON.parse(sessionStorage.getItem('historicPosition'));
		let dataFormElements = JSON.parse(sessionStorage.getItem('formElements'));
		let dataResponsesInfos = JSON.parse(sessionStorage.getItem('allResponsesInfos'));
		PublicUtils.formatStorageData(dataFormElements, vm.formElements, dataResponsesInfos, vm.allResponsesInfos);
	};

}]);