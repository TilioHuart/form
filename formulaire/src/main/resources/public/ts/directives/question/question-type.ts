import {Directive, ng} from "entcore";
import {FormElements, Question, Types} from "@common/models";

interface IViewModel {
    question: Question;
    hasFormResponses: boolean;
    types: typeof Types;
    formElements: FormElements;
    matrixType: number;
}

export const questionType: Directive = ng.directive('questionType', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            hasFormResponses: '=',
            formElements: '<',
            matrixType: '<'
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="question-type focusable">
                <!-- FREETEXT -->
                <question-type-freetext ng-if="vm.question.question_type == vm.types.FREETEXT"
                                        question="vm.question">
                </question-type-freetext>
                <!-- SHORTANSWER -->
                <question-type-shortanswer ng-if="vm.question.question_type == vm.types.SHORTANSWER"
                                           question="vm.question">
                </question-type-shortanswer>
                <!-- LONGANSWER -->
                <question-type-longanswer ng-if="vm.question.question_type == vm.types.LONGANSWER"
                                          question="vm.question">
                </question-type-longanswer>
                <!-- SINGLEANSWER -->
                <question-type-singleanswer ng-if="vm.question.question_type == vm.types.SINGLEANSWER"
                                            question="vm.question"
                                            has-form-responses="vm.hasFormResponses"
                                            form-elements="vm.formElements"
                                            is-radio="false">
                </question-type-singleanswer>
                <!-- MULTIPLEANSWER -->
                <question-type-multipleanswer ng-if="vm.question.question_type == vm.types.MULTIPLEANSWER"
                                              question="vm.question"
                                              has-form-responses="vm.hasFormResponses">
                </question-type-multipleanswer>
                <!-- DATE -->
                <question-type-date ng-if="vm.question.question_type == vm.types.DATE"
                                    question="vm.question">
                </question-type-date>
                <!-- TIME -->
                <question-type-time ng-if="vm.question.question_type == vm.types.TIME"
                                    question="vm.question" >
                </question-type-time>
                <!-- FILE -->
                <question-type-file ng-if="vm.question.question_type == vm.types.FILE"
                                    question="vm.question">
                </question-type-file>
                <!-- SINGLEANSWERRADIO -->
                <question-type-singleanswer ng-if="vm.question.question_type == vm.types.SINGLEANSWERRADIO"
                                            question="vm.question"
                                            has-form-responses="vm.hasFormResponses"
                                            form-elements="vm.formElements"
                                            is-radio="true">
                </question-type-singleanswer>
                <!-- MATRIX -->
                <question-type-matrix ng-if="vm.question.question_type == vm.types.MATRIX"
                                      question="vm.question"
                                      has-form-responses="vm.hasFormResponses"
                                      form-elements="vm.formElements"
                                      matrix-type="vm.matrixType">
                </question-type-matrix>
                <!-- CURSOR -->
                <question-type-cursor ng-if="vm.question.question_type == vm.types.CURSOR"
                                      question="vm.question"
                                      has-form-responses="vm.hasFormResponses">
                </question-type-cursor>
                <!-- RANKING -->
                <question-type-ranking ng-if="vm.question.question_type == vm.types.RANKING"
                                      question="vm.question">
                </question-type-ranking>
            </div>
        `,

        controller: function ($scope) {
            const vm: IViewModel = <IViewModel> this;
        },
        link: function ($scope, $element) {
            const vm: IViewModel = $scope.vm;
            vm.types = Types;
        }
    };
});
