import {idiom, model, ng, notify, template, toasts} from 'entcore';
import rights from "../rights";
import {Form} from "../models";
import {formService} from "../services";

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location', 'FormService',
	($scope, route, $location) => {
		$scope.lang = idiom;
		$scope.template = template;

		// Init variables
		$scope.page = '';
		$scope.edit = {
			mode: false,
			form: new Form()
		};

		// Routing & template opening

		route({
			formsList: () => {
				$scope.page = 'list';
				template.open('main', `containers/main`);
				template.open('tab', `containers/forms-list`);
			},
			createForm: () => {
				$scope.page = 'create';
				template.open('main', `containers/main`);
				template.open('tab', `containers/create-form`);
			},
			editForm: async (params) => {
				$scope.page = 'edit';
				let { data } = await formService.get(params.idForm);
				$scope.edit.form = data;
				$scope.edit.mode = true;
				template.open('main', `containers/main`);
				template.open('tab', `containers/edit-form`);
			},
			e404: () => {
				$scope.page = 'e404';
				template.open('main', `containers/e404`);
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
