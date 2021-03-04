import {Directive, ng} from "entcore";
import {Question} from "../../models";

interface IViewModel {
    question: Question
}

export const questionTypeSingleanswer: Directive = ng.directive('questionTypeSingleanswer', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        replace: true,
        template: `
            <div class="seven">
                <div ng-repeat="choice in vm.question.choices.all">
                    <span>[[$index + 1]].</span>
                    <input type="text" class="eleven" ng-model="choice.value" i18n-placeholder="Choix [[$index + 1]]" ng-if="!vm.question.selected" disabled>
                    <input type="text" class="eleven" ng-model="choice.value" i18n-placeholder="Choix [[$index + 1]]" ng-if="vm.question.selected">
                    <img src="/formulaire/public/img/icons/cancel.svg" ng-click="vm.deleteChoice(vm.question, $index)" ng-if="vm.question.selected"/>
                </div>
                <div style="display: flex; justify-content: center;" ng-if="vm.question.selected">
                    <img src="/formulaire/public/img/icons/plus-circle.svg" ng-click="vm.createNewChoice(vm.question)"/>
                </div>
            </div>
        `,

        controller: async ($scope) => {
            const vm: IViewModel = <IViewModel> this;
        },
        link: ($scope, $element) => {
            const vm: IViewModel = $scope.vm;
        }
    };
});
