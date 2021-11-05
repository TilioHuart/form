import {Behaviours, idiom, model, ng, template} from 'entcore';
import {Distribution, DistributionStatus, Form, Question, QuestionTypes, Types} from "../models";
import {distributionService, formService, questionService} from "../services";
import {AxiosResponse} from "axios";
import {
	Direction,
	Exports,
	FiltersFilters,
	FiltersOrders,
	FORMULAIRE_BROADCAST_EVENT,
	FORMULAIRE_EMIT_EVENT,
	Pages
} from "../core/enums";

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location', 'FormService',
	($scope, route, $location) => {
		$scope.lang = idiom;
		$scope.template = template;

		// Init variables
		$scope.FiltersOrders = FiltersOrders;
		$scope.FiltersFilters = FiltersFilters;
		$scope.Direction = Direction;
		$scope.Types = Types;
		$scope.Exports = Exports;
		$scope.Pages = Pages;
		$scope.currentPage = Pages.FORMS_LIST;
		$scope.form = new Form();
		$scope.distribution = new Distribution();
		$scope.question = new Question();
		$scope.questionTypes = new QuestionTypes();
		$scope.isMobile = window.screen.width <= 500;

		const init = async () : Promise<void> => {
			await $scope.questionTypes.sync();
		}

		// Routing & template opening
		route({
			list: () => {
				if ($scope.canCreate()) {
					$scope.redirectTo('/list/mine');
				}
				else if ($scope.canRespond()) {
					$scope.redirectTo('/list/responses');
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			formsList: () => {
				$scope.currentPage = Pages.FORMS_LIST;
				if ($scope.canCreate()) {
					template.open('main', 'containers/forms-list');
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			formsResponses: () => {
				$scope.currentPage = Pages.FORMS_RESPONSE;
				if ($scope.canRespond()) {
					template.open('main', 'containers/forms-responses');
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			createForm: () => {
				$scope.currentPage = Pages.CREATE_FORM;
				if ($scope.canCreate()) {
					$scope.form = new Form();
					template.open('main', 'containers/prop-form');
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			propForm: async (params) => {
				$scope.currentPage = Pages.PROP_FORM;
				await $scope.getFormWithRights(params.idForm);
				if ($scope.canCreate() && $scope.hasShareRightContrib($scope.form)) {
					$scope.$broadcast(FORMULAIRE_BROADCAST_EVENT.INIT_CONTROLLER);
					template.open('main', 'containers/prop-form');
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			emptyResults: async (params) => {
				$scope.currentPage = Pages.EMPTY_RESULTS;
				template.open('main', 'containers/empty-results');
			},
			resultsForm: async (params) => {
				$scope.currentPage = Pages.RESULTS_FORM;
				await $scope.getFormWithRights(params.idForm);
				if ($scope.canCreate() && $scope.hasShareRightContrib($scope.form)) {
					$scope.form.nb_responses = $scope.getDataIf200(await distributionService.count(params.idForm)).count;

					if ($scope.form.nb_responses > 0) {
						$scope.form.nb_questions = $scope.getDataIf200(await questionService.countQuestions(params.idForm)).count;
						if (params.position < 1) {
							$scope.redirectTo(`/form/${params.idForm}/results/1`);
						}
						else if (params.position > $scope.form.nb_questions) {
							$scope.redirectTo(`/form/${params.idForm}/results/${$scope.form.nb_questions}`);
						}
						else {
							$scope.question = $scope.getDataIf200(await questionService.getByPosition(params.idForm, params.position));
							$scope.$broadcast(FORMULAIRE_BROADCAST_EVENT.INIT_CONTROLLER);
							template.open('main', 'containers/results-form');
						}
					}
					else {
						$scope.redirectTo(`/form/${params.idForm}/results/empty`);
					}
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			editForm: async (params) => {
				$scope.currentPage = Pages.EDIT_FORM;
				await $scope.getFormWithRights(params.idForm);
				if ($scope.canCreate() && $scope.hasShareRightContrib($scope.form)) {
					if (!!$scope.form.id) {
						$scope.$broadcast(FORMULAIRE_BROADCAST_EVENT.INIT_CONTROLLER);
						template.open('main', 'containers/edit-form');
					}
					else {
						$scope.redirectTo('/list/mine');
					}
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			respondForm: async (params) => {
				await $scope.getFormWithRights(params.idForm);
				if ($scope.canRespond() && $scope.hasShareRightResponse($scope.form)) {
					$scope.redirectTo(`/form/${params.idForm}/question/1`);
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			respondQuestion: async (params) => {
				$scope.currentPage = Pages.RESPOND_QUESTION;
				await $scope.getFormWithRights(params.idForm);
				if ($scope.canRespond() && $scope.hasShareRightResponse($scope.form)) {
					let newDistrib = false;
					if ($scope.form.multiple) {
						let distribs : any = $scope.getDataIf200(await distributionService.listByFormAndResponder(params.idForm));
						let distrib: Distribution = null;
						for (let d of distribs) {
							if (d.status != DistributionStatus.FINISHED) {
								distrib = d;
								break;
							}
						}
						if (!!!distrib) {
							await distributionService.add($scope.form.id, distribs[0]);
							newDistrib = true;
						}
					}
					$scope.distribution = $scope.getDataIf200(await distributionService.get(params.idForm));

					// If form not archived && date ok && not already responded
					if (!$scope.form.archived && $scope.form.date_opening < new Date() &&
						($scope.form.date_ending ? ($scope.form.date_ending > new Date()) : true)) {
						if ($scope.form.multiple || (!!$scope.distribution.status && $scope.distribution.status != DistributionStatus.FINISHED)) {
							$scope.form.nb_questions = $scope.getDataIf200(await questionService.countQuestions(params.idForm)).count;

							if (newDistrib || params.position < 1) {
								$scope.redirectTo(`/form/${params.idForm}/question/1`);
							}
							else if (params.position > $scope.form.nb_questions) {
								$scope.redirectTo(`/form/${params.idForm}/questions/recap`);
							}
							else {
								$scope.question = $scope.getDataIf200(await questionService.getByPosition(params.idForm, params.position));
								$scope.$broadcast(FORMULAIRE_BROADCAST_EVENT.INIT_CONTROLLER);
								template.open('main', 'containers/respond-question');
							}
						}
						else {
							$scope.redirectTo('/e409');
						}
					}
					else {
						$scope.redirectTo('/e403');
					}
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			recapQuestions: async (params) => {
				$scope.currentPage = Pages.RECAP_QUESTIONS;
				await $scope.getFormWithRights(params.idForm);
				if ($scope.canRespond() && $scope.hasShareRightResponse($scope.form)) {
					$scope.distribution = $scope.getDataIf200(await distributionService.get(params.idForm));
					if (!$scope.distribution) {
						$scope.redirectTo(`/form/${params.idForm}/question/1`);
					}
					else if (!$scope.form.archived && $scope.form.date_opening < new Date() &&
						($scope.form.date_ending ? ($scope.form.date_ending > new Date()) : true)) {
						// If form not already responded
						if ($scope.form.multiple || ($scope.distribution.status && $scope.distribution.status != DistributionStatus.FINISHED)) {
							$scope.form.nb_questions = $scope.getDataIf200(await questionService.countQuestions(params.idForm)).count;
							$scope.$broadcast(FORMULAIRE_BROADCAST_EVENT.INIT_CONTROLLER);
							template.open('main', 'containers/recap-questions');
						}
						else {
							$scope.redirectTo('/e409');
						}
					}
					else {
						$scope.redirectTo('/e403');
					}
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			e403: () => {
				$scope.currentPage = Pages.E403;
				template.open('main', 'containers/error/e403');
			},
			e404: () => {
				$scope.currentPage = Pages.E404;
				template.open('main', 'containers/error/e404');
			},
			e409: () => {
				$scope.currentPage = Pages.E409;
				template.open('main', 'containers/error/e409');
			}
		});


		// Utils

		$scope.displayDate = (dateToFormat: Date) : string => {
			return new Date(dateToFormat).toLocaleString([], {day: '2-digit', month: '2-digit', year:'numeric', hour: '2-digit', minute:'2-digit'});
		};

		$scope.getI18nWithParams = (key: string, params: string[]) : string => {
			let finalI18n = idiom.translate(key);
			for (let i = 0; i < params.length; i++) {
				finalI18n = finalI18n.replace(`{${i}}`, params[i]);
			}
			return finalI18n;
		};

		$scope.getTypeNameByCode = (code: number) : string => {
			return $scope.questionTypes.all.filter(type => type.code === code)[0].name;
		};

		$scope.getFormWithRights = async (formId : number) : Promise<void> => {
			$scope.form.setFromJson($scope.getDataIf200(await formService.get(formId)));
			$scope.form.myRights = $scope.getDataIf200(await formService.getMyFormRights(formId)).map(right => right.action);
		};

		$scope.getDataIf200 = (response: AxiosResponse) : any => {
			if ($scope.isStatusXXX(response, 200)) { return response.data; }
			else { return null; }
		};

		$scope.isStatusXXX = (response: AxiosResponse, status: number) : any => {
			return response.status === status;
		};

		$scope.redirectTo = (path: string) => {
			$location.path(path);
			$scope.safeApply();
		};

		$scope.safeApply = (fn?) => {
			const phase = $scope.$root.$$phase;
			if (phase == '$apply' || phase == '$digest') {
				if (fn && (typeof (fn) === 'function')) {
					fn();
				}
			} else {
				$scope.$apply(fn);
			}
		};

		$scope.$on(FORMULAIRE_EMIT_EVENT.REFRESH, () => { $scope.safeApply() });


		// Rights

		$scope.hasRight = (right: string) => {
			return model.me.hasWorkflow(right);
		};

		$scope.canAccess = () => {
			return $scope.hasRight(Behaviours.applicationsBehaviours.formulaire.rights.workflow.access);
		};

		$scope.canCreate = () => {
			return $scope.hasRight(Behaviours.applicationsBehaviours.formulaire.rights.workflow.creation);
		};

		$scope.canRespond = () => {
			return $scope.hasRight(Behaviours.applicationsBehaviours.formulaire.rights.workflow.response);
		};

		$scope.hasShareRightManager = (form : Form) => {
			return form.owner_id === model.me.userId || (form.myRights && form.myRights.includes(Behaviours.applicationsBehaviours.formulaire.rights.resources.manager.right));
		};

		$scope.hasShareRightContrib = (form : Form) => {
			return form.owner_id === model.me.userId || (form.myRights && form.myRights.includes(Behaviours.applicationsBehaviours.formulaire.rights.resources.contrib.right));
		};

		$scope.hasShareRightResponse = (form : Form) => {
			return form.myRights.includes(Behaviours.applicationsBehaviours.formulaire.rights.resources.comment.right);
		};

		init();
}]);
