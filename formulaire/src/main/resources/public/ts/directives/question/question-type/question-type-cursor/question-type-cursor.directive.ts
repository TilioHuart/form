import {ng} from "entcore";
import {Question, Types} from "@common/models";
import {IScope} from "angular";
import {RootsConst} from "../../../../core/constants/roots.const";
import {Constants} from "@common/core/constants";

interface IQuestionTypeCursorProps {
    question: Question;
    hasFormResponses: boolean;
}

interface IViewModel extends ng.IController, IQuestionTypeCursorProps {
    onChangeStep(newStep: number): void;
}

interface IQuestionTypeCursorScope extends IScope, IQuestionTypeCursorProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;
    hasFormResponses: boolean;

    constructor(private $scope: IQuestionTypeCursorScope, private $sce: ng.ISCEService) {}

    $onInit = async () : Promise<void> => {
        if (this.question.question_type != Types.CURSOR || !this.question.specific_fields) return;
        this.question.specific_fields.cursor_min_val = this.question.specific_fields.cursor_min_val != null ? this.question.specific_fields.cursor_min_val : Constants.DEFAULT_CURSOR_MIN_VALUE;
        this.question.specific_fields.cursor_max_val = this.question.specific_fields.cursor_max_val != null ? this.question.specific_fields.cursor_max_val : Constants.DEFAULT_CURSOR_MAX_VALUE;
        this.question.specific_fields.cursor_step = this.question.specific_fields.cursor_step != null ? this.question.specific_fields.cursor_step : Constants.DEFAULT_CURSOR_STEP;
    }

    $onDestroy = async () : Promise<void> => {}

    getHtmlDescription = (description: string) : string => {
        return !!description ? this.$sce.trustAsHtml(description) : null;
    }

    onChangeStep = (newStep: number) : void => {
        if (this.question.question_type != Types.CURSOR || !this.question.specific_fields) return;
        if (this.question.specific_fields.cursor_max_val - newStep < this.question.specific_fields.cursor_min_val) {
            this.question.specific_fields.cursor_max_val = this.question.specific_fields.cursor_min_val + newStep;
        }
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/question-type/question-type-cursor/question-type-cursor.html`,
        transclude: true,
        scope: {
            question: '=',
            hasFormResponses: '=',
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeCursorScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeCursor = ng.directive('questionTypeCursor', directive);