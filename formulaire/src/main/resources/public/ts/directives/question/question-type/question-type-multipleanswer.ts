import {Directive, ng} from "entcore";
import {Question, QuestionChoice} from "@common/models";
import {questionChoiceService} from "@common/services";
import {FormElementUtils} from "@common/utils";
import {Direction} from "@common/core/enums";
import {PropPosition} from "@common/core/enums/prop-position";

interface IViewModel {
    question: Question,
    hasFormResponses: boolean,
    Direction: typeof Direction;

    createNewChoice(): void;
    moveChoice(choice: QuestionChoice, direction: string): void;
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
                        <input type="checkbox" id="check-[[choice.id]]" disabled>
                        <span style="cursor: default"></span>
                        <input type="text" ng-model="choice.value" ng-if="!vm.question.selected" disabled
                                class="eleven ten-mobile" placeholder="Choix [[$index + 1]]">
                        <input type="text" ng-model="choice.value" ng-if="vm.question.selected" input-guard
                                class="eleven ten-mobile" placeholder="Choix [[$index + 1]]">
                    </label>
                    <i class="i-cancel lg-icon dontSave" ng-click="vm.deleteChoice($index)" ng-if="vm.question.selected && !vm.hasFormResponses"></i>
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
        }
    };
});
