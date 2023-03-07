import {Directive, ng} from "entcore";
import {
    Distributions,
    DistributionStatus,
    Form,
    Question, QuestionChoice,
    Response,
    Responses,
    Types
} from "../../models";
import {ColorUtils, DateUtils, UtilsUtils} from "@common/utils";
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";
import {GraphUtils} from "@common/utils/graph";

interface IViewModel {
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

    $onInit() : Promise<void>;
    getHtmlDescription(description: string) : string;
    syncResultsMap() : void;
    downloadFile(fileId: number) : void;
    zipAndDownload() : void;
    getWidth(nbResponses: number, divisor: number) : number;
    getColor(choiceId: number) : string;
    formatAnswers(distribId: number) : any;
    showMoreButton() : boolean;
    loadMoreResults() : Promise<void>;
}

export const resultQuestionItem: Directive = ng.directive('resultQuestionItem', ['$sce', ($sce) => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            form: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            
        <div class="question">
            <!-- Question title -->
            <div class="title">
                <h4 ng-if="vm.question.question_type == vm.Types.FREETEXT">[[vm.question.title]]</h4>
                <h4 ng-if="vm.question.question_type != vm.Types.FREETEXT && vm.nbResponses > 1">
                    [[vm.question.title]] ([[vm.nbResponses]] <i18n>formulaire.responses</i18n>)<span ng-if="vm.question.mandatory" style="color:red;margin-left:10px">*</span>
                </h4>
                <h4 ng-if="vm.question.question_type != vm.Types.FREETEXT && vm.nbResponses <= 1">
                    [[vm.question.title]] ([[vm.nbResponses]] <i18n>formulaire.response</i18n>)<span ng-if="vm.question.mandatory" style="color:red;margin-left:10px">*</span>
                </h4>
                <button class="cell" ng-click="vm.zipAndDownload()" ng-if="vm.question.question_type == vm.Types.FILE" ng-disabled="!vm.hasFiles">
                    <i18n>formulaire.form.download.all.files</i18n>
                </button>
            </div>

            <!-- List of results FREETEXT -->
            <div ng-if="vm.question.question_type == vm.Types.FREETEXT" class="freetext" data-ng-bind-html="vm.getHtmlDescription(vm.question.statement)"></div>

            <!-- List of results SINGLEANSWER, MULTIPLEANSWER, SINGLEANSWERRADIO -->
            <div ng-if="vm.question.canHaveCustomAnswers()">
                <div class="choices">
                    <!-- Data -->
                    <div class="twelve-mobile" ng-class="vm.question.question_type == vm.Types.MULTIPLEANSWER ? 'twelve' : 'five'">
                        <div ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']" class="choice">
                            <!-- Data for MULTIPLEANSWER -->
                            <div class="infos twelve-mobile" ng-class="vm.question.question_type == vm.Types.MULTIPLEANSWER  ? 'five' : 'twelve'">
                                <div class="choice-value eight twelve-mobile ellipsis">
                                    <span ng-if="($index+1) != vm.question.choices.all.length">[[$index + 1]]. </span>[[choice.value]]
                                </div>
                                <div class="four twelve-mobile ellipsis bold">
                                    [[choice.nbResponses]]
                                    <i18n ng-if="vm.question.question_type == vm.Types.MULTIPLEANSWER && choice.nbResponses <= 1">formulaire.vote</i18n>
                                    <i18n ng-if="vm.question.question_type == vm.Types.MULTIPLEANSWER && choice.nbResponses > 1">formulaire.votes</i18n>
                                    <i18n ng-if="vm.question.question_type != vm.Types.MULTIPLEANSWER && choice.nbResponses <= 1">formulaire.response</i18n>
                                    <i18n ng-if="vm.question.question_type != vm.Types.MULTIPLEANSWER && choice.nbResponses > 1">formulaire.responses</i18n>
                                    ([[vm.getWidth(choice.nbResponses, 100).toFixed(2)]]%)
                                </div>
                            </div>
                            <!-- Graph for MULTIPLEANSWER -->
                            <div ng-if="vm.question.question_type == vm.Types.MULTIPLEANSWER" class="seven zero-mobile">
                                <div class="graph-bar"
                                     ng-style="{width: (vm.getWidth(choice.nbResponses, 95) + '%'), 'background-color': vm.getColor(choice.id)}">
                                </div>
                            </div>
                        </div>
                    </div>
    
                    <!-- Graph for SINGLEANSWER, SINGLEANSWERRADIO -->
                    <div class="graph-camembert seven zero-mobile"
                         ng-if="vm.question.question_type == vm.Types.SINGLEANSWER
                                || vm.question.question_type == vm.Types.SINGLEANSWERRADIO">
                        <div class="eight">
                            <div id="chart-[[vm.question.id]]"></div>
                        </div>
                    </div>
                </div>
                
                <!-- Custom answers -->
                <div class="custom-answers" ng-if="vm.question.hasCustomChoice()">
                    <div class="custom-answers-title"><i18n>formulaire.results.custom.answers</i18n></div>
                    <div ng-repeat="distrib in vm.distributions.all | orderBy:'date_response':true" class="distrib"
                         ng-if="vm.results.get(distrib.id).length > 0 && vm.question.hasCustomChoice()">
                        <div class="infos four twelve-mobile">
                            <div class="four twelve-mobile">[[vm.DateUtils.displayDate(distrib.date_response)]]</div>
                            <div class="eight twelve-mobile ellipsis" ng-if="!vm.form.anonymous">[[distrib.responder_name]]</div>
                        </div>
                        <div class="eight twelve-mobile results">
                            <div ng-repeat="result in vm.results.get(distrib.id) | filter:{custom_answer:'!!'}"
                                 ng-class="{'notLast' : !$last}">
                                <div>[[result.custom_answer]]</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Graph for MATRIX -->
            <div class="graph-histogram twelve" ng-if="vm.question.question_type == vm.Types.MATRIX">
                <div class="eight" style="height: 400px">
                    <div id="chart-[[vm.question.id]]"></div>
                </div>
            </div>

            <!-- List of results SHORTANSWER, LONGANSWER, DATE, TIME, FILE -->
            <div ng-if="!vm.question.isTypeGraphQuestion() && vm.question.question_type != vm.Types.FREETEXT">
                <div ng-repeat="distrib in vm.distributions.all | orderBy:'date_response':true" class="distrib" ng-if="vm.results.get(distrib.id).length > 0">
                    <div class="infos four twelve-mobile">
                        <div class="four twelve-mobile">[[vm.DateUtils.displayDate(distrib.date_response)]]</div>
                        <div class="eight twelve-mobile ellipsis" ng-if="!vm.form.anonymous">[[distrib.responder_name]]</div>
                    </div>
                    <div class="eight twelve-mobile results">
                        <div ng-repeat="result in vm.results.get(distrib.id)"
                             ng-class="{'notLast' : !$last}">
                            <div ng-if="vm.question.question_type == vm.Types.SHORTANSWER ||
                                        vm.question.question_type == vm.Types.LONGANSWER ||
                                        vm.question.question_type == vm.Types.DATE ||
                                        vm.question.question_type == vm.Types.TIME ||
                                        (vm.question.question_type == vm.Types.FILE && result.files.all.length <= 0)"
                                 data-ng-bind-html="vm.getHtmlDescription(result.answer)"></div>
                            <div ng-repeat="file in result.files.all" ng-if="vm.question.question_type == vm.Types.FILE">
                                <a ng-if="file.id" ng-click="vm.downloadFile(file.id)">
                                    <i class="i-download lg-icon spaced-right"></i> [[file.filename]]
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Graph for CURSOR -->
            <div ng-if="vm.question.question_type == vm.Types.CURSOR">
                 <div>
                    <div id="chart-[[vm.question.id]]"></div>
                </div>
            </div>
            
               <!-- Graph for RANKING -->
            <div ng-if="vm.question.question_type == vm.Types.RANKING">
                 <div class="eight" style="height: 400px">
                    <div id="chart-[[vm.question.id]]"></div>
                </div>
            </div>

            <!-- See more button -->
            <div ng-if="vm.showMoreButton()" style="margin: 1%;">
                <a ng-click="vm.loadMoreResults()">
                    <i18n>formulaire.seeMore</i18n>
                </a>
            </div>
        </div>
        `,

        controller: function ($scope) {
            const vm: IViewModel = <IViewModel> this;

            vm.$onInit = async () : Promise<void> => {
                vm.isGraphQuestion = vm.question.isTypeGraphQuestion();
                vm.responses = new Responses();
                vm.distributions = new Distributions();
                vm.results = new Map();
                vm.hasFiles = false;

                if (vm.question.question_type != Types.FREETEXT) {
                    await vm.question.choices.sync(vm.question.id);
                    await vm.responses.sync(vm.question, vm.question.question_type == Types.FILE);
                    await vm.distributions.syncByFormAndStatus(vm.form.id, DistributionStatus.FINISHED, vm.question.id, vm.isGraphQuestion ? null : 0);
                    vm.nbResponses = new Set(vm.responses.all.map((r: Response) => r.distribution_id)).size;

                    if (vm.isGraphQuestion) {
                        if (vm.question.canHaveCustomAnswers()) vm.syncResultsMap();
                        vm.question.fillChoicesInfo(vm.distributions, vm.responses.all);
                        vm.colors = ColorUtils.generateColorList(vm.question.choices.all.length);
                        generateChart();
                    }
                    else {
                        vm.syncResultsMap();
                        vm.hasFiles = (vm.responses.all.map((r: Response) => r.files.all) as any).flat().length > 0;
                    }
                }
                UtilsUtils.safeApply($scope);
            };

            const generateChart = () : void => {
                if (vm.question.question_type == Types.SINGLEANSWER || vm.question.question_type == Types.SINGLEANSWERRADIO) {
                    if (vm.singleAnswerResponseChart) { vm.singleAnswerResponseChart.destroy(); }
                    GraphUtils.generateGraphForResult(vm.question, [vm.singleAnswerResponseChart], null,
                        null, false);
                }
                else if (vm.question.question_type == Types.MATRIX) {
                    if (vm.matrixResponseChart) { vm.matrixResponseChart.destroy(); }
                    GraphUtils.generateGraphForResult(vm.question, [vm.matrixResponseChart], null,
                        null,false);
                }
                else if (vm.question.question_type == Types.CURSOR) {
                    if (vm.cursorResponseChart) { vm.cursorResponseChart.destroy(); }
                    GraphUtils.generateGraphForResult(vm.question, [vm.cursorResponseChart], vm.responses.all,
                        null, false);
                }
                else if (vm.question.question_type == Types.RANKING) {
                    if (vm.rankingResponseChart) { vm.cursorResponseChart.destroy(); }
                    GraphUtils.generateGraphForResult(vm.question, [vm.rankingResponseChart], vm.responses.all,
                        null, false);
                }
            };
        },
        link: function ($scope) {
            const vm: IViewModel = $scope.vm;
            vm.Types = Types;
            vm.DistributionStatus = DistributionStatus;
            vm.DateUtils = DateUtils;

            vm.getHtmlDescription = (description: string) : string => {
                return !!description ? $sce.trustAsHtml(description) : null;
            }

            vm.syncResultsMap = () : void => {
                vm.results = new Map();
                for (let distribution of vm.distributions.all) {
                    if (!vm.results.get(distribution.id)) {
                        vm.results.set(distribution.id, vm.formatAnswers(distribution.id));
                    }
                }
            };

            vm.downloadFile = (fileId: number) : void => {
                window.open(`/formulaire/responses/files/${fileId}/download`);
            };

            vm.zipAndDownload = () : void => {
                window.open(`/formulaire/responses/${vm.question.id}/files/download/zip`);
            };

            vm.getWidth = (nbResponses: number, divisor: number) : number => {
                let width: number = nbResponses / (vm.distributions.all.length > 0 ? vm.distributions.all.length : 1) * divisor;
                return width < 0 ? 0 : (width > divisor ? divisor : width);
            }

            vm.getColor = (choiceId: number) : string => {
                let colorIndex: number = vm.question.choices.all.filter((c: QuestionChoice) => c.nbResponses > 0).findIndex(c => c.id === choiceId);
                return colorIndex >= 0 ? vm.colors[colorIndex] : '#fff';
            };

            vm.formatAnswers = (distribId: number) : any => {
                let results: Response[] =  vm.responses.all.filter((r: Response) => r.distribution_id === distribId);
                for (let result of results) {
                    if (!result.custom_answer && (result.answer == ""
                        || (vm.question.question_type === Types.FILE && result.files.all.length <= 0))) {
                        result.answer = "-";
                    }
                }
                return results;
            };

            vm.showMoreButton = () : boolean => {
                return vm.nbResponses > vm.distributions.all.length && vm.question.question_type != Types.FREETEXT && !vm.isGraphQuestion;
            };

            vm.loadMoreResults = async () : Promise<void> => {
                if (!vm.isGraphQuestion) {
                    await vm.distributions.syncByFormAndStatus(vm.form.id, DistributionStatus.FINISHED, vm.question.id, vm.distributions.all.length);
                    vm.syncResultsMap();
                    UtilsUtils.safeApply($scope);
                }
            };

            $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.REFRESH_QUESTION, () => { vm.$onInit(); });
        }
    };
}]);
