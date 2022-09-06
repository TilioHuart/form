import {Directive, ng} from "entcore";
import {Question, Types} from "../../models";
import {I18nUtils} from "@common/utils";

interface IViewModel {
    question: Question;
    hasFormResponses: boolean;
    matrixType: number;
    Types: typeof Types;

    I18n: I18nUtils;
    matrixTypes: Types[];

    onChangeMatrixType(matrixType: number): void;
}

export const questionTitle: Directive = ng.directive('questionTitle', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            hasFormResponses: '=',
            matrixType: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="question-title focusable" ng-class="{onedition: vm.reorder || !vm.hasFormResponses}" guard-root="formTitle">                
                <div class="twelve">
                    <div ng-if="!vm.question.selected" ng-class="{'flex-spaced': vm.question.question_type == vm.Types.MATRIX}" >
                        <h4 ng-if="vm.question.title">[[vm.question.title]]<span ng-if="vm.question.mandatory" style="color:red;margin-left:10px">*</span></h4>
                        <h4 ng-if="!vm.question.title" class="empty">
                            <i18n ng-if="vm.question.question_type == vm.Types.FREETEXT">formulaire.question.title.free.empty</i18n>
                            <i18n ng-if="vm.question.question_type != vm.Types.FREETEXT">formulaire.question.title.empty</i18n>
                        </h4>
                        <div ng-if="vm.question.question_type == vm.Types.MATRIX">
                            <select class="two" ng-model="vm.matrixType" ng-disabled="true">
                                <option ng-repeat="matrixType in vm.matrixTypes" ng-value="matrixType"
                                        ng-attr-selected="[[matrixType === vm.matrixType ? 'selected' : undefined]]">
                                    [[vm.I18n.translate('formulaire.matrix.type.' + vm.Types[matrixType])]]
                                </option>
                            </select>
                        </div>
                    </div>
                    <div ng-if="vm.question.selected">
                        <input type="text" class="ten" ng-if="vm.question.question_type == vm.Types.FREETEXT"
                                ng-model="vm.question.title" i18n-placeholder="formulaire.question.title.free.empty" input-guard/>
                        <input type="text" class="ten" ng-if="vm.question.question_type != vm.Types.FREETEXT && vm.question.question_type != vm.Types.MATRIX"
                                ng-model="vm.question.title" i18n-placeholder="formulaire.question.title.empty" input-guard/>
                        <div class="flex-spaced" ng-if="vm.question.question_type == vm.Types.MATRIX">
                            <input type="text" class="nine" ng-model="vm.question.title" i18n-placeholder="formulaire.question.title.empty" input-guard/>
                            <select class="two" ng-if="!vm.hasFormResponses" ng-model="vm.matrixType" required
                                    ng-change="vm.onChangeMatrixType(vm.matrixType)" input-guard>
                                <option ng-repeat="matrixType in vm.matrixTypes" ng-value="matrixType"
                                        ng-attr-selected="[[matrixType === vm.matrixType ? 'selected' : undefined]]">
                                    [[vm.I18n.translate('formulaire.matrix.type.' + vm.Types[matrixType])]]
                                </option>
                            </select>
                        </div>
                    </div>
                </div>
            </div>
        `,

        controller: function ($scope) {
            const vm: IViewModel = <IViewModel> this;
        },
        link: function ($scope, $element) {
            const vm: IViewModel = $scope.vm;
            vm.Types = Types;
            vm.I18n = I18nUtils;
            vm.matrixTypes = [vm.Types.SINGLEANSWERRADIO];

            vm.onChangeMatrixType = (matrixType: number) : void => {
                for (let child of vm.question.children.all) {
                    child.question_type = matrixType;
                }
            }
        }
    };
});
