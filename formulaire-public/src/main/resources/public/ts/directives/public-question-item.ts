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
    }

    $onDestroy = async (): Promise<void> => {}

    init = async () : Promise<void> => {
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
        this.$scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.REFRESH_QUESTION, () => { this.init(); });
    }

    getHtmlDescription = (description: string): string => {
        return !!description ? this.$sce.trustAsHtml(description) : null;
    }
}

function directive() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
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
                        <textarea ng-model="vm.responses.all[0].answer" i18n-placeholder="[[vm.question.placeholder]]" input-guard></textarea>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.LONGANSWER">
                        <textarea ng-model="vm.responses.all[0].answer" input-guard></textarea>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.SINGLEANSWER">
                        <select ng-model="vm.responses.all[0].choice_id" input-guard>
                            <option ng-value="">[[vm.I18n.translate('formulaire.public.options.select')]]</option>
                            <option ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']" ng-value="choice.id">[[choice.value]]</option>
                        </select>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.MULTIPLEANSWER">
                        <div ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']">
                            <label>
                                <input type="checkbox" ng-model="vm.responses.all[vm.mapChoiceResponseIndex.get(choice)].selected" input-guard>
                                <span>[[choice.value]]</span>
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
                                <input type="radio" ng-model="vm.responses.all[0].choice_id" ng-value="choice.id" input-guard>[[choice.value]]
                            </label>
                        </div>
                    </div>
                    <div ng-if ="vm.question.question_type == vm.Types.CURSOR">
                        <div class="formulaire-cursor-input-wrapper">
                            <div>
                                <label>[[vm.question.cursor_label_min_val]]</label> <!-- label minimum value (optional) -->
                            </div>
                            <div class="formulaire-cursor-input-range">
                                <!-- input range -->
                                <input type="range" ng-model="vm.responses.all[0].answer"
                                       ng-value="[[vm.question.cursor_min_val]]" value="[[vm.question.cursor_min_val]]"
                                       min="[[vm.question.cursor_min_val]]" max="[[vm.question.cursor_max_val]]" 
                                       step="[[vm.question.cursor_step]]" oninput="rangevalue.value = value">
                               <div class="formulaire-cursor-input-range-values">
                                    <output>[[vm.question.cursor_min_val]]</output> <!-- minimum value -->
                                    <output>[[vm.question.cursor_max_val]]</output> <!-- maximum value -->
                               </div>
                            </div>
                            <div>
                                <label>[[vm.question.cursor_label_max_val]]</label> <!-- label maximum value (optional) -->
                            </div>
                        </div>
                        
                        <!-- chosen value -->
                        <label><i18n>formulaire.public.question.selected.result</i18n></label>
                        <output id="rangevalue">[[vm.question.cursor_min_val]]</output>
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