import {Directive, idiom, ng} from "entcore";
import {Question, QuestionTypes} from "../models";
import {FORMULAIRE_EMIT_EVENT, FORMULAIRE_QUESTION_EMIT_EVENT} from "../core/enums/formulaire-event";

interface IViewModel {
    question: Question,
    questionTypes: QuestionTypes,

    displayTypeName(typeName: string): string
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
                <div class="focusable" id="[[vm.question.id]]">
                    <div class="question-title">
                        <!-- Title component -->
                        <question-title question="vm.question"></question-title>
                        <div ng-if="vm.question.selected" ng-show="false">
                            <select ng-model="vm.question.question_type" style="height: 24px;">
                                <option ng-repeat="type in questionTypes.all" ng-value="type.code"
                                        ng-selected="type.code === vm.question.question_type">
                                    [[vm.displayTypeName(type.name)]]
                                </option>
                            </select>
                        </div>
                    </div>
                    <div class="question-main">
                        <!-- Main component -->
                        <question-type question="vm.question"></question-type>
                    </div>
                    <div class="question-bottom" ng-if="vm.question.selected">
                        <div class="mandatory" ng-if="vm.question.question_type != 1">
                            <switch ng-model="vm.question.mandatory"></switch><i18n>formulaire.mandatory</i18n>
                        </div>
                        <img src="formulaire/public/img/icons/duplicate.svg" ng-click="duplicateQuestion()"/>
                        <img src="formulaire/public/img/icons/delete.svg" ng-click="deleteQuestion()"/>
                        <img src="formulaire/public/img/icons/undo.svg" ng-click="undoQuestionChanges()"/>
                    </div>
                </div>
            </div>
        `,

        controller: async ($scope) => {
            const vm: IViewModel = <IViewModel> this;
        },
        link: ($scope, $element) => {
            const vm: IViewModel = $scope.vm;

            vm.displayTypeName = (typeInfo: string) : string => {
                return idiom.translate('formulaire.question.type.' + typeInfo);
            };

            $scope.duplicateQuestion = () : void => {
                $scope.$emit(FORMULAIRE_QUESTION_EMIT_EVENT.DUPLICATE);
            }

            $scope.deleteQuestion = () : void => {
                $scope.$emit(FORMULAIRE_QUESTION_EMIT_EVENT.DELETE);
            }

            $scope.undoQuestionChanges = () : void => {
                $scope.$emit(FORMULAIRE_QUESTION_EMIT_EVENT.UNDO);
            }
        }
    };
});
