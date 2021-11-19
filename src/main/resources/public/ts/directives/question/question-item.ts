import {Directive, idiom, ng} from "entcore";
import {Question, Types} from "../../models";
import {FORMULAIRE_QUESTION_EMIT_EVENT} from "../../core/enums";

interface IViewModel {
    question: Question,
    reorder: boolean,
    hasFormResponses: boolean,
    types: typeof Types,

    getTitle(title: string): string
}

export const questionItem: Directive = ng.directive('questionItem', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            reorder: '=',
            hasFormResponses: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="question-item">                
                <div class="domino" ng-class="{'questionError': !vm.question.title || vm.question.choices.length === 0 }">
                    <div class="question-top" ng-class="{disabled: vm.hasFormResponses}">
                        <div class="dots" ng-if="vm.reorder && !vm.hasFormResponses">
                            <i class="drag xl-icon"></i>
                            <i class="drag xl-icon"></i>
                        </div>
                    </div>
                    <div class="focusable" id="[[vm.question.position]]">
                        <!-- Title component -->
                        <question-title question="vm.question"></question-title>
                        <!-- Main component -->
                        <question-type question="vm.question" has-form-responses="vm.hasFormResponses"></question-type>
                        <!-- Interraction buttons-->
                        <div class="question-bottom" ng-if="vm.question.selected">
                            <div class="mandatory" ng-if="vm.question.question_type != vm.types.FREETEXT">
                                <switch ng-model="vm.question.mandatory"></switch><i18n>formulaire.mandatory</i18n>
                            </div>
                            <i class="duplicate lg-icon spaced-right" ng-click="duplicateQuestion()" title="[[vm.getTitle('duplicate')]]"></i>
                            <i class="delete lg-icon spaced-right" ng-class="{disabled: vm.hasFormResponses}"
                            ng-click="deleteQuestion()" title="[[vm.getTitle('delete')]]"></i>
                            <i class="undo lg-icon spaced-right" ng-click="undoQuestionChanges()" title="[[vm.getTitle('cancel')]]"></i>
                        </div>
                    </div>
                </div>
                
                <div class="warning" ng-if="!vm.question.title"><i18n>formulaire.question.missing.field.title</i18n></div>
                <div class="warning" ng-if="vm.question.choices.length <= 0"><i18n>formulaire.question.missing.field.choice</i18n></div>
            </div>
        `,

        controller: async ($scope) => {
            const vm: IViewModel = <IViewModel> this;
        },
        link: ($scope, $element) => {
            const vm: IViewModel = $scope.vm;
            vm.types = Types;

            vm.getTitle = (title: string) : string => {
                return idiom.translate('formulaire.' + title);
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
