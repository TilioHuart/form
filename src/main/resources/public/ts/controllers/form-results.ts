import {idiom, ng, template} from 'entcore';
import * as ApexCharts from 'apexcharts';
import {
    Distributions, DistributionStatus,
    Form,
    Question, QuestionChoice, QuestionChoices,
    Questions,
    Responses,
    Types
} from "../models";
import {Mix} from "entcore-toolkit";
import {ColorUtils} from "../utils/color";

interface ViewModel {
    types: typeof Types;
    question: Question;
    questions: Questions;
    results: Responses;
    distributions: Distributions;
    form: Form;
    nbResults: number;
    nbLines: number;
    nbLinesDisplay: number;
    nbQuestions: number;
    last: boolean;
    navigatorValue: number;
    singleAnswerResponseChart: any;
    multipleAnswerResponseChart: any;
    colors: string[];
    display: {
        lightbox: {
            download: boolean;
        }
    }

    exportForm() : void;
    doExportForm() : void;
    downloadFile(responseId: number) : void;
    zipAndDownload() : void;
    getDataByDistrib(distribId: number) : any;
    prev() : Promise<void>;
    next() : Promise<void>;
    goTo(position: number) : Promise<void>;
}


export const formResultsController = ng.controller('FormResultsController', ['$scope', '$rootScope',
    function ($scope, $rootScope) {
        let paletteColors = ['#FFE4CC','#FF8500','#9C5000']; // Beige, Orange, Marron

        const vm: ViewModel = this;
        vm.types = Types;
        vm.question = new Question();
        vm.questions = new Questions();
        vm.results = new Responses();
        vm.distributions = new Distributions();
        vm.form = new Form();
        vm.nbResults = 0;
        vm.nbLines = 0;
        vm.nbLinesDisplay = 10;
        vm.nbQuestions = 1;
        vm.last = false;
        vm.navigatorValue = 1;
        vm.singleAnswerResponseChart = null;
        vm.multipleAnswerResponseChart = null;
        vm.colors = [];
        vm.display = {
            lightbox: {
                download: false
            }
        };

        const init = async () : Promise<void> => {
            vm.form = $scope.form;
            vm.question = Mix.castAs(Question, $scope.question);
            vm.navigatorValue = vm.question.position;
            await vm.question.choices.sync(vm.question.id);
            await vm.questions.sync(vm.form.id);
            await vm.results.sync(vm.question.id);
            await vm.distributions.syncByForm(vm.form.id);
            let validDistribIds : any = vm.results.all.map(r => r.distribution_id);
            vm.distributions.all = vm.distributions.all.filter(d => validDistribIds.includes(d.id) && d.status === DistributionStatus.FINISHED);
            vm.nbResults = vm.distributions.all.length;
            vm.nbLines = vm.nbResults;
            vm.nbQuestions = $scope.form.nbQuestions;
            vm.last = vm.question.position === vm.nbQuestions;
            if (vm.question.question_type == vm.types.SINGLEANSWER || vm.question.question_type == vm.types.MULTIPLEANSWER) {
                // Count responses for each choice
                for (let result of vm.results.all) {
                    for (let choice of vm.question.choices.all) {
                        if (result.choice_id === choice.id) {
                            choice.nbResponses++;
                        }
                    }
                }

                // Deal with no choice responses
                let noResponseChoice = new QuestionChoice();
                noResponseChoice.value = idiom.translate('formulaire.response.empty');
                noResponseChoice.nbResponses = vm.results.all.filter(r => !!!r.choice_id).length;
                vm.question.choices.all.push(noResponseChoice);
                let choices = vm.question.choices.all.filter(c => c.nbResponses > 0);
                vm.colors = ColorUtils.interpolateColors(paletteColors, choices.length);

                // Init charts
                if (vm.question.question_type == vm.types.SINGLEANSWER) {
                    initSingleAnswerChart(choices);
                }
                // if (vm.question.question_type == vm.types.MULTIPLEANSWER) {
                //     initMultipleAnswerChart();
                // }
                vm.nbLines = vm.question.choices.all.length;
            }
            $scope.safeApply();
        };

        // Functions

        vm.exportForm = () : void => {
            template.open('lightbox', 'lightbox/results-confirm-download-all');
            vm.display.lightbox.download = true;
            $scope.safeApply();
        };

        vm.doExportForm = () : void => {
            window.open(window.location.pathname + `/export/${vm.question.form_id}`);
            vm.display.lightbox.download = false;
            template.close('lightbox');
            $scope.safeApply();
        };

        vm.downloadFile = (fileId: number) : void => {
            window.open(`/formulaire/responses/files/${fileId}/download`);
        };

        vm.zipAndDownload = () : void => {
            window.open(`/formulaire/responses/${vm.question.id}/files/download/zip`);
        };

        vm.getDataByDistrib = (distribId: number) : any => {
            let results =  vm.results.all.filter(r => r.distribution_id === distribId && r.question_id === vm.question.id);
            if (vm.question.question_type === Types.FILE) {
                return results.map(r => r.files)[0].all;
            }
            return results;
        };

        vm.prev = async () : Promise<void> => {
            let prevPosition: number = vm.question.position - 1;
            if (prevPosition > 0) {
                await vm.goTo(prevPosition);
            }
        };

        vm.next = async () : Promise<void> => {
            let nextPosition: number = vm.question.position + 1;
            if (nextPosition <= vm.nbQuestions) {
                await vm.goTo(nextPosition);
            }
        };

        vm.goTo = async (position: number) : Promise<void> => {
            $scope.redirectTo(`/form/${vm.question.form_id}/results/${position}`);
        };

        // Charts

        const initSingleAnswerChart = (choices: QuestionChoice[]) : void => {
            // Fill data
            let series = [];
            for (let choice of choices) {
                series.push(choice.nbResponses);
            }

            // Fill labels
            let labels = [];
            let i18nValue = idiom.translate('formulaire.response');
            i18nValue = i18nValue.charAt(0).toUpperCase() + i18nValue.slice(1);
            for (let i = 0; i < vm.question.choices.all.length; i++) {
                let choice = vm.question.choices.all[i];
                if (choice.nbResponses > 0) {
                    if (!!!choice.id) {
                        labels.push(idiom.translate('formulaire.response.empty'));
                    }
                    else {
                        labels.push(i18nValue + " " + (i+1));
                    }
                }
            }

            // Generate options with labels and colors
            let options = {
                chart: {
                    type: 'pie',
                    height: 40 * vm.question.choices.all.length
                },
                colors: vm.colors,
                labels: labels
            }

            // Generate chart with options and data
            if (vm.singleAnswerResponseChart) {
                vm.singleAnswerResponseChart.updateSeries(series, true);
                vm.singleAnswerResponseChart.updateOptions(options, true);
            }
            else {
                var newOptions = JSON.parse(JSON.stringify(options));
                newOptions.series = series;
                vm.singleAnswerResponseChart = new ApexCharts(document.querySelector('#singleanswer-response-chart'), newOptions);
                vm.singleAnswerResponseChart.render();
            }
        }

        const initMultipleAnswerChart = () : void => {
            let choices = vm.question.choices.all.filter(c => c.nbResponses > 0);

            // Fill data
            let series = [];
            for (let choice of choices) {
                series.push(choice.nbResponses);
            }

            // Fill labels
            let labels = [];
            let i18nValue = idiom.translate('formulaire.response');
            i18nValue = i18nValue.charAt(0).toUpperCase() + i18nValue.slice(1);
            for (let i = 0; i < choices.length; i++) {
                labels.push(i18nValue + " " + (i+1));
            }
            // labels.push(idiom.translate('formulaire.response.empty'));

            // Generate options with labels and colors
            let options = {
                chart: {
                    type: 'bar'
                },
                plotOptions: {
                    bar: {
                        borderRadius: 4,
                        horizontal: true,
                    }
                },
                dataLabels: {
                    enabled: false
                },
                xaxis: {
                    categories: labels,
                }
            }

            // Generate chart with options and data
            if (vm.multipleAnswerResponseChart) {
                vm.multipleAnswerResponseChart.updateSeries(series, true);
                vm.multipleAnswerResponseChart.updateOptions(options, true);
            }
            else {
                var newOptions = JSON.parse(JSON.stringify(options));
                newOptions.series = [{ data: series }];
                vm.multipleAnswerResponseChart = new ApexCharts(document.querySelector('#multipleanswer-response-chart'), newOptions);
                vm.multipleAnswerResponseChart.render();
            }
        }


        init();

        $rootScope.$on( "$routeChangeSuccess", function(event, next, current) {
            window.location.reload();
        });
    }]);