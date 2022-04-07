import {idiom, ng, template} from 'entcore';
import * as ApexCharts from 'apexcharts';
import {
    Distributions,
    DistributionStatus,
    Form, FormElement, FormElements,
    Question,
    QuestionChoice,
    Responses,
    Types
} from "../models";
import {Mix} from "entcore-toolkit";
import {ColorUtils} from "../utils";
import {
    Exports,
    FORMULAIRE_BROADCAST_EVENT
} from "../core/enums";
import http from "axios";
import {questionChoiceService} from "../services";

interface ViewModel {
    formElement: FormElement;
    formElements: FormElements;
    results: Responses; // TODO delete avec delete initQCMandQCU pour la refonte export
    distributions: Distributions; // TODO delete avec delete initQCMandQCU pour la refonte export
    form: Form;
    nbFormElements: number;
    last: boolean;
    navigatorValue: number;
    typeExport: Exports;
    pdfResponseCharts: any;
    display: {
        lightbox: {
            export: boolean;
        }
    }
    loading: boolean;
    paletteColors: string[];

    $onInit() : Promise<void>;
    export(typeExport: Exports) : void;
    doExport() : void;
    prev() : Promise<void>;
    next() : Promise<void>;
    goTo(position: number) : Promise<void>;
    getGraphQuestions() : Question[];
}


