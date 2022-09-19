import {ng, template} from 'entcore';
import * as ApexCharts from 'apexcharts';
import {
    Distributions,
    DistributionStatus,
    Form, FormElement, FormElements,
    Question,
    QuestionChoice,
    Responses
} from "../models";
import {Mix} from "entcore-toolkit";
import {
    Exports,
    FORMULAIRE_BROADCAST_EVENT
} from "@common/core/enums";
import {formService, questionChoiceService, utilsService} from "../services";
import {GraphUtils} from "@common/utils/graph";

interface ViewModel {
    formElement: FormElement;
    formElements: FormElements;
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

        vm.$onInit = async () : Promise<void> => {
            vm.loading = true;
            vm.form = $scope.form;

            let formElementId: number = $scope.formElement.id;
            await vm.formElements.sync(vm.form.id);
            vm.formElement = vm.formElements.all.filter(e => e.id === formElementId)[0];

            vm.navigatorValue = vm.formElement.position;
            vm.nbFormElements = $scope.form.nbFormElements;
            vm.last = vm.formElement.position === vm.nbFormElements;
            vm.loading = false;

            $scope.safeApply();
        };

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
                doc = await formService.export(vm.formElement.form_id, Exports.CSV);
                blob = new Blob(["\ufeff" + doc.data], {type: 'text/csv; charset=utf-18'});
            }
            else {
                let images = await prepareDataForPDF();
                doc = await formService.export(vm.formElement.form_id, Exports.PDF, images);
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

        vm.getGraphQuestions = () : Question[] => {
            return vm.formElements.getAllQuestions().all.filter((q: Question) => q.isTypeGraphQuestion());
        };

        // Navigation

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

        // PDF

        const prepareDataForPDF = async () : Promise<any> => {
            vm.pdfResponseCharts = [];
            let images: any = {
                idImagesPerQuestion : {}, // id image for each id question of Type QCM or QCU
                idImagesForRemove : [] // all id images (to remove from storage after export PDF)
            };
            let questions: Question[] = vm.getGraphQuestions();

            if (questions.length > 0) {
                // Sync form responses
                let results: Responses = new Responses();
                await results.syncForForm(vm.form.id);
                // Sync form distributions
                let distribs: Distributions = new Distributions();
                await distribs.syncByFormAndStatus(vm.form.id, DistributionStatus.FINISHED, null, null);
                // Sync questions choices
                let questionIds: number[] = questions.map((q: Question) => q.id);
                let listChoices: QuestionChoice[] = Mix.castArrayAs(QuestionChoice, await questionChoiceService.listChoices(questionIds));

                for (let question of questions) {
                    // Format question choices
                    question.choices.all = [];
                    question.choices.all = listChoices.filter((c: QuestionChoice) => c.question_id === question.id);
                    question.choices.replaceSpace();
                    question.fillChoicesInfo(distribs, results.all);
                    // Generate graphs
                    await GraphUtils.generateGraphForPDF(question, vm.pdfResponseCharts, distribs.all.length);
                }

                await storeAllCharts(questions, vm.pdfResponseCharts, images);
            }
            $scope.safeApply();
            return images;
        }

        const storeAllCharts = async (questions: Question[], charts: ApexCharts[], images: any) : Promise<any> => {
            let formData = new FormData();
            for (let i = 0; i < charts.length; i++) {
                let question = questions[i];
                let chart = charts[i];

                let image = await chart.dataURI();
                let blob = new Blob([image["imgURI"]], {type: 'image/png'});
                formData.append(`graph-${vm.form.id}-${question.id}`, blob);
            }

            let config = {
                headers: {
                    'Content-Type': 'multipart/form-data',
                    'Number-Files': charts.length
                }
            };
            let data = await utilsService.postMultipleFiles(formData, config);

            for (let file of data) {
                let fileId = file.id;
                let questionId = file.metadata.name.split('-')[2];
                images.idImagesPerQuestion[questionId] = fileId;
                images.idImagesForRemove.push(fileId);
            }
        }

        $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_FORM_RESULTS, () => { vm.$onInit() });
    }]);