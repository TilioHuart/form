import {Directive, ng} from "entcore";
import {FormElement} from "@common/models";
import {Direction, FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";
import {IScope} from "angular";
import {RootsConst} from "../../../core/constants/roots.const";

interface IElementItemOrganizationProps {
    formElement: FormElement;
    isSectionChild: boolean;
    isFirst: boolean;
    isLast: boolean;
}

interface IViewModel {
    direction: typeof Direction;

    moveQuestion(formElement: FormElement, direction: string): void;
}

interface IElementItemOrganizationScope extends IScope, IElementItemOrganizationProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    formElement: FormElement;
    isSectionChild: boolean;
    isFirst: boolean;
    isLast: boolean;
    direction: typeof Direction;

    constructor(private $scope: IElementItemOrganizationScope, private $sce: ng.ISCEService) {
        this.direction = Direction;
    }

    $onInit = async () : Promise<void> => {}

    $onDestroy = async () : Promise<void> => {}

    moveQuestion = (formElement: FormElement, direction: string) : void => {
        let data = {
            formElement: formElement,
            direction: direction
        }
        this.$scope.$emit(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.MOVE_QUESTION, data);
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/element-item-organization/element-item-organization.html`,
        transclude: true,
        scope: {
            formElement: '=',
            isSectionChild: '=',
            isFirst: '=',
            isLast: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IElementItemOrganizationScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    };
}

export const elementItemOrganization: Directive = ng.directive('elementItemOrganization', directive);
