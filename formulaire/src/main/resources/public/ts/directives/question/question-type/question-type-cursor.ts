import {Directive, ng} from "entcore";
import {Question} from "@common/models";

interface IViewModel {
    question: Question
}

export const questionTypeCursor: Directive = ng.directive('questionTypeCursor', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="formulaire-cursor">
                <div class="cursor-min-value">
                    <label><i18n>formulaire.question.value.minimum</i18n><em>*</em> : </label>
                    <input type="number" ng-model="vm.question.cursor_min_val" value={{1}}>
                </div>
                    <div class="cursor-label-min-val">
                    <label><i18n>formulaire.question.value.label</i18n></label>
                    <input type="text" i18n-placeholder="formulaire.question.label" 
                            ng-model="vm.question.cursor_label_min_val">
                </div>
                 <div class="cursor-max-value">
                    <label><i18n>formulaire.question.value.maximum</i18n><em>*</em> : </label>
                    <input type="number" ng-model="vm.question.cursor_max_val" value={{10}}>
                </div>   
                <div class="cursor-label-max-value">
                    <label><i18n>formulaire.question.value.label</i18n></label>
                    <input type="text" i18n-placeholder="formulaire.question.label" 
                        ng-model="vm.question.cursor_label_max_val">
                </div>  
                <div class="cursor-step">
                    <label><i18n>formulaire.question.step</i18n><em>*</em> : </label>
                    <input type="number" ng-model="vm.question.cursor_step"
                           value={{1}}>
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
