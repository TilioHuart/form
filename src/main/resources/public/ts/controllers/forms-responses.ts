import {idiom, ng, notify, toasts} from 'entcore';
import {Form, Forms, Distribution, Distributions} from "../models";
import {DateUtils} from "../utils/date";

interface ViewModel {
    forms: Forms;
    dists: Distributions;
    allDistsSelected: boolean;
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
    vm.dists = new Distributions();
    vm.searchInput = "";
    vm.allDistsSelected = false;
    vm.display = {
        grid: true
    };

    vm.init = async (): Promise<void> => {
        $scope.edit.mode = false;
        await vm.forms.syncSent();
        $scope.safeApply();
    };

    // Functions

    vm.switchAll = (value:boolean) : void => {
        value ? vm.forms.selectAll() : vm.forms.deselectAll();
    };

    // Utils

    vm.displayDate = (dateToFormat:Date) : string => {
        let localDateTime = DateUtils.localise(dateToFormat);
        let date = DateUtils.format(localDateTime, DateUtils.FORMAT["DAY-MONTH-YEAR"]);
        let time = DateUtils.format(localDateTime, DateUtils.FORMAT["HOUR-MINUTES"]);
        return date + idiom.translate('formulaire.at') + time;
    };


    // Toaster

    vm.openForm = (form:Form): void => {
        vm.forms.deselectAll();
        $scope.redirectTo(`/form/${form.id}`);
        $scope.safeApply();
    };

    vm.init();
}]);