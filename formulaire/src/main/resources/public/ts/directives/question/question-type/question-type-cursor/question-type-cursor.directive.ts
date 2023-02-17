import {ng} from "entcore";
import {Question} from "@common/models";
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

interface IQuestionTypeFreetextScope extends IScope, IQuestionTypeCursorProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;
    hasFormResponses: boolean;

    constructor(private $scope: IQuestionTypeFreetextScope, private $sce: ng.ISCEService) {}

    $onInit = async () : Promise<void> => {
        this.question.cursor_min_val = this.question.cursor_min_val != null ? this.question.cursor_min_val : Constants.DEFAULT_CURSOR_MIN_VALUE;
        this.question.cursor_max_val = this.question.cursor_max_val != null ? this.question.cursor_max_val : Constants.DEFAULT_CURSOR_MAX_VALUE;
        this.question.cursor_step = this.question.cursor_step != null ? this.question.cursor_step : Constants.DEFAULT_CURSOR_STEP;
    }

    $onDestroy = async () : Promise<void> => {}

    getHtmlDescription = (description: string) : string => {
        return !!description ? this.$sce.trustAsHtml(description) : null;
    }

    onChangeStep = (newStep: number) : void => {
        if (this.question.cursor_max_val - newStep < this.question.cursor_min_val) {
            this.question.cursor_max_val = this.question.cursor_min_val + newStep;
        }
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question-type-cursor/question-type-cursor.html`,
        transclude: true,
        scope: {
            question: '=',
            hasFormResponses: '=',
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeFreetextScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeCursor = ng.directive('questionTypeCursor', directive);