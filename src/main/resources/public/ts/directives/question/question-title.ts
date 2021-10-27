import {Directive, idiom, ng} from "entcore";
import {Question, Types} from "../../models";

interface IViewModel {
    question: Question,
    types: typeof Types,

    displayTypeName(typeName: string): string
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
        replace: true,
        template: `
            <div class="question-title">
                <div class="twelve" ng-if="vm.question.question_type == vm.types.FREETEXT">
                    <div ng-if="!vm.question.selected">
                        <h4 ng-if="!!vm.question.title">[[vm.question.title]]</h4>
                        <h4 ng-if="!!!vm.question.title" class="empty"><i18n>formulaire.question.title.free.empty</i18n></h4>
                    </div>
                    <div ng-if="vm.question.selected">
                        <input type="text" class="eight" ng-model="vm.question.title" i18n-placeholder="formulaire.question.title.free.empty"/>
                    </div>
                </div>
                <div class="twelve" ng-if="vm.question.question_type != vm.types.FREETEXT">
                    <div ng-if="!vm.question.selected">
                        <h4 ng-if="!!vm.question.title">[[vm.question.title]]<span ng-if="vm.question.mandatory" style="color:red;margin-left:10px">*</span></h4>
                        <h4 ng-if="!!!vm.question.title" class="empty"><i18n>formulaire.question.title.empty</i18n></h4>
                    </div>
                    <div ng-if="vm.question.selected">
                        <input type="text" class="eight" ng-model="vm.question.title" i18n-placeholder="formulaire.question.title.empty"/>
                    </div>
                </div>
                <div ng-if="vm.question.selected" ng-show="false">
                    <select ng-model="vm.question.question_type" style="height: 24px;">
                        <option ng-repeat="type in questionTypes.all" ng-value="type.code"
                                ng-selected="type.code === vm.question.question_type">
                            [[vm.displayTypeName(type.name)]]
                        </option>
                    </select>
                </div>
            </div>
        `,

        controller: async ($scope) => {
            const vm: IViewModel = <IViewModel> this;
        },
        link: ($scope, $element) => {
            const vm: IViewModel = $scope.vm;
            vm.types = Types;

            vm.displayTypeName = (typeInfo: string) : string => {
                return idiom.translate('formulaire.question.type.' + typeInfo);
            };
        }
    };
});
