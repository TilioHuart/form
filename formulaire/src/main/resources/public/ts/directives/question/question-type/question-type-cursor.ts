import {ng} from "entcore";
import {Question} from "@common/models";
import {IScope} from "angular";

interface IQuestionTypeFreetextProps {
    question: Question;
    hasFormResponses: boolean;
}

interface IViewModel extends ng.IController, IQuestionTypeFreetextProps {
    onChangeStep(newStep: number): void;
}

interface IQuestionTypeFreetextScope extends IScope, IQuestionTypeFreetextProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;
    hasFormResponses: boolean;

    constructor(private $scope: IQuestionTypeFreetextScope, private $sce: ng.ISCEService) {}

    $onInit = async () : Promise<void> => {}

    $onDestroy = async () : Promise<void> => {}

    getHtmlDescription = (description: string) : string => {
        return !!description ? this.$sce.trustAsHtml(description) : null;
    }

    onChangeStep = (newStep: number) : void => {
        if (this.question.cursor_max_val - newStep < this.question.cursor_min_val) {
            this.question.cursor_max_val = this.question.cursor_min_val + newStep;
        }
    }
}

function directive() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            hasFormResponses: '=',
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="formulaire-cursor-form">
                <!-- Line about MIN value and label -->
                <div class="cursor-line">
                    <div class="cursor-value-min twelve-mobile">
                        <div class="eight four-mobile"><label><i18n>formulaire.question.value.minimum</i18n><em>*</em> : </label></div>
                        <div ng-if="!vm.hasFormResponses">
                            <input type="number" ng-model="vm.question.cursor_min_val"
                               max="[[vm.question.cursor_max_val - vm.question.cursor_step]]" pattern="\\d*"
                               i18n-placeholder="formulaire.question.cursor.default.min.val">
                        </div>
                        <div ng-if="vm.hasFormResponses">
                            <input type="number" disabled ng-model="vm.question.cursor_min_val">
                        </div>
                    </div>
                    <div class="cursor-label-min-val">
                        <label><i18n>formulaire.question.value.label</i18n></label>
                        <input type="text" ng-model="vm.question.cursor_min_label" 
                               i18n-placeholder="formulaire.question.label">
                    </div>
                </div>
                <!-- Line about MAX value and label -->
                <div class="cursor-line">
                    <div class="cursor-value-max twelve-mobile">
                        <div class="eight four-mobile"><label><i18n>formulaire.question.value.maximum</i18n><em>*</em> : </label></div>
                        <div ng-if="!vm.hasFormResponses">
                            <input type="number" ng-model="vm.question.cursor_max_val"
                               min="[[vm.question.cursor_min_val + vm.question.cursor_step]]" pattern="\\d*"
                               i18n-placeholder="formulaire.question.cursor.default.max.val">
                        </div>
                        <div ng-if="vm.hasFormResponses">
                            <input type="number" disabled ng-model="vm.question.cursor_min_val">
                        </div>
                    </div>   
                    <div class="cursor-label-max-value">
                        <label><i18n>formulaire.question.value.label</i18n></label>
                        <input type="text" ng-model="vm.question.cursor_max_label"
                               i18n-placeholder="formulaire.question.label">
                    </div> 
                </div>
                <!-- Line about STEP value -->
                <div class="cursor-line">
                    <div class="cursor-value-step">
                        <div class="eight four-mobile"><label><i18n>formulaire.question.value.step</i18n><em>*</em> : </label></div>
                        <div ng-if="!vm.hasFormResponses">
                            <input type="number" ng-model="vm.question.cursor_step"
                               ng-change="vm.onChangeStep(vm.question.cursor_step)" pattern="\\d*"
                               i18n-placeholder="formulaire.question.cursor.default.step">
                        </div>
                        <div ng-if="vm.hasFormResponses">
                           <input type="number" disabled ng-model="vm.question.cursor_step">
                        </div>
                    </div>
                </div>
            </div>
        `,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeFreetextScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeCursor = ng.directive('questionTypeCursor', directive);
