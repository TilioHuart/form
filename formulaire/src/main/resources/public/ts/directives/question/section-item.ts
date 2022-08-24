import {Directive, idiom, ng} from "entcore";
import {FormElements, Section, Types} from "../../models";
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";

interface IViewModel {
    section: Section;
    reorder: boolean;
    hasFormResponses: boolean;
    types: typeof Types;
    formElements: FormElements;

    getTitle(title: string): string;
    editSection(): void;
    deleteSection(): void;
    isOtherElementSelected(): boolean;
    undoSectionChanges(): void;
    validateSection(): void;
    deleteQuestion(): void;
    addQuestionToSection(): Promise<void>;
    addQuestionToSectionGuard(): void;
    hasMultipleConditionals() : boolean;
}

export const sectionItem: Directive = ng.directive('sectionItem', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            section: '=',
            reorder: '=',
            hasFormResponses: '=',
            formElements: '<'
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="ten section-item">
                <div class="domino" ng-class="{'sectionError': !vm.section.title || vm.verifConditional()}">
                    <div class="section-top" ng-class="{'disabled & dontSave': vm.hasFormResponses || vm.section.selected}">
                        <!-- Drag and drop icon -->
                        <div class="section-top-dots grab">
                            <div class="dots" ng-if="vm.reorder || !vm.hasFormResponses">
                                <i class="i-drag xl-icon"></i>
                                <i class="i-drag xl-icon"></i>
                            </div>
                        </div>
                        <div class="section-top-container">
                            <!-- Title component -->
                            <div class="title twelve" guard-root="formTitle">
                                <div class="flex-spaced" ng-if="!vm.section.selected">
                                    <h4 ng-if="vm.section.title" class="ellipsis">
                                        [[vm.section.title]]
                                    </h4>
                                    <h4 ng-if="!vm.section.title" class="empty">
                                        <i18n>formulaire.section.title.empty</i18n>
                                    </h4>
                                    <i class="i-edit md-icon dontSave" ng-click="vm.editSection()" title="[[vm.getTitle('edit')]]"
                                        ng-if="!vm.isOtherElementSelected()"></i>
                                </div>
                                <div class="top-spacing-twice" ng-if="vm.section.selected">
                                    <input type="text" class="twelve" i18n-placeholder="formulaire.section.title.empty"
                                           ng-model="vm.section.title" ng-keydown="$event.keyCode === 13 && vm.validateSection()" input-guard>
                                </div>
                            </div>
                            <!-- Interaction buttons-->
                            <div class="icons-section dontSave" ng-if="vm.section.selected">
                                <i class="i-delete lg-icon spaced-right" ng-class="{disabled: vm.hasFormResponses}" 
                                reset-guard="vm.deleteSection()" title="[[vm.getTitle('delete')]]"></i>
                                <i class="i-undo lg-icon spaced-right" reset-guard="vm.undoSectionChanges()" title="[[vm.getTitle('cancel')]]"></i>
                                <i class="i-validate lg-icon spaced-right" reset-guard="vm.validateSection()" title="[[vm.getTitle('validate')]]"></i>
                            </div>
                        </div>
                    </div>
                    <div class="nofocusable" id="[[vm.section.position]]">
                        <!-- Description -->
                        <div class="description row">
                            <div ng-if="!vm.section.selected">
                                <div ng-if="vm.section.description" ng-bind-html="vm.section.description"></div>
                                <div ng-if="!vm.section.description" class="nodescription"><i18n>formulaire.section.no.description</i18n></div>
                            </div>
                            <div class="dontSave" ng-if="vm.section.selected">
                                <editor ng-model="vm.section.description" input-guard></editor>
                            </div>
                        </div>
                        <!-- Questions children -->
                        <div id="container-[[vm.section.id]]" class="questions row nested-container" ng-if="!vm.hasFormResponses">
                             <div class="childSection" ng-repeat="question in vm.section.questions.all">
                                <question-item question="question"
                                               reorder="vm.reorder"
                                               has-form-responses="vm.hasFormResponses"
                                               form-elements="vm.formElements">
                                </question-item>
                            </div>
                        </div>
                        <div class="questions row" ng-if="vm.hasFormResponses">
                             <div class="childSection" ng-repeat="question in vm.section.questions.all">
                                <question-item question="question"
                                               reorder="vm.reorder"
                                               has-form-responses="vm.hasFormResponses"
                                               form-elements="vm.formElements">
                                </question-item>
                            </div>
                        </div>
                        <!-- Add question button -->
                        <div class="addQuestion row" ng-if="!vm.section.selected && !vm.hasFormResponses">
                            <a reset-guard="vm.addQuestionToSectionGuard()" reset-guard-id="formTitle">
                                <i18n>formulaire.section.new.question</i18n>
                            </a>
                        </div>
                    </div>
                </div>
                
                <div class="warning" ng-if="!vm.section.title"><i18n>formulaire.section.missing.field.title</i18n></div>
                <div class="warning" ng-if="vm.verifConditional()"><i18n>formulaire.element.block.multiple.conditional</i18n></div>
            </div>
        `,

        controller: function ($scope) {
            const vm: IViewModel = <IViewModel> this;
        },
        link: function ($scope, $element) {
            const vm: IViewModel = $scope.vm;
            vm.types = Types;

            vm.getTitle = (title: string) : string => {
                return idiom.translate('formulaire.' + title);
            };

            vm.editSection = () : void => {
                vm.section.selected = true;
            };

            vm.deleteSection = () : void => {
                if (!vm.hasFormResponses) {
                    $scope.$emit(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.DELETE_ELEMENT);
                }
            };

            vm.isOtherElementSelected = () : boolean => {
                let hasSelectedChild: boolean = vm.section.questions.all.filter(q => q.selected).length > 0;
                let hasSiblingChild: boolean = vm.formElements.hasSelectedElement() && vm.formElements.getSelectedElement().id != vm.section.id;
                return hasSelectedChild || hasSiblingChild;
            };

            vm.undoSectionChanges = () : void => {
                $scope.$emit(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.UNDO_CHANGES);
            };

            vm.validateSection = async () : Promise<void> => {
                $scope.$emit(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.VALIDATE_SECTION);
            };

            vm.addQuestionToSection = async () : Promise<void> => {
                if (!vm.hasFormResponses) {
                    $scope.$emit(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.CREATE_QUESTION);
                }
            };

            vm.addQuestionToSectionGuard = () =>{
                vm.addQuestionToSection().then();
            }

            vm.hasMultipleConditionals = (): boolean => {
                return vm.section.questions.all.filter(q =>q.conditional).length > 1;
            }
        }
    };
});
