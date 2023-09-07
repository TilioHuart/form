import {Directive, ng} from "entcore";
import {Question} from "@common/models";
import {IScope} from "angular";
import {RootsConst} from "../../../../core/constants/roots.const";

interface IQuestionTypeShortanswerProps {
    question: Question
}

interface IViewModel extends ng.IController, IQuestionTypeShortanswerProps {}

interface IQuestionTypeShortanswerScope extends IScope, IQuestionTypeShortanswerProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;

    constructor(private $scope: IQuestionTypeShortanswerScope, private $sce: ng.ISCEService) {}

    $onInit = async () : Promise<void> => {}

    $onDestroy = async () : Promise<void> => {}
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/question-type/question-type-shortanswer/question-type-shortanswer.html`,
        transclude: true,
        scope: {
            question: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeShortanswerScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeShortanswer: Directive = ng.directive('questionTypeShortanswer', directive);
