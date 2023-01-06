import {ng} from "entcore";
import {Question, Response, Types} from "@common/models";
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";
import {I18nUtils} from "@common/utils";
import {IScope} from "angular";

interface IPublicQuestionItemProps {
    question: Question;
    response: Response;
    selectedIndexes: boolean[];
    Types: typeof Types;
    I18n: I18nUtils;
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
    response: Response;
    selectedIndexes: boolean[];
    Types = Types;
    I18n = I18nUtils;

    constructor(private $scope: IPublicQuestionItemScope, private $sce: ng.ISCEService) {}

    $onInit = async () : Promise<void> => {
        await this.init();
    }

    $onDestroy = async (): Promise<void> => {}

    init = async () : Promise<void> => {
        if (!this.response.question_id) { this.response.question_id = this.question.id; }
        if (this.question.question_type === Types.TIME && this.response.answer) {
            this.response.answer = new Date("January 01 1970 " + this.response.answer);
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
            response: '=',
            selectedIndexes: '='
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
                        <textarea ng-model="vm.response.answer" i18n-placeholder="[[vm.question.placeholder]]" input-guard></textarea>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.LONGANSWER">
                        <textarea ng-model="vm.response.answer" input-guard></textarea>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.SINGLEANSWER">
                        <select ng-model="vm.response.choice_id" input-guard>
                            <option ng-value="">[[vm.I18n.translate('formulaire.public.options.select')]]</option>
                            <option ng-repeat="choice in vm.question.choices.all" ng-value="choice.id">[[choice.value]]</option>
                        </select>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.MULTIPLEANSWER">
                        <div ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']">
                            <label for="check-[[choice.id]]">
                                <input type="checkbox" id="check-[[choice.id]]" ng-model="vm.selectedIndexes[$index]" input-guard>
                                <span>[[choice.value]]</span>
                            </label>
                        </div>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.DATE">
                        <date-picker ng-model="vm.response.answer" input-guard></date-picker>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.TIME">
                        <input type="time" ng-model="vm.response.answer" input-guard/>
                    </div>
                    <div ng-if ="vm.question.question_type == vm.Types.SINGLEANSWERRADIO">
                        <div ng-repeat ="choice in vm.question.choices.all | orderBy:['position', 'id']">
                            <label>
                                <input type="radio" ng-model="vm.response.choice_id" ng-value="[[choice.id]]" input-guard>[[choice.value]]
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