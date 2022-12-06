import {Directive, ng} from "entcore";
import {Question} from "@common/models";

interface IViewModel {
    question: Question;

    $onInit(): Promise<void>;
    onChangeStep(newStep: number): void;
}

export const questionTypeCursor: Directive = ng.directive('questionTypeCursor', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="formulaire-cursor-form">
                <!-- Line about MIN value and label -->
                <div class="cursor-line">
                    <div class="cursor-value-min twelve-mobile">
                        <div class="six four-mobile"><label><i18n>formulaire.question.value.minimum</i18n><em>*</em> : </label></div>
                        <div><input type="number" ng-model="vm.question.cursor_min_val"
                               max="[[vm.question.cursor_max_val - vm.question.cursor_step]]" pattern="\\d*"
                               i18n-placeholder="formulaire.question.cursor.default.min.val"></div>
                    </div>
                    <div class="cursor-label-min-val">
                        <label><i18n>formulaire.question.value.label</i18n></label>
                        <input type="text" ng-model="vm.question.cursor_label_min_val" 
                               i18n-placeholder="formulaire.question.label">
                    </div>
                </div>
                <!-- Line about MAX value and label -->
                <div class="cursor-line">
                    <div class="cursor-value-max twelve-mobile">
                        <div class="six four-mobile"><label><i18n>formulaire.question.value.maximum</i18n><em>*</em> : </label></div>
                        <div><input type="number" ng-model="vm.question.cursor_max_val"
                               min="[[vm.question.cursor_min_val + vm.question.cursor_step]]" pattern="\\d*"
                               i18n-placeholder="formulaire.question.cursor.default.max.val"></div>
                    </div>   
                    <div class="cursor-label-max-value">
                        <label><i18n>formulaire.question.value.label</i18n></label>
                        <input type="text" ng-model="vm.question.cursor_label_max_val"
                               i18n-placeholder="formulaire.question.label">
                    </div> 
                </div>
                <!-- Line about STEP value -->
                <div class="cursor-line">
                    <div class="cursor-value-step">
                        <div class="six four-mobile"><label><i18n>formulaire.question.value.step</i18n><em>*</em> : </label></div>
                        <div><input type="number" ng-model="vm.question.cursor_step"
                               ng-change="vm.onChangeStep(vm.question.cursor_step)" pattern="\\d*"
                               i18n-placeholder="formulaire.question.cursor.default.step"></div>
                    </div>
                </div>
            </div>
        `,
        controller: async ($scope) => {
            const vm: IViewModel = <IViewModel> this;

            vm.$onInit = async () : Promise<void> => {
                await initQuestionTypeCursor();
            }

            const initQuestionTypeCursor = async () : Promise<void> => {
                vm.question.cursor_min_val = 1;
                vm.question.cursor_max_val = 10;
                vm.question.cursor_step = 1;
            }
        },
        link: ($scope, $element) => {
            const vm: IViewModel = $scope.vm;

            vm.onChangeStep = (newStep: number) : void => {
                if (vm.question.cursor_max_val - newStep < vm.question.cursor_min_val) {
                    vm.question.cursor_max_val = vm.question.cursor_min_val + newStep;
                }
            }
        }
    };
});
