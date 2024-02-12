import {Directive, ng} from "entcore";
import {
    Distribution,
    Question, QuestionChoice,
    Response,
    Responses,
    Types
} from "../../../models";
import {I18nUtils, UtilsUtils} from "@common/utils";
import {RootsConst} from "../../../core/constants/roots.const";
import {IScope} from "angular";

interface IRespondMatrixProps {
    question: Question;
    responses: Responses;
    distribution: Distribution;
}

interface IViewModel extends ng.IController, IRespondMatrixProps {
    mapChildChoicesResponseIndex: Map<Question, Map<QuestionChoice, number>>;
    types: typeof Types;
    i18n: I18nUtils;

    switchValue(child: Question, choice: QuestionChoice): void;
    resetLine(child: Question): void;
}

interface IRespondMatrixScope extends IScope, IRespondMatrixProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;
    responses: Responses;
    distribution: Distribution;
    mapChildChoicesResponseIndex: Map<Question, Map<QuestionChoice, number>>;
    types: typeof Types;
    i18n: I18nUtils;

    constructor(private $scope: IRespondMatrixScope, private $sce: ng.ISCEService) {
        this.types = Types;
        this.i18n = I18nUtils;
    }

    $onInit = async () : Promise<void> => {
        await this.initRespondMatrix();
    }

    $onChanges = async (changes: any) : Promise<void> => {
        this.question = changes.question.currentValue;
        await this.initRespondMatrix();
    }

    $onDestroy = async () : Promise<void> => {}

    initRespondMatrix = async () : Promise<void> => {
        this.mapChildChoicesResponseIndex = new Map();

        for (let child of this.question.children.all) {
            this.mapChildChoicesResponseIndex.set(child, new Map());
            let existingResponses: Responses = new Responses();
            existingResponses.all = this.responses.filter((response:Response) => response.selected);

            for (let choice of this.question.choices.all) {
                // Get default response matching this choice and child and get its index in list
                let matchingResponses: Response[] = this.responses.all.filter((r:Response) => r.choice_id == choice.id && r.question_id == child.id);
                if (matchingResponses.length != 1) console.error("Be careful, 'this.responses' has been badly implemented !!");
                let matchingIndex = this.responses.all.indexOf(matchingResponses[0]);

                this.mapChildChoicesResponseIndex.get(child).set(choice, matchingIndex);
            }
        }

        UtilsUtils.safeApply(this.$scope);
    }

    switchValue = (child: Question, choice: QuestionChoice) : void => {
        if (child.question_type == this.types.SINGLEANSWERRADIO) {
            for (let response of this.responses.all) {
                if (response.question_id == child.id && response.choice_id != choice.id) {
                    response.selected = false;
                }
            }
        }
    }

    resetLine = (child: Question) : void => {
        this.responses.all
            .filter((r: Response) => r.question_id == child.id)
            .forEach((r: Response) => r.selected = false);
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/respond-matrix/respond-matrix.html`,
        transclude: true,
        scope: {
            question: '<',
            responses: '=',
            distribution: '=',
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IRespondMatrixScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const respondMatrix: Directive = ng.directive('respondMatrix', directive);
