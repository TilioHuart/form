import {idiom, model, ng, notify, template, toasts} from 'entcore';
import rights from "../rights";
import {Form} from "../models";
import {formService} from "../services";

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location', 'FormService',
	($scope, route, $location) => {
		$scope.lang = idiom;
		$scope.template = template;

		// Init variables

		$scope.page = "";
		$scope.formsListPages = ["mine", "shared", "sent", "archived"];
		$scope.edit = {
			mode: false,
			form: new Form()
		};

		// Routing & template opening

		template.open('main', `containers/main`);
		route({
			formsList: (params) => {
				if ($scope.formsListPages.includes(params.pageName)) {
					$scope.page = params.pageName;
					template.open('tab', `containers/forms-list`);
				}
				else {
					$scope.redirectTo('/e404');
				}
			},
			form: async (params) => {
				$scope.page = 'form';
				let idForm = params.idForm;
				$scope.edit.mode = true;
				let { data } = await formService.get(idForm);
				$scope.edit.form = data;
				template.open('tab', `containers/form`);
			},
			e404: () => {
				$scope.page = 'e404';
				template.open('tab', `containers/e404`);
			}
		});

		// Utils

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
