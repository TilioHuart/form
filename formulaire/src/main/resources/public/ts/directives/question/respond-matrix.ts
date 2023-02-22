import {Directive, ng} from "entcore";
import {
    Distribution,
    Question, QuestionChoice,
    Response,
    Responses,
    Types
} from "../../models";
import {I18nUtils} from "@common/utils";

interface IViewModel {
    question: Question;
    responses: Responses;
    distribution: Distribution;
    Types: typeof Types;
    I18n: I18nUtils;
    mapChildChoicesResponseIndex: Map<Question, Map<QuestionChoice, number>>;

    $onInit(): Promise<void>;
    $onChanges(changes: any): Promise<void>;
    switchValue(child: Question, choice: QuestionChoice): void;
    resetLine(child: Question): void;
}

export const respondMatrix: Directive = ng.directive('respondMatrix', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '<',
            responses: '=',
            distribution: '=',
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="question" guard-root>
                <div class="question-title">
                    <h4>[[vm.question.title]]<span ng-if="vm.question.mandatory" style="color:red;margin-left:10px">*</span></h4>
                </div>
                <div class="question-main">
                    <table class="twelve matrix-table">
                        <thead>
                            <tr>
                                <th class="two"></th>
                                <th ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']">[[choice.value]]</th>
                                <th class="one"></th>
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
                                <td><i class="i-restore md-icon dark-grey spaced-left" ng-click="vm.resetLine(child)"></i></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        `,

        controller: function ($scope) {
            const vm: IViewModel = <IViewModel> this;

            vm.$onInit = async () : Promise<void> => {
                await initRespondMatrix();
            };

            vm.$onChanges = async (changes: any) : Promise<void> => {
                vm.question = changes.question.currentValue;
                await initRespondMatrix();
            };

            const initRespondMatrix = async () : Promise<void> => {
                vm.mapChildChoicesResponseIndex = new Map();

                for (let child of vm.question.children.all) {
                    vm.mapChildChoicesResponseIndex.set(child, new Map());
                    let existingResponses: Responses = new Responses();
                    if (vm.distribution) await existingResponses.syncMine(child.id, vm.distribution.id);

                    for (let choice of vm.question.choices.all) {
                        // Get potential existing response for this choice
                        let existingMatchingResponses: Response[] = existingResponses.all.filter((r:Response) => r.choice_id == choice.id);

                        // Get default response matching this choice and child and get its index in list
                        let matchingResponses: Response[] = vm.responses.all.filter((r:Response) => r.choice_id == choice.id && r.question_id == child.id);
                        if (matchingResponses.length != 1) console.error("Be careful, 'vm.responses' has been badly implemented !!");
                        let matchingIndex = vm.responses.all.indexOf(matchingResponses[0]);

                        // If there was an existing response we use it to replace the default one
                        if (existingMatchingResponses.length == 1) {
                            vm.responses.all[matchingIndex] = existingMatchingResponses[0];
                            vm.responses.all[matchingIndex].selected = true;
                        }

                        vm.mapChildChoicesResponseIndex.get(child).set(choice, matchingIndex);
                    }
                }

                $scope.$apply();
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
            }

            vm.resetLine = (child: Question) : void => {
                vm.responses.all
                    .filter((r: Response) => r.question_id == child.id)
                    .forEach((r: Response) => r.selected = false);
            }
        }
    };
});
