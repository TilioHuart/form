import {idiom, model, ng, template} from 'entcore';
import {Forms} from "../models/Form";
import rights from "../rights";

export const mainController = ng.controller('MainController', ['$scope', 'route', ($scope, route) => {
	route({
		main: () => {
			$scope.forms = new Forms();
			await $scope.forms.sync();
			template.open('main', `containers/main`);
		}
	});

	$scope.lang = idiom;
	$scope.template = template;


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
