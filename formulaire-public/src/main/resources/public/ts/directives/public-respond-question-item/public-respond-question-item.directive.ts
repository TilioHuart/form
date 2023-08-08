import {ng} from "entcore";
import {
    Question,
    QuestionChoice,
    Response,
    Responses,
    Types
} from "@common/models";
import {Direction, FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";
import {FormElementUtils, I18nUtils, RankingUtils} from "@common/utils";
import {IScope} from "angular";
import {PropPosition} from "@common/core/enums/prop-position";
import * as Sortable from "sortablejs";
import {RootsConst} from "../../core/constants/roots.const";

interface IPublicQuestionItemProps {
    question: Question;
    responses: Responses;
    Types: typeof Types;
    I18n: I18nUtils;
    mapChoiceResponseIndex: Map<QuestionChoice, number>;
    direction: typeof Direction;
}

interface IViewModel extends ng.IController, IPublicQuestionItemProps {
    question: Question;
    direction: typeof Direction;

    init(): Promise<void>;
    getHtmlDescription(description: string): string;
    moveResponse(resp: Response, direction: string): void;
    isSelectedChoiceCustom(choiceId: number): boolean;
    deselectIfEmpty(choice: QuestionChoice) : void;
    onClickChoice(choice: QuestionChoice): void;
    resetDate(): void;
    hasImages(): boolean;
}

interface IPublicQuestionItemScope extends IScope, IPublicQuestionItemProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;
    responses: Responses;
    Types = Types;
    I18n = I18nUtils;
    mapChoiceResponseIndex: Map<QuestionChoice, number>;
    direction = Direction;

    constructor(private $scope: IPublicQuestionItemScope, private $sce: ng.ISCEService) {
        this.Types = Types;
        this.direction = Direction;
    }

    $onInit = async () : Promise<void> => {
        await this.init();

        // used when previous question is same type than this one
        this.$scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.REFRESH_QUESTION, (event, data) => {
            this.responses = data.get(this.question);
            this.init();
        });

        if (this.question.question_type === Types.RANKING) {
            this.initDrag();
        }
    }

    $onDestroy = async (): Promise<void> => {}

    init = async () : Promise<void> => {
        if (this.responses == null) return; // happens when previous question is not same type than this one

        if (this.question.question_type === Types.TIME && this.responses.all[0].answer) {
            this.responses.all[0].answer = new Date("January 01 1970 " + this.responses.all[0].answer);
        }

        if (this.question.isTypeMultipleRep()) {
            this.mapChoiceResponseIndex = new Map();
            for (let choice of this.question.choices.all) {
                let matchingResponses: Response[] = this.responses.all.filter((r:Response) => r.choice_id == choice.id);
                if (matchingResponses.length != 1) console.error("Be careful, 'vm.responses' has been badly implemented !!");
                this.mapChoiceResponseIndex.set(choice, this.responses.all.indexOf(matchingResponses[0]));

                // If question type multiplanswer, assign image to each choice
                if (this.question.canHaveImages()) {
                    for (let response of this.responses.all) {
                        const choice: QuestionChoice = this.question.choices.all.find((c: QuestionChoice) => c.id === response.choice_id);
                        if (choice) {
                            response.image = choice.image;
                        }
                    }
                }
            }
        }

        if (this.question.question_type === Types.CURSOR && this.question.specific_fields) {
            let answer: number = Number.parseInt(this.responses.all[0].answer.toString());
            this.responses.all[0].answer = Number.isNaN(answer) ? this.question.specific_fields.cursor_min_val : answer;
        }
    }

    getHtmlDescription = (description: string): string => {
        return !!description ? this.$sce.trustAsHtml(description) : null;
    }

    moveResponse = (resp: Response, direction: string) : void => {
        FormElementUtils.switchPositions(this.responses, resp.choice_position - 1, direction, PropPosition.CHOICE_POSITION);
        this.responses.all.sort((a: Response, b: Response) => a.choice_position - b.choice_position);
    };

    isSelectedChoiceCustom = (choiceId: number) : boolean => {
        let selectedChoice: QuestionChoice = this.question.choices.all.find((c: QuestionChoice) => c.id === choiceId);
        return selectedChoice && selectedChoice.is_custom;
    }

    deselectIfEmpty = (choice: QuestionChoice) : void => { // Unselected choice if custom answer is empty
        if (this.question.question_type === Types.SINGLEANSWERRADIO) {
            this.responses.all[0].choice_id = this.responses.all[0].custom_answer.length > 0 ? choice.id : null;
        }
        else if (this.question.question_type === Types.MULTIPLEANSWER) {
            let response: Response = this.responses.all[this.mapChoiceResponseIndex.get(choice)];
            response.selected = response.custom_answer.length > 0;
        }
        else return;
    }

    onClickChoice = (choice: QuestionChoice) : void => {
        this.responses.all[0].choice_id = (this.responses.all[0].choice_id != choice.id) ? choice.id : null;
    }

    resetDate = () : void => {
        this.responses.all[0].answer = new Date();
    }

    hasImages = () : boolean => {
        return this.question.choices.all.some((choice: QuestionChoice) => choice.image !== null && choice.image !== undefined && choice.image !== '');
    }

    initDrag = (): void => {
        // Loop through each sortable response for DragAndDrop in view response
        window.setTimeout((): void => {
            let respDrag = document.querySelectorAll(".drag-container");
            for (let i = 0; i < respDrag.length; i++) {
                Sortable.create(respDrag[i], {
                    group: "drag-container",
                    animation: 150,
                    fallbackOnBody: true,
                    swapThreshold: 0.65,
                    ghostClass: "sortable-ghost",
                    onEnd: async (evt): Promise<void> => {
                        await RankingUtils.onEndRankingDragAndDrop(evt, this.responses);
                        this.$scope.$apply();
                    },
                });
            }
        }, 500);
    };
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}public-respond-question-item/public-respond-question-item.html`,
        transclude: true,
        scope: {
            question: '<',
            responses: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IPublicQuestionItemScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const publicQuestionItem = ng.directive('publicQuestionItem', directive);