import {Directive, ng} from "entcore";
import {FormElement, FormElements, Question, QuestionChoice, Section} from "@common/models";
import {questionChoiceService} from "@common/services";
import {FormElementUtils, I18nUtils} from "@common/utils";
import {Direction} from "@common/core/enums/formulaire-directions";
import {PropPosition} from "@common/core/enums/prop-position";

interface IViewModel {
    question: Question;
    hasFormResponses: boolean;
    formElements: FormElements;
    I18n: I18nUtils;
    Direction: typeof Direction;

    createNewChoice(): void;
    moveChoice(choice: QuestionChoice, direction: string): void;
    deleteChoice(index: number): Promise<void>;
    isSectionsAfter(formElement: FormElement): boolean;
}

export const questionTypeSingleanswer: Directive = ng.directive('questionTypeSingleanswer', () => {

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
                <div class="choice" ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']" guard-root="formTitle">
                    <div class="container-arrow" ng-if="vm.question.selected">
                        <div ng-class="{hidden : $first}" ng-click="vm.moveChoice(choice, vm.Direction.UP)">
                            <i class="i-chevron-up lg-icon"></i>
                        </div>
                        <div ng-class="{hidden : $last}" ng-click="vm.moveChoice(choice, vm.Direction.DOWN)">
                            <i class="i-chevron-down lg-icon"></i>
                        </div>
                    </div>
                    <label class="nine left-spacing-twice">
                        <span class="content-line">[[$index + 1]].</span>
                        <input type="text" ng-model="choice.value" ng-if="!vm.question.selected" disabled
                                ng-class="vm.question.conditional ? 'five four-mobile' : 'nine'" placeholder="Choix [[$index + 1]]">
                        <input type="text" ng-model="choice.value" ng-if="vm.question.selected" input-guard
                                ng-class="vm.question.conditional ? 'five four-mobile' : 'nine'" placeholder="Choix [[$index + 1]]">
                    </label>
                    <i class="i-cancel lg-icon dontSave" ng-click="vm.deleteChoice($index)" ng-if="vm.question.selected && !vm.hasFormResponses"></i>
                    <select ng-if="vm.question.conditional" ng-model="choice.next_section_id" ng-disabled="!vm.question.selected" input-guard>
                        <option ng-repeat="section in vm.formElements.all | filter:vm.isSectionsAfter" ng-value="section.id">
                            [[vm.I18n.translate('formulaire.access.section') + section.title]]
                        </option>
                        <option ng-value="null" ng-selected="true">[[vm.I18n.translate('formulaire.access.recap')]]</option>
                    </select>
                </div>
                <div style="display: flex; justify-content: center;" ng-if="vm.question.selected && !vm.hasFormResponses">
                    <i class="i-plus-circle lg-icon" ng-click="vm.createNewChoice()"></i>
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

            vm.createNewChoice = () : void => {
                vm.question.choices.all.push(new QuestionChoice(vm.question.id, vm.question.choices.all.length + 1));
                $scope.$apply();
            };

            vm.moveChoice = (choice: QuestionChoice, direction: string) : void => {
                FormElementUtils.switchPositions(vm.question.choices, choice.position - 1, direction, PropPosition.POSITION);
                vm.question.choices.all.sort((a, b) => a.position - b.position);
                $scope.$apply();
            };

            vm.deleteChoice = async (index: number) : Promise<void> => {
                if (vm.question.choices.all[index].id) {
                    await questionChoiceService.delete(vm.question.choices.all[index].id);
                }
                for (let i = index + 1; i < vm.question.choices.all.length; i++) {
                    vm.question.choices.all[i].position--;
                }
                vm.question.choices.all.splice(index,1);
                $scope.$apply();
            };

            vm.isSectionsAfter = (formElement: FormElement) : boolean => {
                if (formElement instanceof Question) {
                    return false;
                }
                else if (formElement instanceof Section) {
                    let position = vm.question.position ? vm.question.position : vm.formElements.all.filter(e => e.id === vm.question.section_id)[0].position;
                    return formElement.position > position;
                }
            };
        }
    };
});
