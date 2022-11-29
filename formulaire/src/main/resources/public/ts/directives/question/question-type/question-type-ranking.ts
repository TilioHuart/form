import {Directive, ng} from "entcore";
import {FormElements, Question, QuestionChoice} from "@common/models";
import {FormElementUtils, I18nUtils} from "@common/utils";
import {PropPosition} from "@common/core/enums/prop-position";
import {questionChoiceService} from "@common/services";
import {Direction} from "@common/core/enums";

interface IViewModel {
    I18n: I18nUtils;
    question: Question;
    formElements: FormElements;
    Direction: typeof Direction;

    createNewChoice(): void;
    moveChoice(choice: QuestionChoice, direction: string): void;
    deleteChoice(index: number): Promise<void>;
}

export const questionTypeRanking: Directive = ng.directive('questionTypeRanking', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            formElements: '<'
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
              <div class="twelve">
                <div class="choice" ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']" guard-root="formTitle">
                    <!-- Arrow to order -->
                    <div class="container-arrow" ng-if="vm.question.selected">
                        <div ng-class="{hidden : $first}" ng-click="vm.moveChoice(choice, vm.Direction.UP)">
                            <i class="i-chevron-up lg-icon"></i>
                        </div>
                        <div ng-class="{hidden : $last}" ng-click="vm.moveChoice(choice, vm.Direction.DOWN)">
                            <i class="i-chevron-down lg-icon"></i>
                        </div>
                    </div>
                    <!-- Choices -->
                    <label class="twelve left-spacing-twice">
                        <span style="cursor: default"></span>
                        <input type="text" class="width95 ten-mobile" ng-model="choice.value" ng-if="!vm.question.selected" disabled
                                placeholder="[[vm.I18n.getWithParam('formulaire.choice', choice.position)]]">
                        <input type="text" class="width95 ten-mobile" ng-model="choice.value" ng-if="vm.question.selected" input-guard
                                placeholder="[[vm.I18n.getWithParam('formulaire.choice', choice.position)]]">
                    </label>
                    <!-- Cross to delete choice -->
                    <i class="i-cancel lg-icon dontSave" ng-click="vm.deleteChoice($index)" ng-if="vm.question.selected && !vm.hasFormResponses"></i>
                </div>
                <!-- Plus to add new choice -->
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
                vm.question.choices.all.sort((a: QuestionChoice, b: QuestionChoice) => a.position - b.position);
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
        }
    };
});
