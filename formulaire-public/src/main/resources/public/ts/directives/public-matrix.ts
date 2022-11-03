import {Directive, ng} from "entcore";
import {Question, QuestionChoice, Response, Responses, Types} from "@common/models";
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";
import {I18nUtils} from "@common/utils";

interface IViewModel {
    question: Question;
    responses: Responses;
    Types: typeof Types;
    I18n: I18nUtils;
    mapChildChoicesResponseIndex: Map<Question, Map<QuestionChoice, number>>;

    $onInit() : Promise<void>;
    switchValue(child: Question, choice: QuestionChoice): void;
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
                                <th ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']">[[choice.value]]</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr ng-repeat="child in vm.question.children.all | orderBy:matrix_position" ng-init="childIndex = $index">
                                <td>[[child.title]]</td>
                                <td ng-repeat ="choice in vm.question.choices.all | orderBy:['position', 'id']">
                                    <label>
                                        <input type="radio" name="child-[[child.id]]" ng-change="vm.switchValue(child, choice)" ng-value="true" input-guard
                                               ng-model="vm.responses.all[vm.mapChildChoicesResponseIndex.get(child).get(choice)].selected">
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
                await initPublicMatrix();
            };

            const initPublicMatrix = async () : Promise<void> => {
                vm.mapChildChoicesResponseIndex = new Map();
                for (let child of vm.question.children.all) {
                    vm.mapChildChoicesResponseIndex.set(child, new Map());
                    for (let choice of vm.question.choices.all) {
                        let matchingResponses: Response[] = vm.responses.all.filter((r:Response) => r.question_id == child.id && r.choice_id == choice.id);
                        if (matchingResponses.length != 1) console.error("Be careful, 'vm.responses' has been badly implemented !!");
                        vm.mapChildChoicesResponseIndex.get(child).set(choice, vm.responses.all.indexOf(matchingResponses[0]));
                    }
                }
            }
        },
        link: function ($scope, $element) {
            const vm: IViewModel = $scope.vm;
            vm.Types = Types;
            vm.I18n = I18nUtils;

            vm.switchValue = (child: Question, choice: QuestionChoice) : void => {
                if (child.question_type == Types.SINGLEANSWERRADIO) {
                    for (let response of vm.responses.all) {
                        if (response.question_id == child.id && response.choice_id != choice.id) {
                            response.selected = false;
                        }
                    }
                }
                $scope.$apply();
            }

            $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.REFRESH_QUESTION, () => { vm.$onInit(); });
        }
    };
});
