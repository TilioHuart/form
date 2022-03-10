import {Directive, idiom, ng} from "entcore";
import {Distribution, DistributionStatus, Form, Question, QuestionChoice, Responses, Types} from "../../models";

interface IViewModel {
    question: Question;
    responses: Responses;
    form: Form;
    distribution: Distribution;
    Types: typeof Types;
    DistributionStatus: typeof DistributionStatus;

    getStringResponse(): string;
    isSelectedChoice(choice: QuestionChoice) : boolean;
    getResponseFileNames() : string[];
}

export const recapQuestionItem: Directive = ng.directive('recapQuestionItem', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            responses: '=',
            form: '=',
            distribution: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
                <div class="question">
                    <div class="question-title">
                        <h4>[[vm.question.title]]<span ng-if="vm.question.mandatory" style="color:red; margin-left:10px">*</span></h4>
                    </div>
                    <div class="question-main">
                        <div ng-if="vm.question.question_type == vm.Types.FREETEXT">
                            <div ng-if="vm.question.statement" bind-html="vm.question.statement"></div>
                        </div>
                        <div ng-if="vm.question.question_type == vm.Types.SHORTANSWER">
                            <div bind-html="vm.getStringResponse(vm.question)"></div>
                        </div>
                        <div ng-if="vm.question.question_type == vm.Types.LONGANSWER">
                            <div bind-html="vm.getStringResponse(vm.question)"></div>
                        </div>
                        <div ng-if="vm.question.question_type == vm.Types.SINGLEANSWER">
                            <div bind-html="vm.getStringResponse(vm.question)"></div>
                        </div>
                        <div ng-if="vm.question.question_type == vm.Types.MULTIPLEANSWER">
                            <div ng-repeat="choice in vm.question.choices.all">
                                <label>
                                    <input type="checkbox" disabled checked ng-if="vm.isSelectedChoice(choice)">
                                    <input type="checkbox" disabled ng-if="!vm.isSelectedChoice(choice)">
                                    <span style="cursor: default"></span>
                                    <span class="ten eight-mobile">[[choice.value]]</span>
                                </label>
                            </div>
                        </div>
                        <div ng-if="vm.question.question_type == vm.Types.DATE">
                            <div bind-html="vm.getStringResponse(vm.question)"></div>
                        </div>
                        <div ng-if="vm.question.question_type == vm.Types.TIME">
                            <div bind-html="vm.getStringResponse(vm.question)"></div>
                        </div>
                        <div ng-if="vm.question.question_type == vm.Types.FILE">
                            <div ng-repeat="filename in vm.getResponseFileNames()">
                                <span bind-html="[[filename]]"></span>
                            </div>
                        </div>
                        <div ng-if="vm.question.question_type == vm.Types.SINGLEANSWERRADIO">
                            <div ng-repeat="choice in vm.question.choices.all">
                                <label>
                                    <input type="radio" disabled checked ng-if="vm.isSelectedChoice(choice)">
                                    <input type="radio" disabled ng-if="!vm.isSelectedChoice(choice)">
                                    <span style="cursor: default"></span>
                                    <span class="ten eight-mobile">[[choice.value]]</span>
                                </label>
                            </div>
                        </div>
                    </div>
                    <div class="question-edit" ng-if="(vm.form.editable || vm.distribution.status != vm.DistributionStatus.FINISHED) && vm.question.question_type != vm.Types.FREETEXT">
                        <a href="#/form/[[vm.form.id]]/[[vm.distribution.id]]/question/[[vm.question.position]]"><i18n>formulaire.edit</i18n></a>
                    </div>
                </div>
        `,

        controller: function ($scope) {
            const vm: IViewModel = <IViewModel> this;
        },
        link: function ($scope, $element) {
            const vm: IViewModel = $scope.vm;
            vm.Types = Types;
            vm.DistributionStatus = DistributionStatus;
            let missingResponse = "<em>" + idiom.translate('formulaire.response.missing') + "</em>";

            // Display helper functions

            vm.getStringResponse = () : string => {
                let responses = vm.responses.all.filter(r => r.question_id === vm.question.id);
                if (responses && responses.length > 0) {
                    let answer = responses[0].answer.toString();
                    return answer ? answer : missingResponse;
                }
                return missingResponse;
            };

            vm.isSelectedChoice = (choice) : boolean => {
                let selectedChoices: any = vm.responses.all.filter(r => r.question_id === vm.question.id).map(r => r.choice_id);
                return selectedChoices.includes(choice.id);
            };

            vm.getResponseFileNames = () : string[] => {
                let responses = vm.responses.all.filter(r => r.question_id === vm.question.id);
                if (responses && responses.length === 1 && responses[0].files.all.length > 0) {
                    return responses[0].files.all.map(rf => rf.filename.substring(rf.filename.indexOf("_") + 1));
                }
                else {
                    return [missingResponse];
                }
            };
        }
    };
});
