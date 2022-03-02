import {Directive, ng} from "entcore";
import {FormElement} from "../models";

interface IViewModel {
    formElement: FormElement,
    nbFormElements: number
}

export const progressBubbleBar: Directive = ng.directive('progressBubbleBar', () => {
    return {
        restrict: 'E',
        scope: {
            formElement: '=',
            nbFormElements: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="progressbar-container" ng-if="vm.nbFormElements > 1">
                <ul class="progressbar six twelve-mobile">
                    <li ng-repeat="n in [].constructor(vm.nbFormElements) track by $index"
                        ng-class="{ active: $index+1 <= vm.formElement.position }"></li>
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
