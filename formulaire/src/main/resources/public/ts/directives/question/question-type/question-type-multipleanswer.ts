import {Directive, ng} from "entcore";
import {Question} from "@common/models";
import {I18nUtils} from "@common/utils";
import {Direction} from "@common/core/enums";

interface IViewModel {
    question: Question,
    hasFormResponses: boolean,
    I18n: I18nUtils;
    Direction: typeof Direction;

    deleteChoice(index: number): Promise<void>;
}

export const questionTypeMultipleanswer: Directive = ng.directive('questionTypeMultipleanswer', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            hasFormResponses: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="twelve">
                <div class="choice" ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']">
                    <div class="container-arrow" ng-if="vm.question.selected">
                        <div ng-class="{hidden : $first || choice.is_custom}" ng-click="vm.question.moveChoice(choice, vm.Direction.UP)">
                            <i class="i-chevron-up lg-icon"></i>
                        </div>
                        <div ng-class="{hidden : $last || vm.question.choices.all[$index+1].is_custom}"
                             ng-click="vm.question.moveChoice(choice, vm.Direction.DOWN)">
                            <i class="i-chevron-down lg-icon"></i>
                        </div>
                    </div>
                    <label class="twelve left-spacing-twice">
                        <input type="checkbox" id="check-[[choice.id]]" disabled>
                        <span style="cursor: default"></span>
                        <input type="text" class="width95 ten-mobile" ng-model="choice.value" ng-if="!vm.question.selected && !choice.is_custom"
                                placeholder="[[vm.I18n.getWithParam('formulaire.choice', choice.position)]]" disabled>
                        <input type="text" class="width95 ten-mobile" ng-model="choice.value" ng-if="vm.question.selected && !choice.is_custom"
                                placeholder="[[vm.I18n.getWithParam('formulaire.choice', choice.position)]]" input-guard>
                        <input type="text" class="width95 ten-mobile" ng-if="choice.is_custom"
                                value="[[vm.I18n.translate('formulaire.other')]] : " disabled>
                    </label>
                    <i class="i-cancel lg-icon dontSave" ng-click="vm.deleteChoice($index)" ng-if="vm.question.selected && !vm.hasFormResponses"></i>
                </div>
                <div class="add-choice" ng-if="vm.question.selected && !vm.hasFormResponses">
                    <i class="i-plus-circle lg-icon" ng-click="vm.question.createNewChoice()"></i>
                    <div ng-if="!vm.question.choices.all[vm.question.choices.all.length - 1].is_custom">
                        <i18n>formulaire.question.add.choice.other.text</i18n>
                        <a class="dontSave" ng-click="vm.question.createNewChoice(true)"><i18n>formulaire.question.add.choice.other.link</i18n></a>
                    </div>
                </div>
            </div>
        `,

        controller: async ($scope) => {
            const vm: IViewModel = <IViewModel> this;
        },
        link: ($scope, $element) => {
            const vm: IViewModel = $scope.vm;
            vm.I18n = I18nUtils;
            vm.Direction = Direction;

            vm.deleteChoice = async (index: number) : Promise<void> => {
                await vm.question.deleteChoice(index);
                $scope.$apply();
            }
        }
    };
});
