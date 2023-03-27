import {Directive, ng} from "entcore";
import {
    Distribution,
    Distributions,
    DistributionStatus,
    Form,
    Question, QuestionChoice,
    Response,
    Responses,
    Types
} from "../../../models";
import {ColorUtils, DateUtils} from "@common/utils";
import {GraphUtils} from "@common/utils/graph";
import {IScope} from "angular";
import {RootsConst} from "../../../core/constants/roots.const";
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";

interface IResultQuestionItemScopeProps {
    question: Question;
    form: Form;

    responses: Responses;
    distributions: Distributions;
    isGraphQuestion: boolean;
    colors: string[];
    singleAnswerResponseChart: any;
    matrixResponseChart: any;
    cursorResponseChart: any;
    rankingResponseChart: any;
    results: Map<number, Response[]>;
    hasFiles: boolean;
    nbResponses;

    Types: typeof Types;
    DistributionStatus: typeof DistributionStatus;
    DateUtils: DateUtils;
}

interface IViewModel extends ng.IController, IResultQuestionItemScopeProps {
    $onInit() : Promise<void>;
    getHtmlDescription(description: string) : string;
    syncResultsMap() : void;
    downloadFile(fileId: number) : void;
    zipAndDownload() : void;
    getWidth(nbResponses: number, divisor: number) : number;
    getAverage(responses: Response[]): number;
    getColor(choiceId: number) : string;
    formatAnswers(distribId: number) : any;
    showMoreButton() : boolean;
    loadMoreResults() : Promise<void>;
    getCustomResponse(distribution: Distribution): Response;
}

interface IResultQuestionItemScope extends IScope, IResultQuestionItemScopeProps {
    this: IViewModel;
}

class Controller implements ng.IController, IViewModel {
    question: Question;
    form: Form;

    responses: Responses;
    distributions: Distributions;
    isGraphQuestion: boolean;
    colors: string[];
    singleAnswerResponseChart: any;
    matrixResponseChart: any;
    cursorResponseChart: any;
    rankingResponseChart: any;
    results: Map<number, Response[]>;
    hasFiles: boolean;
    nbResponses;

    Types: typeof Types;
    DistributionStatus: typeof DistributionStatus;
    DateUtils: DateUtils;

    constructor(private $scope: IResultQuestionItemScope, private $sce: ng.ISCEService) {
        this.Types = Types;
        this.DistributionStatus = DistributionStatus;
        this.DateUtils = DateUtils;

        this.$scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.REFRESH_QUESTION, () => { this.$onInit(); });
    }

    $onInit = async () : Promise<void> => {
        this.isGraphQuestion = this.question.isTypeGraphQuestion();
        this.responses = new Responses();
        this.distributions = new Distributions();
        this.results = new Map();
        this.hasFiles = false;

        if (this.question.question_type != Types.FREETEXT) {
            await this.question.choices.sync(this.question.id);
            await this.responses.sync(this.question, this.question.question_type == Types.FILE);
            await this.distributions.syncByFormAndStatus(this.form.id, DistributionStatus.FINISHED, this.question.id,
                this.isGraphQuestion ? null : 0);
            this.nbResponses = new Set(this.responses.all.map((r: Response) => r.distribution_id)).size;

            if (this.isGraphQuestion) {
                if (this.question.canHaveCustomAnswers()) this.syncResultsMap();
                this.question.fillChoicesInfo(this.distributions, this.responses.all);
                this.colors = ColorUtils.generateColorList(this.question.choices.all.length);
                this.generateChart();
            }
            else {
                this.syncResultsMap();
                this.hasFiles = (this.responses.all.map((r: Response) => r.files.all) as any).flat().length > 0;
            }
        }
        this.$scope.$apply();
    };

    generateChart = () : void => {
        if (this.question.question_type == Types.SINGLEANSWER || this.question.question_type == Types.SINGLEANSWERRADIO) {
            if (this.singleAnswerResponseChart) { this.singleAnswerResponseChart.destroy(); }
            GraphUtils.generateGraphForResult(this.question, [this.singleAnswerResponseChart], null,
                null, false);
        }
        else if (this.question.question_type == Types.MATRIX) {
            if (this.matrixResponseChart) { this.matrixResponseChart.destroy(); }
            GraphUtils.generateGraphForResult(this.question, [this.matrixResponseChart], null,
                null,false);
        }
        else if (this.question.question_type == Types.CURSOR) {
            if (this.cursorResponseChart) { this.cursorResponseChart.destroy(); }
            GraphUtils.generateGraphForResult(this.question, [this.cursorResponseChart], this.responses.all,
                null, false);
        }
        else if (this.question.question_type == Types.RANKING) {
            if (this.rankingResponseChart) { this.cursorResponseChart.destroy(); }
            GraphUtils.generateGraphForResult(this.question, [this.rankingResponseChart], this.responses.all,
                null, false);
        }
    };

    getHtmlDescription = (description: string) : string => {
        return !!description ? this.$sce.trustAsHtml(description) : null;
    }

    syncResultsMap = () : void => {
        this.results = new Map();
        for (let distribution of this.distributions.all) {
            if (!this.results.get(distribution.id)) {
                this.results.set(distribution.id, this.formatAnswers(distribution.id));
            }
        }
    };

    downloadFile = (fileId: number) : void => {
        window.open(`/formulaire/responses/files/${fileId}/download`);
    };

    zipAndDownload = () : void => {
        window.open(`/formulaire/responses/${this.question.id}/files/download/zip`);
    };

    getWidth = (nbResponses: number, divisor: number) : number => {
        let width: number = nbResponses / (this.distributions.all.length > 0 ? this.distributions.all.length : 1) * divisor;
        return width < 0 ? 0 : (width > divisor ? divisor : width);
    }

    getAverage = (responses: Response[]) : number => {
        let resp: number[] = responses.map((r: Response) => Number(r.answer)).sort((a: number, b: number) => a - b);
        return (resp.reduce((a: number, b: number) => a + b, 0) / resp.length);
    }

    getColor = (choiceId: number) : string => {
        let colorIndex: number =
            this.question.choices.all.filter((c: QuestionChoice) => c.nbResponses > 0).findIndex(c => c.id === choiceId);
        return colorIndex >= 0 ? this.colors[colorIndex] : '#fff';
    };

    formatAnswers = (distribId: number) : any => {
        let results: Response[] =  this.responses.all.filter((r: Response) => r.distribution_id === distribId);
        for (let result of results) {
            if (!result.custom_answer && (result.answer == ""
                || (this.question.question_type === Types.FILE && result.files.all.length <= 0))) {
                result.answer = "-";
            }
        }
        return results;
    };

    showMoreButton = () : boolean => {
        return this.nbResponses >
            this.distributions.all.length && this.question.question_type != Types.FREETEXT && !this.isGraphQuestion;
    };

    loadMoreResults = async () : Promise<void> => {
        if (!this.isGraphQuestion) {
            await this.distributions.syncByFormAndStatus(this.form.id, DistributionStatus.FINISHED, this.question.id,
                this.distributions.all.length);
            this.syncResultsMap();
            this.$scope.$apply();
        }
    };

    getCustomResponse = (distribution: Distribution) : Response => {
        let customResponses: Response[] = this.results.get(distribution.id).filter((r: Response) => !!r.custom_answer);
        return customResponses.length == 1 ? customResponses[0] : null;
    };
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/result-question-item/result-question-item.html`,
        transclude: true,
        scope: {
            question: '=',
            form: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IResultQuestionItemScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const resultQuestionItem: Directive = ng.directive('resultQuestionItem', directive);