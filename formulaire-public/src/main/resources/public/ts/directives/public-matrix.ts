import {ng} from "entcore";
import {Question, QuestionChoice, Response, Responses, Types} from "@common/models";
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";
import {I18nUtils} from "@common/utils";
import {IScope} from "angular";

interface IPublicMatrixProps {
    question: Question;
    responses: Responses;
    Types: typeof Types;
    I18n: I18nUtils;
    mapChildChoicesResponseIndex: Map<Question, Map<QuestionChoice, number>>;
}

interface IViewModel extends ng.IController, IPublicMatrixProps {
    init(): Promise<void>;
    switchValue(child: Question, choice: QuestionChoice): void;
}

interface IPublicQuestionItemScope extends IScope, IPublicMatrixProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;
    responses: Responses;
    Types = Types;
    I18n = I18nUtils;
    mapChildChoicesResponseIndex: Map<Question, Map<QuestionChoice, number>>;

    constructor(private $scope: IPublicQuestionItemScope, private $sce: ng.ISCEService) {}

    $onInit = async () : Promise<void> => {
        await this.init();

        // used when previous question is same type than this one
        this.$scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.REFRESH_QUESTION, (event, data) => {
            this.responses = data.get(this.question);
            this.init();
        });
    }

    $onDestroy = async (): Promise<void> => {}

    init = async () : Promise<void> => {
        if (this.responses == null) return; // happens when previous question is not same type than this one

        this.mapChildChoicesResponseIndex = new Map();
        for (let child of this.question.children.all) {
            this.mapChildChoicesResponseIndex.set(child, new Map());
            for (let choice of this.question.choices.all) {
                let matchingResponses: Response[] = this.responses.all.filter((r:Response) => r.question_id == child.id && r.choice_id == choice.id);
                if (matchingResponses.length != 1) console.error("Be careful, 'vm.responses' has been badly implemented !!");
                this.mapChildChoicesResponseIndex.get(child).set(choice, this.responses.all.indexOf(matchingResponses[0]));
            }
        }
    }

    switchValue = (child: Question, choice: QuestionChoice) : void => {
        if (child.question_type == Types.SINGLEANSWERRADIO) {
            for (let response of this.responses.all) {
                if (response.question_id == child.id && response.choice_id != choice.id) {
                    response.selected = false;
                }
            }
        }
        this.$scope.$apply();
    }
}


function directive() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '<',
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
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IPublicQuestionItemScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const publicMatrix = ng.directive('publicMatrix', directive);