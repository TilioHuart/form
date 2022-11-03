import {Directive, ng} from "entcore";
import {FormElements, Question, QuestionChoice} from "@common/models";
import {questionChoiceService, questionService} from "@common/services";
import {FormElementUtils, I18nUtils} from "@common/utils";
import {Direction} from "@common/core/enums";
import {PropPosition} from "@common/core/enums/prop-position";

interface IViewModel {
    question: Question;
    hasFormResponses: boolean;
    formElements: FormElements;
    matrixType: number;
    I18n: I18nUtils;
    Direction: typeof Direction;

    createNewChoice(): void;
    moveChoice(choice: QuestionChoice, direction: string): void;
    deleteChoice(index: number): Promise<void>;
    createNewChild(): void;
    moveChild(child: Question, direction: string): void;
    deleteChild(index: number): Promise<void>;
}

export const questionTypeMatrix: Directive = ng.directive('questionTypeMatrix', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            hasFormResponses: '=',
            formElements: '<',
            matrixType: '<'
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="twelve matrix">
                <!-- Define the columns' titles (= choices) -->
                <div class="matrix-columns">
                    <h4><i18n>formulaire.matrix.columns</i18n></h4>
                    <div class="choice" ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']" guard-root="formTitle">
                        <div class="container-arrow" ng-if="vm.question.selected">
                            <div ng-class="{hidden : $first}" ng-click="vm.moveChoice(choice, vm.Direction.UP)">
                                <i class="i-chevron-up lg-icon"></i>
                            </div>
                            <div ng-class="{hidden : $last}" ng-click="vm.moveChoice(choice, vm.Direction.DOWN)">
                                <i class="i-chevron-down lg-icon"></i>
                            </div>
                        </div>
                        <label class="twelve left-spacing-twice">
                            <span style="cursor: default"></span>
                            <input type="text" class="width95-always" ng-model="choice.value" ng-if="!vm.question.selected" disabled
                                   placeholder="[[vm.I18n.getWithParam('formulaire.option', choice.position)]]">
                            <input type="text" class="width95-always" ng-model="choice.value" ng-if="vm.question.selected" input-guard
                                   placeholder="[[vm.I18n.getWithParam('formulaire.option', choice.position)]]">
                        </label>
                        <i class="i-cancel lg-icon dontSave" ng-click="vm.deleteChoice($index)" ng-if="vm.question.selected && !vm.hasFormResponses"></i>
                    </div>
                    <div style="display: flex; justify-content: center;" ng-if="vm.question.selected && !vm.hasFormResponses">
                        <i class="i-plus-circle lg-icon" ng-click="vm.createNewChoice()"></i>
                    </div>
                </div>
                <!-- Define the lines' titles (= children questions) -->
                <div class="matrix-lines">
                    <h4><i18n>formulaire.matrix.lines</i18n></h4>
                    <div class="choice" ng-repeat="child in vm.question.children.all | orderBy:'matrix_position'" guard-root="formTitle">
                        <div class="container-arrow" ng-if="vm.question.selected">
                            <div ng-class="{hidden : $first}" ng-click="vm.moveChild(child, vm.Direction.UP)">
                                <i class="i-chevron-up lg-icon"></i>
                            </div>
                            <div ng-class="{hidden : $last}" ng-click="vm.moveChild(child, vm.Direction.DOWN)">
                                <i class="i-chevron-down lg-icon"></i>
                            </div>
                        </div>
                        <label class="twelve left-spacing-twice">
                            <span style="cursor: default"></span>
                            <input type="text" class="width95-always" ng-model="child.title" ng-if="!vm.question.selected" disabled
                                   placeholder="[[vm.I18n.getWithParam('formulaire.option', child.matrix_position)]]">
                            <input type="text" class="width95-always" ng-model="child.title" ng-if="vm.question.selected" input-guard
                                   placeholder="[[vm.I18n.getWithParam('formulaire.option', child.matrix_position)]]">
                        </label>
                        <i class="i-cancel lg-icon dontSave" ng-click="vm.deleteChild($index)" ng-if="vm.question.selected && !vm.hasFormResponses"></i>
                    </div>
                    <div style="display: flex; justify-content: center;" ng-if="vm.question.selected && !vm.hasFormResponses">
                        <i class="i-plus-circle lg-icon" ng-click="vm.createNewChild()"></i>
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

            vm.createNewChild = () : void => {
                vm.question.children.all.push(new Question(vm.question.id, vm.matrixType, vm.question.children.all.length + 1));
                $scope.$apply();
            };

            vm.moveChild = (child: Question, direction: string) : void => {
                FormElementUtils.switchPositions(vm.question.children, child.matrix_position - 1, direction, PropPosition.MATRIX_POSITION);
                vm.question.children.all.sort((a, b) => a.matrix_position - b.matrix_position);
                $scope.$apply();
            };

            vm.deleteChild = async (index: number) : Promise<void> => {
                if (vm.question.children.all[index].id) {
                    await questionService.delete(vm.question.children.all[index].id);
                }
                for (let i = index + 1; i < vm.question.children.all.length; i++) {
                    vm.question.children.all[i].matrix_position--;
                }
                vm.question.children.all.splice(index,1);
                $scope.$apply();
            };
        }
    };
});
