import {Directive, idiom, ng} from "entcore";
import {FormElements, Question, Section, Types} from "../../models";
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";

interface IViewModel {
    question: Question;
    reorder: boolean;
    hasFormResponses: boolean;
    Types: typeof Types;
    formElements: FormElements;

    getTitle(title: string): string;
    duplicateQuestion(): void;
    deleteQuestion(): Promise<void>;
    undoQuestionChanges(): void;
    showConditionalSwitch(): boolean;
    onSwitchMandatory(isMandatory: boolean): void;
    onSwitchConditional(isConditional: boolean): void;
}

export const questionItem: Directive = ng.directive('questionItem', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            reorder: '=',
            hasFormResponses: '=',
            formElements: '<'
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="question-item" ng-class="vm.question.section_id ? 'twelve' : 'nine'">
                <div class="domino" ng-class="{'questionError': !vm.question.title || vm.question.choices.length === 0, 'disabled': vm.hasFormResponses || vm.question.selected}">
                    <div class="question-top grab">
                        <div class="dots" ng-if="vm.reorder || !vm.hasFormResponses">
                            <i class="i-drag xl-icon"></i>
                            <i class="i-drag xl-icon"></i>
                        </div>
                    </div>
                    <div class="question-main focusable">
                        <!-- Title component -->
                        <question-title question="vm.question"></question-title>
                        <!-- Main component -->
                        <question-type question="vm.question" has-form-responses="vm.hasFormResponses" form-elements="vm.formElements"></question-type>
                        <!-- Interaction buttons-->
                        <div class="question-bottom" ng-if="vm.question.selected">
                            <div class="mandatory" ng-if="vm.question.question_type != vm.Types.FREETEXT" title="[[vm.getTitle('mandatory.explanation')]]">
                                <switch ng-model="vm.question.mandatory" ng-class="{switchoff: vm.hasFormResponses}"
                                        ng-change="vm.onSwitchMandatory(vm.question.mandatory)"></switch>
                                <i18n>formulaire.mandatory</i18n>
                            </div>
                            <div class="conditional" ng-if="vm.showConditionalSwitch()">
                                <switch ng-model="vm.question.conditional" ng-class="{switchoff: vm.hasFormResponses}"
                                        ng-change="vm.onSwitchConditional(vm.question.conditional)"></switch>
                                <i18n>formulaire.conditional</i18n>
                            </div>
                            <i class="i-duplicate lg-icon spaced-right" ng-click="vm.duplicateQuestion()"
                                ng-class="{disabled: vm.hasFormResponses}"  title="[[vm.getTitle('duplicate')]]"></i>
                            <i class="i-delete lg-icon spaced-right" ng-class="{disabled: vm.hasFormResponses}" 
                                ng-click="vm.deleteQuestion()" title="[[vm.getTitle('delete')]]"></i>
                            <i class="i-undo lg-icon spaced-right" ng-click="vm.undoQuestionChanges()" title="[[vm.getTitle('cancel')]]"></i>
                        </div>
                    </div>
                </div>
                
                <div class="warning" ng-if="!vm.question.title"><i18n>formulaire.question.missing.field.title</i18n></div>
                <div class="warning" ng-if="vm.question.choices.length <= 0"><i18n>formulaire.question.missing.field.choice</i18n></div>
            </div>
        `,

        controller: function ($scope) {
            const vm: IViewModel = <IViewModel> this;
        },
        link: function ($scope, $element) {
            const vm: IViewModel = $scope.vm;
            vm.Types = Types;

            vm.getTitle = (title: string) : string => {
                return idiom.translate('formulaire.' + title);
            };

            vm.duplicateQuestion = () : void => {
                if (!vm.hasFormResponses) {
                    $scope.$emit(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.DUPLICATE_ELEMENT);
                }
            };

            vm.deleteQuestion =  async() : Promise<void> => {
                if (!vm.hasFormResponses) {
                    $scope.$emit(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.DELETE_ELEMENT);
                }
            };

            vm.undoQuestionChanges = () : void => {
                $scope.$emit(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.UNDO_CHANGES);
            };


            vm.showConditionalSwitch = () : boolean => {
                let isConditionalQuestionType = vm.question.question_type === Types.SINGLEANSWER || vm.question.question_type === Types.SINGLEANSWERRADIO;
                if (!isConditionalQuestionType) {
                    return false;
                }
                else if (!vm.question.section_id) {
                    return true;
                }
                else {
                    let parentSection = vm.formElements.all.filter(e => e.id === vm.question.section_id)[0];
                    return (parentSection as Section).questions.all.filter(q => q.id != vm.question.id && q.conditional).length <= 0;
                }
            };

            vm.onSwitchMandatory = (isMandatory: boolean) : void => {
                if (vm.hasFormResponses) {
                    vm.question.mandatory = !isMandatory;
                }
                if (!isMandatory && vm.question.conditional) {
                    vm.question.mandatory = true;
                }
                $scope.$apply();
            };

            vm.onSwitchConditional = (isConditional: boolean) : void => {
                if (vm.hasFormResponses) {
                    vm.question.conditional = !isConditional;
                }
                vm.question.mandatory = vm.question.conditional;
                $scope.$apply();
            };
        }
    };
});
