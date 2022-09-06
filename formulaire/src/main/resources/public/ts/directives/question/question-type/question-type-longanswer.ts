import {Directive, ng} from "entcore";
import {Question} from "@common/models";

interface IViewModel {
    question: Question
}

export const questionTypeLonganswer: Directive = ng.directive('questionTypeLonganswer', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <textarea disabled i18n-placeholder="formulaire.question.type.LONGANSWER"></textarea>
        `,

        controller: async ($scope) => {
            const vm: IViewModel = <IViewModel> this;
        },
        link: ($scope, $element) => {
            const vm: IViewModel = $scope.vm;
        }
    };
});
