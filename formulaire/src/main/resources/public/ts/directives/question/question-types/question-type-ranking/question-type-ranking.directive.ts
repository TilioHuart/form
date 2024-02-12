import {Directive, ng} from "entcore";
import {Question} from "@common/models";
import {I18nUtils} from "@common/utils";
import {Direction} from "@common/core/enums";
import {RootsConst} from "../../../../core/constants/roots.const";

interface IQuestionTypeRankingProps {
    I18n: I18nUtils;
    question: Question;
    direction: typeof Direction;
}

interface IViewModel extends ng.IController, IQuestionTypeRankingProps {
}

class Controller implements ng.IController, IViewModel {
    I18n: I18nUtils;
    question: Question;
    direction: typeof Direction;

    constructor(private $scope: IQuestionTypeRankingProps, private $sce: ng.ISCEService) {
        this.I18n = I18nUtils;
        this.direction = Direction;
    }

    $onInit = async () : Promise<void> => {

    }

    $onDestroy = async () : Promise<void> => {}

    getHtmlDescription = (description: string) : string => {
        return !!description ? this.$sce.trustAsHtml(description) : null;
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directiveQuestionTypes}/question-type-ranking/question-type-ranking.html`,
        transclude: true,
        scope: {
            question: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeRankingProps,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeRanking: Directive = ng.directive('questionTypeRanking', directive);