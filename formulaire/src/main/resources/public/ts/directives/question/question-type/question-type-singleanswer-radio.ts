import {Directive, ng} from "entcore";
import {FormElement, FormElements, Question, Section} from "@common/models";
import {I18nUtils} from "@common/utils";
import {Direction} from "@common/core/enums";

interface IViewModel {
    question: Question;
    hasFormResponses: boolean;
    formElements: FormElements;
    I18n: I18nUtils;
    Direction: typeof Direction;

    deleteChoice(index: number): Promise<void>;
    isSectionsAfter(formElement: FormElement): boolean;
}

export const questionTypeSingleanswerRadio: Directive = ng.directive('questionTypeSingleanswerRadio', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            hasFormResponses: '=',
            formElements: '<'
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
                    <label class="left-spacing-twice" ng-class="vm.question.conditional ? 'five four-mobile' : 'twelve'">
                        <input type="radio" disabled>
                        <span style="cursor: default"></span>
                        <input type="text" ng-model="choice.value" ng-if="!vm.question.selected && !choice.is_custom" disabled
                                ng-class="vm.question.conditional ? 'eleven seven-mobile' : 'width95 ten-mobile'"
                                placeholder="[[vm.I18n.getWithParam('formulaire.choice', choice.position)]]">
                        <input type="text" ng-model="choice.value" ng-if="vm.question.selected && !choice.is_custom" input-guard
                                ng-class="vm.question.conditional ? 'eleven seven-mobile' : 'width95 ten-mobile'"
                                placeholder="[[vm.I18n.getWithParam('formulaire.choice', choice.position)]]">
                        <input type="text" value="[[vm.I18n.translate('formulaire.other')]] : " ng-if="choice.is_custom" disabled
                                ng-class="vm.question.conditional ? 'eleven seven-mobile' : 'width95 ten-mobile'">
                    </label>
                    <i class="i-cancel lg-icon dontSave" ng-click="vm.deleteChoice($index)" ng-if="vm.question.selected && !vm.hasFormResponses"></i>
                    <select class="five" ng-if="vm.question.conditional" ng-model="choice.next_section_id" ng-disabled="!vm.question.selected" input-guard>
                        <option ng-repeat="section in vm.formElements.all | filter:vm.isSectionsAfter" ng-value="section.id">
                            [[vm.I18n.translate('formulaire.access.section') + section.title]]
                        </option>
                        <option ng-value="null" ng-selected="true">[[vm.I18n.translate('formulaire.access.recap')]]</option>
                    </select>
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

            vm.isSectionsAfter = (formElement: FormElement) : boolean => {
                if (formElement instanceof Question) {
                    return false;
                }
                else if (formElement instanceof Section) {
                    let position = vm.question.position ? vm.question.position : vm.formElements.all.filter(e => e.id === vm.question.section_id)[0].position;
                    return formElement.position > position;
                }
            }
        }
    };
});
