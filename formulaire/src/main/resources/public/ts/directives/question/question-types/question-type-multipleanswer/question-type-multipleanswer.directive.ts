import {Directive, ng} from "entcore";
import {Form, Question, QuestionChoice} from "@common/models";
import {I18nUtils} from "@common/utils";
import {Direction} from "@common/core/enums";
import {RootsConst} from "../../../../core/constants/roots.const";
import {IScope} from "angular";

interface IQuestionTypeMultipleanswerProps {
    question: Question;
    hasFormResponses: boolean;
    form: Form;
}

interface IQuestionTypeMultipleanswerScope extends IScope, IQuestionTypeMultipleanswerProps{
    vm: IViewModel;
}

interface IViewModel extends ng.IController, IQuestionTypeMultipleanswerProps {
    i18n: I18nUtils;
    direction: typeof Direction;
    selectedChoiceIndex: number;

    displayImageSelect(index: number): void;
    deleteImageSelect(index: number): void;
    sortChoices(index: number): void;
}

class Controller implements IViewModel {
    question: Question;
    hasFormResponses: boolean;
    form: Form;
    i18n: I18nUtils;
    direction: typeof Direction;
    selectedChoiceIndex: number;

    constructor(private $scope: IQuestionTypeMultipleanswerScope, private $sce: ng.ISCEService) {
        this.i18n = I18nUtils;
        this.direction = Direction;
        this.selectedChoiceIndex = -1;
    }

    $onInit = async (): Promise<void> => {}

    $onDestroy = async () : Promise<void> => {}

    displayImageSelect = (index: number): void => {
        this.selectedChoiceIndex = index;
    };

    deleteImageSelect = (index: number): void => {
        this.selectedChoiceIndex = -1;
        if (index) {
            let choice: QuestionChoice = this.question.choices.all[index];
            choice.image = null;
        }
    }

    sortChoices = (index: number): void => {
        this.deleteImageSelect(index);
        this.question.choices.sortChoices();
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directiveQuestionTypes}/question-type-multipleanswer/question-type-multipleanswer.html`,
        transclude: true,
        scope: {
            question: '=',
            hasFormResponses: '=',
            form: '<'
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeMultipleanswerScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeMultipleanswer: Directive = ng.directive('questionTypeMultipleanswer', directive);
