import {Directive, ng} from "entcore";
import {Question} from "@common/models";

interface IViewModel {
    question: Question
}

export const questionTypeTime: Directive = ng.directive('questionTypeTime', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <i18n style="color:grey">formulaire.question.time.empty</i18n>
        `,

        controller: async ($scope) => {
            const vm: IViewModel = <IViewModel> this;
        },
        link: ($scope, $element) => {
            const vm: IViewModel = $scope.vm;
        }
    };
});
