import {Directive, ng} from "entcore";
import {FormElement} from "../models";

interface IViewModel {
    formElement: FormElement,
    historicPosition: number,
    longestPath: number;
}

export const progressBubbleBar: Directive = ng.directive('progressBubbleBar', () => {
    return {
        restrict: 'E',
        scope: {
            formElement: '=',
            longestPath: '=',
            historicPosition: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="progressbar-container" ng-if="vm.longestPath > 0">
                <ul class="progressbar six twelve-mobile">
                    <li ng-repeat="n in [].constructor(vm.longestPath) track by $index"
                    ng-class="{ active: $index+1 <= vm.historicPosition.length }"></li>
                </ul>
            </div>
        `,

        controller: function($scope) {
            const vm: IViewModel = <IViewModel> this;
        },
        link: function ($scope, $element) {
            const vm: IViewModel = $scope.vm;
        }
    };
});
