import {ng} from 'entcore';
import {Question, Questions, Responses, Types} from "../models";
import {questionService, responseFileService} from "../services";

interface ViewModel {
    types: typeof Types;
    question: Question;
    questions: Questions;
    results: Responses;
    nbResults: number;
    nbResultsDisplay: number;
    nbQuestions: number;
    last: boolean;
    navigatorValue: number;
    files: File[];

    exportForm() : void;
    downloadFile(responseId: number): Promise<void>;
    prev(): Promise<void>;
    next(): Promise<void>;
    goTo(position: number): Promise<void>;
}


export const formResultsController = ng.controller('FormResultsController', ['$scope', '$rootScope',
    function ($scope, $rootScope) {

        const vm: ViewModel = this;
        vm.types = Types;
        vm.question = new Question();
        vm.questions = new Questions();
        vm.results = new Responses();
        vm.files = [];
        vm.nbResults = 0;
        vm.nbResultsDisplay = 10;
        vm.nbQuestions = 1;
        vm.last = false;
        vm.navigatorValue = 1;

        const init = async (): Promise<void> => {
            vm.question = $scope.question;
            vm.navigatorValue = vm.question.position;
            await vm.questions.sync($scope.form.id);
            await vm.results.sync(vm.question.id);
            vm.nbResults = vm.results.all.length;
            vm.nbQuestions = $scope.form.nbQuestions;
            vm.last = vm.question.position === vm.nbQuestions;
            if (vm.question.question_type === Types.FILE) {
                vm.files = [];
                let files = $scope.getDataIf200(await responseFileService.list(vm.question.id));
                for (let i = 0; i < files.length; i++) {
                    vm.files.push(files[i]);
                }
                vm.nbResults = vm.files.length;
            }
            $scope.safeApply();
        };

        // Functions

        vm.exportForm = () : void => {
            window.open(window.location.pathname + `/export/${vm.question.form_id}`);
        };

        vm.downloadFile = async (responseId: number) : Promise<void> => {
            window.open(`/formulaire/responses/${responseId}/files/download`);
        }

        vm.prev = async (): Promise<void> => {
            let prevPosition: number = vm.question.position - 1;
            if (prevPosition > 0) {
                await vm.goTo(prevPosition);
            }
        };

        vm.next = async (): Promise<void> => {
            let nextPosition: number = vm.question.position + 1;
            if (nextPosition <= vm.nbQuestions) {
                await vm.goTo(nextPosition);
            }
        };

        vm.goTo = async (position: number) : Promise<void> => {
            $scope.redirectTo(`/form/${vm.question.form_id}/results/${position}`);
        }


        init();

        $rootScope.$on( "$routeChangeSuccess", function(event, next, current) {
            window.location.reload();
        });
    }]);