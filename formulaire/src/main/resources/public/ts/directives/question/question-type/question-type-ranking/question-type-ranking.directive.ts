import {Directive, ng} from "entcore";
import {Question, QuestionChoice} from "@common/models";
import {FormElementUtils, I18nUtils} from "@common/utils";
import {PropPosition} from "@common/core/enums/prop-position";
import {questionChoiceService} from "@common/services";
import {Direction} from "@common/core/enums";
import {RootsConst} from "../../../../core/constants/roots.const";

interface IQuestionTypeRankingProps {
    I18n: I18nUtils;
    question: Question;
    direction: typeof Direction;
}

interface IViewModel extends ng.IController, IQuestionTypeRankingProps {
    createNewChoice(): void;
    moveChoice(choice: QuestionChoice, direction: string): void;
    deleteChoice(index: number): Promise<void>;
}

class Controller implements ng.IController, IViewModel {
    I18n: I18nUtils;
    question: Question;
    direction: typeof Direction;

    constructor(private $scope: IQuestionTypeRankingProps, private $sce: ng.ISCEService) {
        this.I18n = I18nUtils;
        this.direction = Direction;
    }

    $onInit = async () : Promise<void> => {

    }

    $onDestroy = async () : Promise<void> => {}

    getHtmlDescription = (description: string) : string => {
        return !!description ? this.$sce.trustAsHtml(description) : null;
    }

    createNewChoice = () : void => {
        this.question.choices.all.push(new QuestionChoice(this.question.id, this.question.choices.all.length + 1));
    }

    moveChoice = (choice: QuestionChoice, direction: string) : void => {
        FormElementUtils.switchPositions(this.question.choices, choice.position - 1, direction, PropPosition.POSITION);
        this.question.choices.all.sort((a: QuestionChoice, b: QuestionChoice) => a.position - b.position);
    }

    deleteChoice = async (index: number) : Promise<void> => {
        if (this.question.choices.all[index].id) {
            await questionChoiceService.delete(this.question.choices.all[index].id);
        }
        for (let i = index + 1; i < this.question.choices.all.length; i++) {
            this.question.choices.all[i].position--;
        }
        this.question.choices.all.splice(index,1);
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/question-type/question-type-ranking/question-type-ranking.html`,
        transclude: true,
        scope: {
            question: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeRankingProps,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeRanking: Directive = ng.directive('questionTypeRanking', directive);