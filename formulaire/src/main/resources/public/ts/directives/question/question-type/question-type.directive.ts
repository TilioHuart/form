import {Directive, ng} from "entcore";
import {Form, FormElements, Question, Types} from "@common/models";
import {IScope} from "angular";
import {RootsConst} from "../../../core/constants/roots.const";

interface IQuestionTypeProps {
    question: Question;
    form: Form;
    hasFormResponses: boolean;
    types: typeof Types;
    formElements: FormElements;
    matrixType: number;
}

interface IViewModel extends ng.IController, IQuestionTypeProps {
}

interface IQuestionTypeScope extends IScope, IQuestionTypeProps{
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;
    form: Form;
    hasFormResponses: boolean;
    types: typeof Types;
    formElements: FormElements;
    matrixType: number;

    constructor(private $scope: IQuestionTypeScope, private $sce: ng.ISCEService) {
        this.types = Types;
    }

    $onInit = async () : Promise<void> => {}

    $onDestroy = async () : Promise<void> => {}
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/question-type/question-type.html`,
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
        link: function ($scope: IQuestionTypeScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeDirective: Directive = ng.directive('questionType', directive);
