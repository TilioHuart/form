import {appPrefix, ng} from 'entcore';

export let formsItem = ng.directive("formsItem", function () {

    return {
        restrict: 'E',
        scope: {
            form: '=',
            first: '='
        },
        templateUrl: `/${appPrefix}/public/template/directives/formsItem.html`,
        controller: ['$scope', function ($scope) {

        }]
    };
});
