import {Directive, ng} from "entcore";
import {Question} from "@common/models";
import {IScope} from "angular";
import {RootsConst} from "../../../../core/constants/roots.const";

interface IQuestionTypDateProps {
    question: Question;
}

interface IViewModel {}

interface IQuestionTypeDateScope extends IScope, IQuestionTypDateProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;

    constructor(private $scope: IQuestionTypeDateScope, private $sce: ng.ISCEService) {}
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/question-type/question-type-date/question-type-date.html`,
        transclude: true,
        scope: {
            question: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeCursorScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeDate: Directive = ng.directive('questionTypeDate', directive);