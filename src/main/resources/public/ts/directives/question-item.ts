import {Directive, ng} from "entcore";
import {Question, QuestionTypes} from "../models";

interface IViewModel {
    question: Question,
    questionTypes: QuestionTypes
}

export const questionItem: Directive = ng.directive('questionItem', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            questionTypes: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        replace: true,
        template: `
            <div class="domino">
                <div class="question-top">
                    <div class="dots">
                        <i class="drag lg-icon"></i>
                        <i class="drag lg-icon"></i>
                    </div>
                </div>
                <div class="focusable" id="[[question.id]]">
                    <div class="question-title">
                        <!-- Title component -->
                        <question-title question="vm.question"></question-title>
                        <div ng-if="question.selected" ng-show="false">
                            <select ng-model="question.question_type" style="height: 24px;">
                                <option ng-repeat="type in questionTypes.all" ng-value="type.code"
                                        ng-selected="type.code === question.question_type">
                                    [[vm.displayTypeName(type.name)]]
                                </option>
                            </select>
                        </div>
                    </div>
                    <div class="question-main">
                        <!-- Main component -->
                        <question-type question="vm.question"></question-type>
                    </div>
                    <div class="question-bottom" ng-if="question.selected">
                        <div class="mandatory" ng-if="question.question_type != 1">
                            <switch ng-model="question.mandatory"></switch><i18n>formulaire.mandatory</i18n>
                        </div>
                        <img src="formulaire/public/img/icons/duplicate.svg" ng-click="vm.duplicateQuestion()"/>
                        <img src="formulaire/public/img/icons/delete.svg" ng-click="vm.deleteQuestion()"/>
                        <img src="formulaire/public/img/icons/undo.svg" ng-click="vm.undoQuestionChanges()"/>
                    </div>
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