export const formResultsController = ng.controller('FormResultsController', ['$scope',
    function ($scope) {

        const vm: ViewModel = this;
        vm.formElements = new FormElements();
        vm.results = new Responses();
        vm.distributions = new Distributions();
        vm.form = new Form();
        vm.nbFormElements = 1;
        vm.last = false;
        vm.navigatorValue = 1;
        vm.typeExport = Exports.CSV;
        vm.pdfResponseCharts = [];
        vm.display = {
            lightbox: {
                export: false
            }
        };
        vm.paletteColors = ['#0F2497','#2A9BC7','#77C4E1','#C0E5F2']; // Dark blue to light blue

        vm.$onInit = async () : Promise<void> => {
            vm.loading = true;
            vm.form = $scope.form;
            vm.formElement = $scope.formElement;
            vm.navigatorValue = vm.formElement.position;
            vm.nbFormElements = $scope.form.nbFormElements;
            vm.last = vm.formElement.position === vm.nbFormElements;
            await vm.formElements.sync(vm.form.id);
            vm.loading = false;
            $scope.safeApply();
        };

        const initQCMandQCU = async (question: Question) : Promise<Question> => {
            // Get distributions and results
            let results = new Responses();
            let distribs = new Distributions();
            await results.sync(question, false, null);
            await distribs.syncByFormAndStatus(vm.form.id, DistributionStatus.FINISHED, null);

            // Count responses for each choice
            for (let result of results.all) {
                for (let choice of question.choices.all) {
                    if (result.choice_id === choice.id) {
                        choice.nbResponses++;
                    }
                }
            }

            // Deal with no choice responses
            let finishedDistribIds : any = distribs.all.map(d => d.id);
            let noResponseChoice = new QuestionChoice();
            noResponseChoice.value = idiom.translate('formulaire.response.empty');
            noResponseChoice.nbResponses = results.all.filter(r => !r.choice_id && finishedDistribIds.includes(r.distribution_id)).length;

            question.choices.all.push(noResponseChoice);

            return question;
        }

        // Functions

        vm.export = (typeExport: Exports) : void => {
            vm.typeExport = typeExport;
            template.open('lightbox', 'lightbox/results-confirm-export');
            vm.display.lightbox.export = true;
            $scope.safeApply();
        };

        vm.doExport = async () : Promise<void> => {
            vm.loading = true;
            let doc;
            let blob;

            // Generate document (CSV or PDF) and store it in a blob
            if (vm.typeExport === Exports.CSV) {
                doc = await http.post(`/formulaire/export/csv/${vm.formElement.form_id}`, {});
                blob = new Blob(["\ufeff" + doc.data], {type: 'text/csv; charset=utf-18'});
            }
            else {
                let images = await prepareDataForPDF();
                doc = await http.post(`/formulaire/export/pdf/${vm.formElement.form_id}`, images, {responseType: "arraybuffer"});
                blob = new Blob([doc.data], {type: 'application/pdf; charset=utf-18'});
            }

            // Download the blob
            let link = document.createElement('a');
            link.href = window.URL.createObjectURL(blob);
            link.download =  doc.headers['content-disposition'].split('filename=')[1];
            document.body.appendChild(link);
            link.click();
            setTimeout(function() {
                document.body.removeChild(link);
                window.URL.revokeObjectURL(link.href);
            }, 100);

            // Delete PDF charts on the web page
            let chart : ApexCharts;
            for (chart of vm.pdfResponseCharts) {
                chart.destroy();
            }

            vm.display.lightbox.export = false;
            vm.loading = false;
            template.close('lightbox');
            $scope.safeApply();
        };

        vm.prev = async () : Promise<void> => {
            let prevPosition: number = vm.formElement.position - 1;
            if (prevPosition > 0) {
                await vm.goTo(prevPosition);
            }
        };

        vm.next = async () : Promise<void> => {
            let nextPosition: number = vm.formElement.position + 1;
            if (nextPosition <= vm.nbFormElements) {
                await vm.goTo(nextPosition);
            }
        };

        vm.goTo = async (position: number) : Promise<void> => {
            $scope.redirectTo(`/form/${vm.formElement.form_id}/results/${position}`);
            $scope.safeApply();
        };

        vm.getGraphQuestions = () : Question[] => {
            return vm.formElements.getAllQuestions().all.filter(q => q.question_type === Types.SINGLEANSWER ||
                q.question_type === Types.MULTIPLEANSWER || q.question_type ===Types.SINGLEANSWERRADIO);
        };

        // PDF

        const prepareDataForPDF = async () : Promise<any> => {
            vm.pdfResponseCharts = [];
            let images = {
                idImagesPerQuestion : {}, // id image for each id question of Type QCM or QCU
                idImagesForRemove : [] // all id images (to remove from storage after export PDF)
            };
            let questions = vm.getGraphQuestions();
            let question : Question = null;
            if(questions.length > 0) {
                let questionIds = questions.map(q => q.id);
                let listChoices = Mix.castArrayAs(QuestionChoice, await questionChoiceService.listChoices(questionIds));
                for (question of questions) {
                    question.choices.all = [];
                    question.choices.all = listChoices.filter(c => c.question_id === question.id);
                    question.choices.replaceSpace();
                    question = await initQCMandQCU(question);
                    let dataOptions = initChartsForPDF(question);
                    let options = generateOptions(dataOptions, question.question_type);
                    await renderGraphForPDF(options);
                    let idImage = await storeChart(vm.pdfResponseCharts[vm.pdfResponseCharts.length-1]);
                    images.idImagesPerQuestion[question.id] = idImage;
                    images.idImagesForRemove.push(idImage);
                }
            }
            $scope.safeApply();
            return images;
        }

        const initChartsForPDF = (question: Question) : any => {
            let choices = question.question_type === Types.SINGLEANSWER || question.question_type === Types.SINGLEANSWERRADIO ?
                question.choices.all.filter(c => c.nbResponses > 0) :
                question.choices.all;

            let series = [];
            let labels = [];

            for (let choice of choices) {
                series.push(choice.nbResponses); // Fill data
                // Fill labels
                !choice.id ?
                    labels.push(idiom.translate('formulaire.response.empty')) :
                    labels.push(choice.value.substring(0, 40) + (choice.value.length > 40 ? "..." : ""));
            }

            return {
                series: series,
                labels: labels,
                colors: ColorUtils.interpolateColors(vm.paletteColors, labels.length)
            };
        }

        const generateOptions = (dataOptions: any, type: Types) : any => {
            let newOptions;
            if (type === Types.SINGLEANSWER || type === Types.SINGLEANSWERRADIO) {
                let options = {
                    chart: {
                        type: 'pie',
                        height: 400,
                        width: 600,
                        animations: {
                            enabled: false
                        }
                    },
                    colors: dataOptions.colors,
                    labels: dataOptions.labels
                }
                newOptions = JSON.parse(JSON.stringify(options));
                newOptions.series = dataOptions.series;
            }
            else {
                let options = {
                    chart: {
                        type: 'bar',
                        height: 400,
                        width: 600,
                        animations: {
                            enabled: false
                        }
                    },
                    plotOptions: {
                        bar: {
                            borderRadius: 4,
                            horizontal: true,
                        }
                    },
                    colors: ColorUtils.interpolateColors(vm.paletteColors, 1),
                    xaxis: {
                        categories: dataOptions.labels,
                    }
                }
                newOptions = JSON.parse(JSON.stringify(options));
                newOptions.series = [{ data: dataOptions.series }];
            }
            return newOptions;
        }

        const renderGraphForPDF = async (options: any) : Promise<void> => {
            vm.pdfResponseCharts.push(new ApexCharts(document.querySelector('#pdf-response-chart-' + (vm.pdfResponseCharts.length)), options));
            await vm.pdfResponseCharts[vm.pdfResponseCharts.length - 1].render();
        }

        const storeChart = async (chart: ApexCharts) : Promise<number> => {
            let image = await chart.dataURI();
            let blob = new Blob([image["imgURI"]], {type: 'image/png'});
            let formData = new FormData();
            formData.append('file', blob);
            let response = await http.post('/formulaire/file/img', formData);
            return response.data._id;
        };

        $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_FORM_RESULTS, () => { vm.$onInit() });
    }]);