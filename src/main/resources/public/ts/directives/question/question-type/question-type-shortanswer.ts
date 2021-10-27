import {Directive, ng} from "entcore";
import {Question} from "../../../models";

interface IViewModel {
    question: Question
}

export const questionTypeShortanswer: Directive = ng.directive('questionTypeShortanswer', () => {

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
            <textarea disabled i18n-placeholder="formulaire.question.type.SHORTANSWER"></textarea>
        `,

        controller: async ($scope) => {
            const vm: IViewModel = <IViewModel> this;
        },
        link: ($scope, $element) => {
            const vm: IViewModel = $scope.vm;
        }
    };
});
