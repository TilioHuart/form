import {ng} from "entcore";
import {Question} from "@common/models";
import {IScope} from "angular";
import {RootsConst} from "../../../../core/constants/roots.const";

interface IQuestionTypeFileProps {
    question: Question;
}

interface IViewModel extends ng.IController, IQuestionTypeFileProps {
}

interface IQuestionTypeFileScope extends IScope, IQuestionTypeFileProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;

    constructor(private $scope: IQuestionTypeFileScope, private $sce: ng.ISCEService) {}

    $onInit = async () : Promise<void> => {}

    $onDestroy = async () : Promise<void> => {}
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/question-type/question-type-file/question-type-file.html`,
        transclude: true,
        scope: {
            question: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeFileScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeFile = ng.directive('questionTypeFile', directive);