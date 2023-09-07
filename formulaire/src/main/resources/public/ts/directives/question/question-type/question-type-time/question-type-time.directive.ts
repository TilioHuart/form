import {Directive, ng} from "entcore";
import {Question} from "@common/models";
import {IScope} from "angular";
import {RootsConst} from "../../../../core/constants/roots.const";

interface IQuestionTypeTimeProps {
    question: Question;
}

interface IViewModel extends ng.IController, IQuestionTypeTimeProps {}

interface IQuestionTypeTimeScope extends IScope, IQuestionTypeTimeProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;

    constructor(private $scope: IQuestionTypeTimeScope, private $sce: ng.ISCEService) {}

    $onInit = async (): Promise<void> => {}

    $onDestroy = async () : Promise<void> => {}
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/question-type/question-type-time/question-type-time.html`,
        transclude: true,
        scope: {
            question: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeTimeScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeTime: Directive = ng.directive('questionTypeTime', directive);
