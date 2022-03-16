import {Directive, ng} from "entcore";
import {FormElement, FormElements, Question, QuestionChoice, Section} from "../../../models";
import {questionChoiceService} from "../../../services";
import {I18nUtils} from "../../../utils";

interface IViewModel {
    question: Question;
    hasFormResponses: boolean;
    formElements: FormElements;
    I18n: I18nUtils;

    createNewChoice(): void;
    deleteChoice(index: number): Promise<void>;
    isSectionsAfter(formElement: FormElement): boolean;
}

export const questionTypeSingleanswerRadio: Directive = ng.directive('questionTypeSingleanswerRadio', () => {

    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            hasFormResponses: '=',
            formElements: '<'
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="twelve">
                <div class="choice" ng-repeat="choice in vm.question.choices.all | orderBy:'id'">
                    <label for="radio-[[choice.id]]" ng-class="vm.question.conditional ? 'five four-mobile' : 'nine'">
                        <input type="radio" id="radio-[[choice.id]]" disabled>
                        <span style="cursor: default"></span>
                        <input type="text" ng-model="choice.value" ng-if="!vm.question.selected" disabled
                                ng-class="vm.question.conditional ? 'eleven eight-mobile' : 'eleven ten-mobile'" placeholder="Choix [[$index + 1]]">
                        <input type="text" ng-model="choice.value" ng-if="vm.question.selected" input-guard
                                ng-class="vm.question.conditional ? 'eleven eight-mobile' : 'eleven ten-mobile'" placeholder="Choix [[$index + 1]]">
                    </label>
                    <i class="i-cancel lg-icon dontSave" ng-click="vm.deleteChoice($index)" ng-if="vm.question.selected && !vm.hasFormResponses"></i>
                    <select class="five" ng-if="vm.question.conditional" ng-model="choice.next_section_id" ng-disabled="!vm.question.selected" input-guard>
                        <option ng-repeat="section in vm.formElements.all | filter:vm.isSectionsAfter" ng-value="section.id">
                            [[vm.I18n.translate('formulaire.access.section') + section.title]]
                        </option>
                        <option ng-value="null" ng-selected="true">[[vm.I18n.translate('formulaire.access.recap')]]</option>
                    </select>
                </div>
                <div style="display: flex; justify-content: center;" ng-if="vm.question.selected && !vm.hasFormResponses">
                    <i class="i-plus-circle lg-icon" ng-click="vm.createNewChoice()"></i>
                </div>
            </div>
        `,

        controller: async ($scope) => {
            const vm: IViewModel = <IViewModel> this;
        },
        link: ($scope, $element) => {
            const vm: IViewModel = $scope.vm;
            vm.I18n = I18nUtils;

            vm.createNewChoice = () : void => {
                vm.question.choices.all.push(new QuestionChoice(vm.question.id));
                $scope.$apply();
            };

            vm.deleteChoice = async (index: number) : Promise<void> => {
                if (vm.question.choices.all[index].id) {
                    await questionChoiceService.delete(vm.question.choices.all[index].id);
                }
                vm.question.choices.all.splice(index,1);
                $scope.$apply();
            };

            vm.isSectionsAfter = (formElement: FormElement) : boolean => {
                if (formElement instanceof Question) {
                    return false;
                }
                else if (formElement instanceof Section) {
                    let position = vm.question.position ? vm.question.position : vm.formElements.all.filter(e => e.id === vm.question.section_id)[0].position;
                    return formElement.position > position;
                }
            };
        }
    };
});
