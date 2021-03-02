import {idiom, ng} from 'entcore';
import {Distribution, Distributions, DistributionStatus, Form, Forms} from "../models";
import {DateUtils} from "../utils/date";
import {distributionService} from "../services";

interface ViewModel {
    forms: Forms;
    distributions: Distributions;
    allFormsSelected: boolean;
    searchInput: string;
    display: {
        grid: boolean
    };

    switchAll(boolean): void;
    sort() : void;
    filter() : void;
    displayDate(Date): string;
    checkOpenButton(): boolean;
    openForm(Form): void;
}


export const formsResponsesController = ng.controller('FormsResponsesController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.forms = new Forms();
    vm.distributions = new Distributions();
    vm.searchInput = "";
    vm.allFormsSelected = false;
    vm.display = {
        grid: true
    };

    const init = async (): Promise<void> => {
        await vm.forms.syncSent();
        try {
            for (let form of vm.forms.all) {
                vm.distributions.all.push($scope.getDataIf200(await distributionService.get(form.id)));
            }
        }
        catch (e) {
            throw e;
        }
        $scope.safeApply();
    };

    // Functions

    vm.switchAll = (value:boolean) : void => {
        value ? vm.forms.selectAll() : vm.forms.deselectAll();
        vm.allFormsSelected = value;
    };

    vm.sort = () : void => {
        vm.forms.orderForms();
        $scope.safeApply();
    };

    vm.filter = () : void => {
        vm.forms.filterForms();
        $scope.safeApply();
    };

    // Utils

    vm.displayDate = (dateToFormat:Date) : string => {
        let localDateTime = DateUtils.localise(dateToFormat);
        let date = DateUtils.format(localDateTime, DateUtils.FORMAT["DAY-MONTH-YEAR"]);
        let time = DateUtils.format(localDateTime, DateUtils.FORMAT["HOUR-MINUTES"]);
        return date + idiom.translate('formulaire.at') + time;
    };

    // Toaster

    vm.checkOpenButton = (): boolean => {
        let formId = vm.forms.selected[0].id;
        let status = vm.distributions.all.filter(distrib => distrib.form_id === formId)[0].status;
        return vm.forms.selected.length === 1 && status != DistributionStatus.FINISHED;
    };

    vm.openForm = async (form:Form): Promise<void> => {
        vm.forms.deselectAll();
        // let distrib: Distribution = $scope.getDataIf200(await distributionService.get(form.id));
        let distrib: Distribution = vm.distributions.all.filter(distrib => distrib.form_id === form.id)[0];
        if (distrib.status != DistributionStatus.FINISHED) {
            $scope.redirectTo(`/form/${form.id}/question/1`);
        }
        $scope.safeApply();
    };

    init();
}]);