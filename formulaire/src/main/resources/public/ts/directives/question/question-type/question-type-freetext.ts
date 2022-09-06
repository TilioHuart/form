import {Directive, ng} from "entcore";
import {Question} from "@common/models";

interface IViewModel {
    question: Question
}

export const questionTypeFreetext: Directive = ng.directive('questionTypeFreetext', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div>
                <div ng-if="!vm.question.selected">
                    <div ng-if="vm.question.statement" ng-bind-html="vm.question.statement"></div>
                    <textarea disabled ng-if="!vm.question.statement" i18n-placeholder="formulaire.question.type.FREETEXT"></textarea>
                </div>
                <div ng-if="vm.question.selected">
                    <editor ng-model="vm.question.statement" input-guard></editor>
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
