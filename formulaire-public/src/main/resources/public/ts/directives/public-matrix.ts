import {Directive, ng} from "entcore";
import {Question, Responses, Types} from "@common/models";
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";
import {I18nUtils} from "@common/utils";

interface IViewModel {
    question: Question;
    responses: Responses;
    Types: typeof Types;
    I18n: I18nUtils;

    $onInit() : Promise<void>;
}

export const publicMatrix: Directive = ng.directive('publicMatrix', () => {
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
                    <table class="twelve matrix-table">
                        <thead>
                            <tr>
                                <th class="two"></th>
                                <th ng-repeat="choice in vm.question.choices.all | orderBy:'id'">[[choice.value]]</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr ng-repeat="child in vm.question.children.all" ng-init="childIndex = $index">
                                <td>[[child.title]]</td>
                                <td ng-repeat ="choice in vm.question.choices.all | orderBy:'id'">
                                    <label>
                                        <input type="radio" ng-model="vm.responses.all[childIndex].choice_id" ng-value="[[choice.id]]" input-guard>
                                    </label>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        `,

        controller: function ($scope) {
            const vm: IViewModel = <IViewModel> this;

            vm.$onInit = async () : Promise<void> => {
                for (let child of vm.question.children.all) {
                    let childIndex: number = vm.question.children.all.map((q: Question) => q.id).indexOf(child.id);
                    if (!vm.responses.all[childIndex].question_id) { vm.responses.all[childIndex].question_id = child.id; }
                }
            };
        },
        link: function ($scope, $element) {
            const vm: IViewModel = $scope.vm;
            vm.Types = Types;
            vm.I18n = I18nUtils;

            $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.REFRESH_QUESTION, () => { vm.$onInit(); });
        }
    };
});
