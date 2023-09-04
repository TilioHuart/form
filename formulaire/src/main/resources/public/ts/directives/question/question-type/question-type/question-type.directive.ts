import {Directive, ng} from "entcore";
import {Form, FormElements, Question, Types} from "@common/models";
import {RootsConst} from "../../../../core/constants/roots.const";

interface IQuestionTypeProps {
    question: Question;
    form: Form;
    hasFormResponses: boolean;
    formElements: FormElements;
    matrixType: number;
}

interface IViewModel {
    types: typeof Types;
}

class Controller implements ng.IController, IViewModel {
    question: Question;
    form: Form;
    hasFormResponses: boolean;
    formElements: FormElements;
    matrixType: number;
    types: typeof Types;

    constructor(private $scope: IQuestionTypeProps, private $sce: ng.ISCEService) {
        this.types = Types;
    }

    $onInit = async (): Promise<void> => {}

    $onDestroy = async () : Promise<void> => {}
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/question-type/question-type/question-type.html`,
        transclude: true,
        scope: {
            question: '=',
            form: '<',
            hasFormResponses: '=',
            formElements: '<',
            matrixType: '<'
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeProps,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionType: Directive = ng.directive('questionType', directive);