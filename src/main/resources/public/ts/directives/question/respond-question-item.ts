import {Directive, idiom, ng} from "entcore";
import {Question, Types} from "../../models";

interface IViewModel {
    question: Question,
    response: Response,
    files: any,
    Types: typeof Types,

    displayDefaultOption(): string
}

export const respondQuestionItem: Directive = ng.directive('respondQuestionItem', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            response: '=',
            files: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="question" guard-root>
                <div class="question-title">
                    <h4>[[vm.question.title]]<span ng-if="vm.question.mandatory" style="color:red;margin-left:10px">*</span></h4>
                </div>
                <div class="question-main">
                    <div ng-if="vm.question.question_type == vm.Types.FREETEXT">
                        <div ng-if="vm.question.statement" bind-html="vm.question.statement"></div>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.SHORTANSWER">
                        <textarea ng-model="vm.response.answer" i18n-placeholder="formulaire.question.type.SHORTANSWER" input-guard></textarea>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.LONGANSWER">
                        <editor ng-model="vm.response.answer" input-guard></editor>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.SINGLEANSWER">
                        <select ng-model="vm.response.choice_id" input-guard>
                            <option ng-value="">[[vm.displayDefaultOption()]]</option>
                            <option ng-repeat="choice in vm.question.choices.all" ng-value="choice.id">[[choice.value]]</option>
                        </select>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.MULTIPLEANSWER">
                        <div ng-repeat="choice in vm.question.choices.all">
                            <label for="check-[[choice.id]]">
                                <input type="checkbox" id="check-[[choice.id]]" ng-model="vm.response.selectedIndex[$index]" input-guard>
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
                    <div ng-if="vm.question.question_type == vm.Types.FILE">
                        <formulaire-picker-file files="vm.files" multiple="true" input-guard></formulaire-picker-file>
                    </div>
                    <div ng-if ="vm.question.question_type == vm.Types.SINGLEANSWERRADIO">
                        <div ng-repeat ="choice in vm.question.choices.all">
                            <label>
                                <input type="radio" ng-model="vm.response.choice_id" ng-value="[[choice.id]]" input-guard>[[choice.value]]
                            </label>
                        </div>
                    </div>
                </div>
            </div>
        `,

        controller: async ($scope) => {
            const vm: IViewModel = <IViewModel> this;
        },
        link: ($scope, $element) => {
            const vm: IViewModel = $scope.vm;
            vm.Types = Types;

            vm.displayDefaultOption = () : string => {
                return idiom.translate('formulaire.options.select');
            };
        }
    };
});
