import {Directive, ng} from "entcore";
import {FormElements, Question} from "@common/models";
import {questionService} from "@common/services";
import {FormElementUtils, I18nUtils, UtilsUtils} from "@common/utils";
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

    createNewChild = () : void => {
        this.updateSorted();
        this.question.children.all.push(new Question(this.question.id, this.matrixType, this.question.children.all.length + 1));
        UtilsUtils.safeApply(this.$scope);
    };

    moveChild = (child: Question, direction: string) : void => {
        FormElementUtils.switchPositions(this.question.children, child.matrix_position - 1, direction, PropPosition.MATRIX_POSITION);
        this.question.children.all.sort((a: Question, b: Question) => a.matrix_position - b.matrix_position);
        this.updateSorted();
        UtilsUtils.safeApply(this.$scope);
    };

    deleteChild = async (index: number) : Promise<void> => {
        if (this.question.children.all[index].id) {
            await questionService.delete(this.question.children.all[index].id);
        }
        for (let i = index + 1; i < this.question.children.all.length; i++) {
            this.question.children.all[i].matrix_position--;
        }
        this.question.children.all.splice(index,1);
        this.question.children.all.sort((a: Question, b: Question) => a.matrix_position - b.matrix_position);
        this.updateSorted();
        UtilsUtils.safeApply(this.$scope);
    };

    updateSorted = () : void => {
        let currentChildren: number[] = this.question.children.all
            .sort((a: Question, b: Question) => a.matrix_position - b.matrix_position)
            .map((q: Question) => q.matrix_position);
        let sortedQuestions: number[] = this.question.children.all
            .sort((a: Question, b: Question) => a.title > b.title ? 1 : -1)
            .map((q: Question) => q.matrix_position);
        this.question.children.sorted = UtilsUtils.areArrayEqual(currentChildren, sortedQuestions);
    }

    sortChildren = () : void => {
        this.question.children.all = this.question.children.sorted ?
            this.question.children.all.reverse() :
            this.question.children.all.sort((a: Question, b: Question) => a.title > b.title ? 1 : -1);
        for (let i = 0; i < this.question.children.all.length; i++) this.question.children.all[i].matrix_position = i+1;
        this.question.children.all.sort((a: Question, b: Question) => a.matrix_position - b.matrix_position);
        this.question.children.sorted = !this.question.children.sorted;
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directiveQuestionTypes}/question-type-matrix/question-type-matrix.html`,
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
