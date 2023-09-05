import {Directive, ng} from "entcore";
import {FormElements, Question, QuestionChoice} from "@common/models";
import {questionChoiceService, questionService} from "@common/services";
import {FormElementUtils, I18nUtils} from "@common/utils";
import {Direction} from "@common/core/enums";
import {PropPosition} from "@common/core/enums/prop-position";
import {IScope} from "angular";
import {RootsConst} from "../../../../core/constants/roots.const";

interface IQuestionTypeMatrixProps {
    question: Question;
    hasFormResponses: boolean;
    formElements: FormElements;
    matrixType: number;
}

interface IViewModel extends ng.IController, IQuestionTypeMatrixProps {
    i18n: I18nUtils;
    direction: typeof Direction;

    createNewChoice(): void;
    moveChoice(choice: QuestionChoice, direction: string): void;
    deleteChoice(index: number): Promise<void>;
    createNewChild(): void;
    moveChild(child: Question, direction: string): void;
    deleteChild(index: number): Promise<void>;
}

interface IQuestionTypeMatrixScope extends IScope, IQuestionTypeMatrixProps {
    vm: IViewModel;
}

class Controller implements ng.IController, IViewModel {
    question: Question;
    hasFormResponses: boolean;
    formElements: FormElements;
    matrixType: number;
    i18n: I18nUtils;
    direction: typeof Direction;

    constructor(private $scope: IQuestionTypeMatrixScope, private $sce: ng.ISCEService) {
        this.i18n = I18nUtils;
        this.direction = Direction;
    }

    $onInit = async (): Promise<void> => {}

    $onDestroy = async () : Promise<void> => {}

    createNewChoice = () : void => {
        this.question.choices.all.push(new QuestionChoice(this.question.id, this.question.choices.all.length + 1));
        this.$scope.$apply();
    };

    moveChoice = (choice: QuestionChoice, direction: string) : void => {
        FormElementUtils.switchPositions(this.question.choices, choice.position - 1, direction, PropPosition.POSITION);
        this.question.choices.all.sort((a: QuestionChoice, b: QuestionChoice) => a.position - b.position);
        this.$scope.$apply();
    };

    deleteChoice = async (index: number) : Promise<void> => {
        if (this.question.choices.all[index].id) {
            await questionChoiceService.delete(this.question.choices.all[index].id);
        }
        for (let i = index + 1; i < this.question.choices.all.length; i++) {
            this.question.choices.all[i].position--;
        }
        this.question.choices.all.splice(index,1);
        this.$scope.$apply();
    };

    createNewChild = () : void => {
        this.question.children.all.push(new Question(this.question.id, this.matrixType, this.question.children.all.length + 1));
        this.$scope.$apply();
    };

    moveChild = (child: Question, direction: string) : void => {
        FormElementUtils.switchPositions(this.question.children, child.matrix_position - 1, direction, PropPosition.MATRIX_POSITION);
        this.question.children.all.sort((a: Question, b: Question) => a.matrix_position - b.matrix_position);
        this.$scope.$apply();
    };

    deleteChild = async (index: number) : Promise<void> => {
        if (this.question.children.all[index].id) {
            await questionService.delete(this.question.children.all[index].id);
        }
        for (let i = index + 1; i < this.question.children.all.length; i++) {
            this.question.children.all[i].matrix_position--;
        }
        this.question.children.all.splice(index,1);
        this.$scope.$apply();
    };

}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/question-type/question-type-matrix/question-type-matrix.html`,
        transclude: true,
        scope: {
            question: '=',
            hasFormResponses: '=',
            formElements: '<',
            matrixType: '<'
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeMatrixProps,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeMatrix: Directive = ng.directive('questionTypeMatrix', directive);
