import {Directive, ng} from "entcore";
import {Form, Question} from "@common/models";
import {I18nUtils} from "@common/utils";
import {Direction} from "@common/core/enums";
import {RootsConst} from "../../../../core/constants/roots.const";

interface IQuestionTypeMultipleanswerProps {
    question: Question;
    hasFormResponses: boolean;
    form: Form;
}

interface IViewModel {
    i18n: I18nUtils;
    direction: typeof Direction;
    selectedChoiceIndex: number;

    deleteChoice(index: number): Promise<void>;
    displayImageSelect(index: number): void;
    deleteImageSelect(index: number): void;
}

class Controller implements ng.IController, IViewModel {
    question: Question;
    hasFormResponses: boolean;
    form: Form;
    i18n: I18nUtils;
    direction: typeof Direction;
    selectedChoiceIndex: number;

    constructor(private $scope: IQuestionTypeMultipleanswerProps, private $sce: ng.ISCEService) {
        this.i18n = I18nUtils;
        this.direction = Direction;
        this.selectedChoiceIndex = -1;
    }

    $onInit = async (): Promise<void> => {}

    $onDestroy = async () : Promise<void> => {}

    deleteChoice = async (index: number) : Promise<void> => {
        await this.question.deleteChoice(index);
    }

    displayImageSelect = (index: number): void => {
        this.selectedChoiceIndex = index;
    };

    deleteImageSelect = (index: number): void => {
        this.selectedChoiceIndex = -1;
        const choice = this.question.choices.all[index];
        choice.image = null;
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/question-type/question-type-multipleanswer/question-type-multipleanswer.html`,
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
        link: function ($scope: IQuestionTypeMultipleanswerProps,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeMultipleanswer: Directive = ng.directive('questionTypeMultipleanswer', directive);
