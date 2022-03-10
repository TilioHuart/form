import {Directive, ng} from "entcore";
import {Question, Types} from "../../models";

interface IViewModel {
    question: Question,
    types: typeof Types
}

export const questionTitle: Directive = ng.directive('questionTitle', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="question-title focusable" ng-class="{onedition: vm.reorder || !vm.hasFormResponses}" guard-root>
                <div class="twelve" ng-if="vm.question.question_type == vm.types.FREETEXT">
                    <div ng-if="!vm.question.selected">
                        <h4 ng-if="vm.question.title">[[vm.question.title]]</h4>
                        <h4 ng-if="!vm.question.title" class="empty"><i18n>formulaire.question.title.free.empty</i18n></h4>
                    </div>
                    <div ng-if="vm.question.selected">
                        <input type="text" class="ten" ng-model="vm.question.title" i18n-placeholder="formulaire.question.title.free.empty" input-guard/>
                    </div>
                </div>
                <div class="twelve" ng-if="vm.question.question_type != vm.types.FREETEXT">
                    <div ng-if="!vm.question.selected">
                        <h4 ng-if="vm.question.title">[[vm.question.title]]<span ng-if="vm.question.mandatory" style="color:red;margin-left:10px">*</span></h4>
                        <h4 ng-if="!vm.question.title" class="empty"><i18n>formulaire.question.title.empty</i18n></h4>
                    </div>
                    <div ng-if="vm.question.selected">
                        <input type="text" class="ten" ng-model="vm.question.title" i18n-placeholder="formulaire.question.title.empty" input-guard>
                    </div>
                </div>
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
