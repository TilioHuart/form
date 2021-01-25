import {idiom, ng, notify, toasts} from 'entcore';
import {Form, Forms} from "../models";
import {DateUtils} from "../utils/date";

interface ViewModel {
    forms: Forms;
    allFormsSelected: boolean;
    searchInput: string;
    display: {
        grid: boolean
    };

    init(): void;
    switchAll(boolean): void;
    displayDate(Date): string;
    openForm(Form): void;
}


export const formsResponsesController = ng.controller('FormsResponsesController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.forms = new Forms();
    vm.searchInput = "";
    vm.allFormsSelected = false;
    vm.display = {
        grid: true
    };

    vm.init = async (): Promise<void> => {
        $scope.edit.mode = false;
        await vm.forms.sync();

        $scope.safeApply();
    };

    // Functions

    vm.switchAll = (value:boolean) : void => {
        value ? vm.forms.selectAll() : vm.forms.deselectAll();
    };

    // Utils

    vm.displayDate = (dateToFormat) : string => {
        return dateToFormat;
    };


    // Toaster

    vm.openForm = (form:Form): void => {
        vm.forms.deselectAll();
        $scope.redirectTo(`/form/${form.id}`);
        $scope.safeApply();
    };

    vm.init();
}]);