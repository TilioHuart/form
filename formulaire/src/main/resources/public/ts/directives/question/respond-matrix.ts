import {Directive, ng} from "entcore";
import {
    Distribution,
    Question,
    QuestionChoices,
    Response,
    Responses,
    Types
} from "../../models";
import {responseService} from "../../services";
import {Mix} from "entcore-toolkit";
import {I18nUtils} from "@common/utils";

interface IViewModel {
    question: Question;
    responses: Responses;
    distribution: Distribution;
    Types: typeof Types;
    I18n: I18nUtils;

    $onInit() : Promise<void>;
}

export const respondMatrix: Directive = ng.directive('respondMatrix', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
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
                vm.question.choices = new QuestionChoices();
                await vm.question.choices.sync(vm.question.id);

                if (vm.distribution) {
                    for (let child of vm.question.children.all) {
                        vm.responses.all.push(new Response());
                        let childIndex: number = vm.question.children.all.map((q: Question) => q.id).indexOf(child.id);

                        let responses: Response[] = await responseService.listMineByDistribution(child.id, vm.distribution.id);
                        if (responses.length > 0) {
                            vm.responses.all[childIndex] = Mix.castAs(Response, responses[0]);
                        }
                        if (!vm.responses.all[childIndex].question_id) { vm.responses.all[childIndex].question_id = child.id; }
                        if (!vm.responses.all[childIndex].distribution_id) { vm.responses.all[childIndex].distribution_id = vm.distribution.id; }
                    }
                }

                $scope.$apply();
            };
        },
        link: function ($scope, $element) {
            const vm: IViewModel = $scope.vm;
            vm.Types = Types;
            vm.I18n = I18nUtils;
        }
    };
});
