import {idiom, model, ng, template} from 'entcore';
import rights from "../rights";
import {Form, QuestionTypes} from "../models";
import {formService} from "../services";

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location', 'FormService',
	($scope, route, $location) => {
		$scope.lang = idiom;
		$scope.template = template;

		// Init variables
		$scope.currentTab = 'formsList';
		$scope.edit = {
			mode: false,
			form: new Form()
		};
		$scope.questionTypes = new QuestionTypes();
		$scope.questionTypes.sync();

		// Routing & template opening

		route({
			formsList: () => {
				$scope.currentTab = 'formsList';
				template.open('main', 'containers/forms-list');
			},
			formsResponses: () => {
				$scope.currentTab = 'formsResponses';
				template.open('main', 'containers/forms-responses');
			},
			createForm: () => {
				template.open('main', 'containers/create-form');
			},
			editForm: async (params) => {
				let { data } = await formService.get(params.idForm);
				$scope.edit.form = data;
				$scope.edit.mode = true;
				template.open('main', 'containers/edit-form');
			},
			e404: () => {
				template.open('main', 'containers/e404');
			}
		});

		// Utils

		$scope.getTypeNameByCode = (code: number) : string => {
			return $scope.questionTypes.all.filter(type => type.code === code)[0].name;
		};

		$scope.redirectTo = (path: string) => {
			$location.path(path);
		};

		$scope.safeApply = function (fn?) {
			const phase = $scope.$root.$$phase;
			if (phase == '$apply' || phase == '$digest') {
				if (fn && (typeof (fn) === 'function')) {
					fn();
				}
			} else {
				$scope.$apply(fn);
			}
		};

		$scope.hasRight = function (right: string) {
			return model.me.hasWorkflow(rights.workflow[right]);
		};
}]);
