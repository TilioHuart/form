import {Directive, ng} from "entcore";
import {Form, Question} from "@common/models";
import {IScope} from "angular";
import {RootsConst} from "../../../../core/constants/roots.const";

interface IQuestionTypeFreetextProps {
    question: Question;
    form: Form;
}

interface IViewModel extends ng.IController, IQuestionTypeFreetextProps {
    getHtmlDescription(description: string) : string;
}

interface IQuestionTypeFreetextScope extends IScope, IQuestionTypeFreetextProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;
    form: Form;

    constructor(private $scope: IQuestionTypeFreetextScope, private $sce: ng.ISCEService) {}

    $onInit = async (): Promise<void> => {}

    $onDestroy = async (): Promise<void> => {}

    getHtmlDescription = (description: string): string => {
        return !!description ? this.$sce.trustAsHtml(description) : null;
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/question-type/question-type-freetext/question-type-freetext.html`,
        transclude: true,
        scope: {
            question: '=',
            form: '<',
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeFreetextScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeFreetext: Directive = ng.directive('questionTypeFreetext', directive);