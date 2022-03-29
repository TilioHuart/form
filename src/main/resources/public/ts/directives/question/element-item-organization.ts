import {Directive, ng} from "entcore";
import {FormElement} from "../../models";
import {Direction, FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "../../core/enums";

interface IViewModel {
    formElement: FormElement;
    isSectionChild: boolean,
    isFirst: boolean;
    isLast: boolean;
    Direction: typeof Direction;

    moveQuestion(formElement: FormElement, direction: string): void;
}

export const elementItemOrganization: Directive = ng.directive('elementItemOrganization', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            formElement: '=',
            isSectionChild: '=',
            isFirst: '=',
            isLast: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="row-shadow-effect" ng-class="{ sectionChild: vm.isSectionChild }">
                <div class="top">
                    <div class="dots">
                        <i class="i-drag lg-icon dark-grey"></i>
                        <i class="i-drag lg-icon dark-grey"></i>
                    </div>
                </div>
                <div class="main">
                    <span class="title">[[vm.formElement.title]]</span>
                    <div class="one two-mobile container-arrow">
                        <div ng-class="vm.isFirst ? 'hidden' : ''" ng-click="vm.moveQuestion(vm.formElement, vm.Direction.UP)">
                            <i class="i-chevron-up lg-icon"></i>
                        </div>
                        <div ng-class="vm.isLast ? 'hidden' : ''" ng-click="vm.moveQuestion(vm.formElement, vm.Direction.DOWN)">
                            <i class="i-chevron-down lg-icon"></i>
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
            vm.Direction = Direction;

            vm.moveQuestion = (formElement: FormElement, direction: string) : void => {
                let data = {
                    formElement: formElement,
                    direction: direction
                }
                $scope.$emit(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.MOVE_QUESTION, data);
            }
        }
    };
});
