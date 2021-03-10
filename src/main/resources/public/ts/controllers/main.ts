import {Behaviours, idiom, model, ng, template} from 'entcore';
import {DistributionStatus, Form, Question, QuestionTypes} from "../models";
import {distributionService, formService, questionService} from "../services";
import {AxiosResponse} from "axios";
import {FORMULAIRE_EMIT_EVENT} from "../core/enums/formulaire-event";

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location', 'FormService',
	($scope, route, $location) => {
		$scope.lang = idiom;
		$scope.template = template;

		// Init variables
		$scope.currentPage = 'formsList';
		$scope.form = new Form();
		$scope.question = new Question();
		$scope.questionTypes = new QuestionTypes();
		$scope.questionTypes.sync();

		// Routing & template opening

		route({
			list: () => {
				$scope.currentPage = 'list';
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
				$scope.currentPage = 'formsList';
				if ($scope.canCreate()) {
					template.open('main', 'containers/forms-list');
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			formsResponses: () => {
				$scope.currentPage = 'formsResponses';
				if ($scope.canRespond()) {
					template.open('main', 'containers/forms-responses');
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			createForm: () => {
				$scope.currentPage = 'createForm';
				if ($scope.canCreate()) {
					$scope.form = new Form();
					template.open('main', 'containers/prop-form');
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			propForm: async (params) => {
				$scope.currentPage = 'propForm';
				if ($scope.canCreate()) {
					$scope.form.setFromJson($scope.getDataIf200(await formService.get(params.idForm)));
					template.open('main', 'containers/prop-form');
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			openForm: async (params) => {
				$scope.currentPage = 'openForm';
				if ($scope.canCreate()) {
					$scope.form.setFromJson($scope.getDataIf200(await formService.get(params.idForm)));
					if (!!$scope.form.id) {
						template.open('main', 'containers/edit-form');
					}
					else {
						$scope.redirectTo('/list/mine');
					}
				}
				else if ($scope.canRespond()) {
					$scope.redirectTo(`/form/${params.idForm}/question/1`);
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			respondQuestion: async (params) => {
				$scope.currentPage = 'respondQuestion';
				if ($scope.canRespond()) {
					let distribution = $scope.getDataIf200(await distributionService.get(params.idForm));
					$scope.form.setFromJson($scope.getDataIf200(await formService.get(params.idForm)));

					// If form not already responded && date ok
					if (!!distribution.status && distribution.status != DistributionStatus.FINISHED &&
						$scope.form.date_opening < new Date() &&
						($scope.form.date_ending ? ($scope.form.date_ending > new Date()) : true)) {
							$scope.form.nbQuestions = $scope.getDataIf200(await questionService.countQuestions(params.idForm)).count;

							if (params.position < 1) {
								$scope.redirectTo(`/form/${params.idForm}/question/1`);
							}
							else if (params.position > $scope.form.nbQuestions) {
								$scope.redirectTo(`/form/${params.idForm}/question/${$scope.form.nbQuestions}`);
							}
							else {
								$scope.question = $scope.getDataIf200(await questionService.getByPosition(params.idForm, params.position));
								template.open('main', 'containers/respond-question');
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
				$scope.currentPage = 'e403';
				template.open('main', 'containers/e403');
			},
			e404: () => {
				$scope.currentPage = 'e404';
				template.open('main', 'containers/e404');
			}
		});

		// Utils

		$scope.getTypeNameByCode = (code: number) : string => {
			return $scope.questionTypes.all.filter(type => type.code === code)[0].name;
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
			return form.owner_id === model.me.userId || form.myRights.includes(Behaviours.applicationsBehaviours.formulaire.rights.resources.manager.right);
		};

		$scope.hasShareRightContrib = (form : Form) => {
			return form.owner_id === model.me.userId || form.myRights.includes(Behaviours.applicationsBehaviours.formulaire.rights.resources.contrib.right);
		};

		$scope.hasShareRightResponse = (form : Form) => {
			return form.owner_id === model.me.userId || form.myRights.includes(Behaviours.applicationsBehaviours.formulaire.rights.resources.comment.right);
		};
}]);
