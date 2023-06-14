import {ng} from "entcore";
import {Form, Question} from "@common/models";
import {IScope} from "angular";

interface IQuestionTypeFreetextProps {
    question: Question;
    form: Form;
}

interface IViewModel extends ng.IController, IQuestionTypeFreetextProps {
    getHtmlDescription(description: string) : string;
}

interface IQuestionTypeFreetextScope extends IScope, IQuestionTypeFreetextProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;
    form: Form;

    constructor(private $scope: IQuestionTypeFreetextScope, private $sce: ng.ISCEService) {}

    $onInit = async (): Promise<void> => {}

    $onDestroy = async (): Promise<void> => {}

    getHtmlDescription = (description: string): string => {
        return !!description ? this.$sce.trustAsHtml(description) : null;
    }

}

function directive() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            form: '<',
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div>
                <div ng-if="!vm.question.selected">
                    <div ng-if="vm.question.statement" data-ng-bind-html="vm.getHtmlDescription(vm.question.statement)"></div>
                    <textarea disabled ng-if="!vm.question.statement" i18n-placeholder="formulaire.question.type.FREETEXT"></textarea>
                </div>
                <div ng-if="vm.question.selected">
                    <editor ng-model="vm.question.statement" visibility="vm.form.is_public ? 'public' : ''" input-guard></editor>
                </div>
            </div>
        `,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeFreetextScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeFreetext = ng.directive('questionTypeFreetext', directive);