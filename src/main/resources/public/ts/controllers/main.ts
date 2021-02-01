import {Behaviours, idiom, model, ng, template} from 'entcore';
import {Form, Question, QuestionTypes} from "../models";
import {formService, questionService} from "../services";
import {AxiosResponse} from "axios";

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location', 'FormService',
	($scope, route, $location) => {
		$scope.lang = idiom;
		$scope.template = template;

		// Init variables
		$scope.currentTab = 'formsList';
		$scope.editMode = false;
		$scope.form = new Form();
		$scope.question = new Question();
		$scope.questionTypes = new QuestionTypes();
		$scope.questionTypes.sync();

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
				if ($scope.canCreate()) {
					$scope.currentTab = 'formsList';
					template.open('main', 'containers/forms-list');
				}
				else {
					$scope.redirectTo('/list');
				}
			},
			formsResponses: () => {
				if ($scope.canRespond()) {
					$scope.currentTab = 'formsResponses';
					template.open('main', 'containers/forms-responses');
				}
				else {
					$scope.redirectTo('/list');
				}
			},
			createForm: () => {
				if ($scope.canCreate()) {
					template.open('main', 'containers/create-form');
				}
				else {
					$scope.redirectTo('/list');
				}
			},
			openForm: async (params) => {
				if ($scope.canCreate()) {
					let { data } = await formService.get(params.idForm);
					$scope.form = data;
					$scope.editMode = true;
					await template.open('main', 'containers/edit-form');
				}
				else if ($scope.canRespond()) {
					$scope.redirectTo(`/form/${params.idForm}/question/1`);
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			respondQuestion: async (params) => {
				if ($scope.canRespond()) {
					$scope.form = $scope.getDataIf200(await formService.get(params.idForm));
					$scope.form.nbQuestions = $scope.getDataIf200(await questionService.countQuestions(params.idForm)).count;

					if (params.position < 1) {
						$scope.redirectTo(`/form/${params.idForm}/question/1`);
					}
					else if (params.position > $scope.form.nbQuestions) {
						$scope.redirectTo(`/form/${params.idForm}/question/${$scope.form.nbQuestions}`);
					}
					else {
						$scope.question = $scope.getDataIf200(await questionService.getByPosition(params.idForm, params.position));
						await template.open('main', 'containers/respond-question');
					}
				}
				else {
					$scope.redirectTo('/e403');
				}
			},
			e403: () => {
				template.open('main', 'containers/e403');
			},
			e404: () => {
				template.open('main', 'containers/e404');
			}
		});

		// Utils

		$scope.getTypeNameByCode = (code: number) : string => {
			return $scope.questionTypes.all.filter(type => type.code === code)[0].name;
		};

		$scope.getDataIf200 = (response: AxiosResponse) : any => {
			if (response.status == 200) { return response.data; }
		};

		$scope.redirectTo = (path: string) => {
			$location.path(path);
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

		// Rights

		$scope.hasRight = (right: string) => {
			return model.me.hasWorkflow(right);
		};

		$scope.hasAccess = () => {
			return $scope.hasRight(Behaviours.applicationsBehaviours.formulaire.rights.workflow.access);
		};

		$scope.canCreate = () => {
			return $scope.hasRight(Behaviours.applicationsBehaviours.formulaire.rights.workflow.creation);
		};

		$scope.canRespond = () => {
			return $scope.hasRight(Behaviours.applicationsBehaviours.formulaire.rights.workflow.response);
		};

		// $scope.canSend = () => {
		// 	return $scope.hasRight(Behaviours.applicationsBehaviours.formulaire.rights.workflow.sending);
		// };
		//
		// $scope.canShare = () => {
		// 	return $scope.hasRight(Behaviours.applicationsBehaviours.formulaire.rights.workflow.sharing);
		// };
}]);
