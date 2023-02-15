import {ng} from "entcore";
import {Question, QuestionChoice, Response, Responses, Types} from "@common/models";
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";
import {I18nUtils} from "@common/utils";
import {IScope} from "angular";

interface IPublicQuestionItemProps {
    question: Question;
    responses: Responses;
    Types: typeof Types;
    I18n: I18nUtils;
    mapChoiceResponseIndex: Map<QuestionChoice, number>;
}

interface IViewModel extends ng.IController, IPublicQuestionItemProps {
    init(): Promise<void>;
    getHtmlDescription(description: string): string;
    isSelectedChoiceCustom(choiceId: number): boolean;
    deselectIfEmpty(choice: QuestionChoice) : void;
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

    constructor(private $scope: IPublicQuestionItemScope, private $sce: ng.ISCEService) {}

    $onInit = async () : Promise<void> => {
        await this.init();

        // used when previous question is same type than this one
        this.$scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.REFRESH_QUESTION, (event, data) => {
            this.responses = data.get(this.question);
            this.init();
        });
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
            }
        }

        if (this.question.question_type === Types.CURSOR) {
            let answer: number = Number.parseInt(this.responses.all[0].answer.toString());
            this.responses.all[0].answer = Number.isNaN(answer) ? this.question.cursor_min_val : answer;
        }
    }

    getHtmlDescription = (description: string): string => {
        return !!description ? this.$sce.trustAsHtml(description) : null;
    }

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
}

function directive() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '<',
            responses: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="question" guard-root>
                <div class="question-title flex-spaced">
                    <h4>[[vm.question.title]]<span ng-if="vm.question.mandatory" style="color:red;margin-left:10px">*</span></h4>
                </div>
                <div class="question-main">
                    <div ng-if="vm.question.question_type == vm.Types.FREETEXT">
                        <div ng-if="vm.question.statement" data-ng-bind-html="vm.getHtmlDescription(vm.question.statement)"></div>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.SHORTANSWER">
                        <textarea ng-model="vm.responses.all[0].answer" i18n-placeholder="[[vm.question.placeholder || 'formulaire.question.type.SHORTANSWER']]" input-guard></textarea>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.LONGANSWER">
                        <textarea ng-model="vm.responses.all[0].answer" input-guard></textarea>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.SINGLEANSWER">
                        <select ng-model="vm.responses.all[0].choice_id" input-guard>
                            <option ng-value="">[[vm.I18n.translate('formulaire.public.options.select')]]</option>
                            <option ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']" ng-value="choice.id">[[choice.value]]</option>
                        </select>
                        <div ng-if="vm.isSelectedChoiceCustom(vm.responses.all[0].choice_id)">
                            <i18n>formulaire.public.response.custom.explanation</i18n>
                            <input type="text" ng-model="vm.responses.all[0].custom_answer" i18n-placeholder="formulaire.public.response.custom.write">
                        </div>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.MULTIPLEANSWER">
                        <div ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']">
                            <label>
                                <input type="checkbox" ng-model="vm.responses.all[vm.mapChoiceResponseIndex.get(choice)].selected" input-guard>
                                <span>[[choice.value]]</span>
                                <span ng-if="choice.is_custom"> : 
                                    <input type="text" ng-model="vm.responses.all[vm.mapChoiceResponseIndex.get(choice)].custom_answer"
                                           ng-change="vm.deselectIfEmpty(choice)"
                                           i18n-placeholder="formulaire.public.response.custom.write">
                                </span>
                            </label>
                        </div>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.DATE">
                        <date-picker ng-model="vm.responses.all[0].answer" input-guard></date-picker>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.TIME">
                        <input type="time" ng-model="vm.responses.all[0].answer" input-guard/>
                    </div>
                    <div ng-if ="vm.question.question_type == vm.Types.SINGLEANSWERRADIO">
                        <div ng-repeat ="choice in vm.question.choices.all | orderBy:['position', 'id']">
                            <label>
                                <input type="radio" ng-model="vm.responses.all[0].choice_id" ng-value="choice.id" input-guard>
                                <span>[[choice.value]]</span>
                                <span ng-if="choice.is_custom"> : 
                                    <input type="text" ng-model="vm.responses.all[0].custom_answer"
                                           ng-change="vm.deselectIfEmpty(choice)"
                                           i18n-placeholder="formulaire.public.response.custom.write">
                                </span>
                            </label>
                        </div>
                    </div>
                    <div ng-if ="vm.question.question_type == vm.Types.CURSOR">
                        <div class="formulaire-cursor-input-wrapper">
                            <div class="formulaire-cursor-input-label">
                                <label>[[vm.question.cursor_min_label]]</label> <!-- label minimum value (optional) -->
                            </div>
                            <div class="formulaire-cursor-input-range">
                                <div class="range-slider"
                                     style="--min:[[vm.question.cursor_min_val]];
                                            --max:[[vm.question.cursor_max_val]];
                                            --step:[[vm.question.cursor_step]];
                                            --value:[[vm.responses.all[0].answer]];">
                                    <!-- native cursor -->
                                    <input type="range" class="twelve" ng-model="vm.responses.all[0].answer"
                                           min="[[vm.question.cursor_min_val]]" max="[[vm.question.cursor_max_val]]" 
                                           step="[[vm.question.cursor_step]]">
                                   <!-- pin cursor -->
                                    <output class="pin">
                                        <div class="pin-content">[[vm.responses.all[0].answer]]</div>
                                    </output>
                                    <!-- progress bar -->
                                    <div class="filler"></div>
                                </div>
                                <!-- Display MIN and MAX -->
                                <div class="formulaire-cursor-input-range-values">
                                    <div>[[vm.question.cursor_min_val]]</div> <!-- minimum value -->
                                    <div>[[vm.question.cursor_max_val]]</div> <!-- maximum value -->
                                </div>
                            </div>
                            <div class="formulaire-cursor-input-label">
                                <label>[[vm.question.cursor_max_label]]</label> <!-- label maximum value (optional) -->
                            </div>
                        </div>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.RANKING">
                       <div ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']">
                           <label>
                               <span>[[choice.value]]</span>
                           </label>
                       </div>
                    </div>
                </div>
            </div>
        `,
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