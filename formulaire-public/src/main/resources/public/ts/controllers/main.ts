import {idiom, ng, template} from 'entcore';
import {FormElementUtils} from "@common/utils";
import {Types} from "@common/models";

export const mainController = ng.controller('MainController', ['$scope', 'route',
	($scope, route) => {
		$scope.idiom = idiom;
		$scope.template = template;

		// Init variables
		$scope.Types = Types;
		$scope.FormElementUtils = FormElementUtils;
		$scope.formKey = "";

		const init = async () : Promise<void> => { };

		// Routing & template opening
		route({
			respondPublicForm: (params) => {
				$scope.formKey = params.formKey;
				if (params.formKey) {
					template.open('main', 'containers/home-page');
				}
				else {
					template.open('main', 'containers/end/e404');
				}
			},
			e404: () => {
				template.open('main', 'containers/end/e404');
			}
		});

		// Utils

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

		init();
}]);
