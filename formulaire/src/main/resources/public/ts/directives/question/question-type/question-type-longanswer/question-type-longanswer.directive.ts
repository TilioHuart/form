import {Directive, ng} from "entcore";
import {Question} from "@common/models";
import {IScope} from "angular";
import {RootsConst} from "../../../../core/constants/roots.const";

interface IQuestionTypeLonganswerProps {
    question: Question
}

interface IViewModel extends ng.IController, IQuestionTypeLonganswerProps {}

interface IQuestionTypeLonganswerScope extends IScope, IQuestionTypeLonganswerProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;

    constructor(private $scope: IQuestionTypeLonganswerScope, private $sce: ng.ISCEService) {}

    $onInit = async () : Promise<void> => {}

    $onDestroy = async () : Promise<void> => {}
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/question-type/question-type-longanswer/question-type-longanswer.html`,
        transclude: true,
        scope: {
            question: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeLonganswerScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeLonganswer: Directive = ng.directive('questionTypeLonganswer', directive);