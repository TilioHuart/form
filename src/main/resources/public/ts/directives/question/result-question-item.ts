import {Directive, idiom, ng} from "entcore";
import {
    Distributions,
    DistributionStatus,
    Form,
    Question,
    Responses,
    Types
} from "../../models";
import {ColorUtils, DateUtils, UtilsUtils} from "../../utils";
import * as ApexCharts from 'apexcharts';
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "../../core/enums";

interface IViewModel {
    question: Question;
    form: Form;
    paletteColors: string[];

    responses: Responses;
    distributions: Distributions;
    isGraphQuestion: boolean;
    colors: string[];
    singleAnswerResponseChart: any;
    results: Map<number, Response[]>;
    hasFiles: boolean;
    nbResponses;

    Types: typeof Types;
    DistributionStatus: typeof DistributionStatus;
    DateUtils: DateUtils;

    $onInit() : Promise<void>;
    syncResultsMap() : void;
    downloadFile(fileId: number) : void;
    zipAndDownload() : void;
    getWidth(nbResponses: number, divisor: number) : number;
    getColor(choiceId: number) : string;
    formatAnswers(distribId: number) : any;
    showMoreButton() : boolean;
    loadMoreResults() : Promise<void>;
}

export const resultQuestionItem: Directive = ng.directive('resultQuestionItem', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            form: '=',
            paletteColors: '='
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
            <div ng-if="vm.question.question_type == vm.Types.FREETEXT" class="freetext" bind-html="vm.question.statement"></div>

            <!-- List of results SINGLEANSWER, MULTIPLEANSWER, SINGLEANSWERRADIO -->
            <div class="choices" ng-if="vm.question.question_type == vm.Types.SINGLEANSWER ||
                                        vm.question.question_type == vm.Types.MULTIPLEANSWER ||
                                        vm.question.question_type == vm.Types.SINGLEANSWERRADIO">
                
                <!-- Data and graph for MULTIPLEANSWER -->
                <div class="twelve-mobile" ng-class="vm.question.question_type == vm.Types.MULTIPLEANSWER ? 'twelve' : 'five'">
                    <div ng-repeat="choice in vm.question.choices.all" class="choice">
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
                <div class="graph-camembert seven zero-mobile" ng-if="vm.question.question_type == vm.Types.SINGLEANSWER || vm.question.question_type == vm.Types.SINGLEANSWERRADIO">
                    <div class="eight">
                        <div id="chart-[[vm.question.id]]"></div>
                    </div>
                </div>
            </div>

            <!-- List of results SHORTANSWER, LONGANSWER, DATE, TIME, FILE -->
            <div ng-if="vm.question.question_type != vm.Types.SINGLEANSWER &&
                        vm.question.question_type != vm.Types.MULTIPLEANSWER &&
                        vm.question.question_type != vm.Types.SINGLEANSWERRADIO &&
                        vm.question.question_type != vm.Types.FREETEXT">
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
                                 bind-html="result.answer"></div>
                            <div ng-repeat="file in result.files.all" ng-if="vm.question.question_type == vm.Types.FILE">
                                <a ng-if="file.id" ng-click="vm.downloadFile(file.id)">
                                    <i class="i-download lg-icon spaced-right"></i> [[file.filename]]
                                </a>
                            </div>
                        </div>
                    </div>
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
                vm.isGraphQuestion = vm.question.question_type == Types.SINGLEANSWER || vm.question.question_type == Types.MULTIPLEANSWER || vm.question.question_type == Types.SINGLEANSWERRADIO;
                vm.responses = new Responses();
                vm.distributions = new Distributions();
                vm.results = new Map();
                vm.hasFiles = false;

                if (vm.question.question_type != Types.FREETEXT) {
                    await vm.question.choices.sync(vm.question.id);
                    await vm.responses.sync(vm.question, vm.question.question_type == Types.FILE, vm.isGraphQuestion ? null : 0);
                    await vm.distributions.syncByFormAndStatus(vm.form.id, DistributionStatus.FINISHED, vm.isGraphQuestion ? null : 0);
                    vm.nbResponses = new Set(vm.responses.all.map(r => r.distribution_id)).size;

                    if (vm.isGraphQuestion) {
                        vm.question.fillChoicesInfo(vm.distributions, vm.responses.all);
                        let choices = vm.question.choices.all.filter(c => c.nbResponses > 0);
                        vm.colors = ColorUtils.interpolateColors(vm.paletteColors, choices.length);

                        // Init charts
                        if (vm.question.question_type == Types.SINGLEANSWER || vm.question.question_type == Types.SINGLEANSWERRADIO) {
                            initSingleAnswerChart();
                        }
                    }
                    else {
                        vm.syncResultsMap();
                        vm.hasFiles = (vm.responses.all.map(r => r.files.all) as any).flat().length > 0;
                    }
                }
                UtilsUtils.safeApply($scope);
            };

            const initSingleAnswerChart = () : void => {
                let choices = vm.question.choices.all.filter(c => c.nbResponses > 0);

                let series = [];
                let labels = [];
                let i18nValue = idiom.translate('formulaire.response');
                i18nValue = i18nValue.charAt(0).toUpperCase() + i18nValue.slice(1);

                for (let choice of choices) {
                    series.push(choice.nbResponses); // Fill data
                    let i = vm.question.choices.all.indexOf(choice) + 1;
                    !choice.id ? labels.push(idiom.translate('formulaire.response.empty')) : labels.push(i18nValue + " " + i); // Fill labels
                }

                // Generate options with labels and colors
                let baseHeight = 40 * vm.question.choices.all.length;
                let options = {
                    chart: {
                        type: 'pie',
                        height: baseHeight < 200 ? 200 : (baseHeight > 500 ? 500 : baseHeight)
                    },
                    colors: vm.colors,
                    labels: labels
                }

                // Generate chart with options and data
                if (vm.singleAnswerResponseChart) { vm.singleAnswerResponseChart.destroy(); }

                let newOptions = JSON.parse(JSON.stringify(options));
                newOptions.series = series;
                vm.singleAnswerResponseChart = new ApexCharts(document.querySelector(`#chart-${vm.question.id}`), newOptions);
                vm.singleAnswerResponseChart.render();
            };
        },
        link: function ($scope, $element) {
            const vm: IViewModel = $scope.vm;
            vm.Types = Types;
            vm.DistributionStatus = DistributionStatus;
            vm.DateUtils = DateUtils;

            vm.syncResultsMap = () : void => {
                vm.results = new Map();
                for (let distribution of vm.distributions.all) {
                    vm.results.set(distribution.id, vm.formatAnswers(distribution.id));
                }
            };

            vm.downloadFile = (fileId: number) : void => {
                window.open(`/formulaire/responses/files/${fileId}/download`);
            };

            vm.zipAndDownload = () : void => {
                window.open(`/formulaire/responses/${vm.question.id}/files/download/zip`);
            };

            vm.getWidth = (nbResponses: number, divisor: number) : number => {
                let width = nbResponses / (vm.distributions.all.length > 0 ? vm.distributions.all.length : 1) * divisor;
                return width < 0 ? 0 : (width > divisor ? divisor : width);
            }

            vm.getColor = (choiceId: number) : string => {
                let colorIndex = vm.question.choices.all.filter(c => c.nbResponses > 0).findIndex(c => c.id === choiceId);
                return colorIndex >= 0 ? vm.colors[colorIndex] : '#fff';
            };

            vm.formatAnswers = (distribId: number) : any => {
                let results =  vm.responses.all.filter(r => r.distribution_id === distribId);
                for (let result of results) {
                    if (result.answer == "" || (vm.question.question_type === Types.FILE && result.files.all.length <= 0)) {
                        result.answer = "-";
                    }
                }
                return results;
            };

            vm.showMoreButton = () : boolean => {
                let nbResponsesDisplayed = new Set(vm.responses.all.map(r => r.distribution_id)).size;
                return vm.nbResponses > nbResponsesDisplayed && vm.question.question_type != Types.FREETEXT && !vm.isGraphQuestion;
            };

            vm.loadMoreResults = async () : Promise<void> => {
                if (!vm.isGraphQuestion) {
                    await vm.responses.sync(vm.question, vm.question.question_type == Types.FILE, vm.isGraphQuestion ? null : vm.distributions.all.length);
                    await vm.distributions.syncByFormAndStatus(vm.form.id, DistributionStatus.FINISHED, vm.distributions.all.length);
                    vm.syncResultsMap();
                    UtilsUtils.safeApply($scope);
                }
            };

            $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.REFRESH_QUESTION, () => { vm.$onInit(); });
        }
    };
});
