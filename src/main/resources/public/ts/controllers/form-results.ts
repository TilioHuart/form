import {ng, template} from 'entcore';
import {
    Distributions, DistributionStatus,
    Form,
    Question, QuestionChoices,
    Questions,
    Responses,
    Types
} from "../models";
import {Mix} from "entcore-toolkit";

interface ViewModel {
    types: typeof Types;
    question: Question;
    questions: Questions;
    results: Responses;
    distributions: Distributions;
    form: Form;
    nbResults: number;
    nbResultsDisplay: number;
    nbQuestions: number;
    last: boolean;
    navigatorValue: number;
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

        const vm: ViewModel = this;
        vm.types = Types;
        vm.question = new Question();
        vm.questions = new Questions();
        vm.results = new Responses();
        vm.distributions = new Distributions();
        vm.form = new Form();
        vm.nbResults = 0;
        vm.nbResultsDisplay = 10;
        vm.nbQuestions = 1;
        vm.last = false;
        vm.navigatorValue = 1;
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
            vm.nbQuestions = $scope.form.nbQuestions;
            vm.last = vm.question.position === vm.nbQuestions;
            if (vm.question.question_type == vm.types.SINGLEANSWER ||
                vm.question.question_type == vm.types.MULTIPLEANSWER) {
                for (let result of vm.results.all) {
                    for (let choice of vm.question.choices.all) {
                        if (result.choice_id == choice.id) {
                            choice.nbResponses++;
                        }
                    }
                }
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

        init();

        $rootScope.$on( "$routeChangeSuccess", function(event, next, current) {
            window.location.reload();
        });
    }]);