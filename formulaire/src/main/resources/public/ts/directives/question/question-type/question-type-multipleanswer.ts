import {Directive, ng} from "entcore";
import {Question, QuestionChoice} from "../../../models";
import {questionChoiceService} from "../../../services";

interface IViewModel {
    question: Question,
    hasFormResponses: boolean,

    createNewChoice(): void,
    deleteChoice(index: number): Promise<void>
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
                <div class="choice" ng-repeat="choice in vm.question.choices.all | orderBy:'id'" guard-root="formTitle">
                    <label for="check-[[choice.id]]" class="nine">
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

            vm.createNewChoice = () : void => {
                vm.question.choices.all.push(new QuestionChoice(vm.question.id));
                $scope.$apply();
            };

            vm.deleteChoice = async (index: number) : Promise<void> => {
                if (vm.question.choices.all[index].id) {
                    await questionChoiceService.delete(vm.question.choices.all[index].id);
                }
                vm.question.choices.all.splice(index,1);
                $scope.$apply();
            };
        }
    };
});
