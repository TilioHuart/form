import {idiom, ng} from 'entcore';
import {Distribution, DistributionStatus, Form, Forms} from "../models";
import {DateUtils} from "../utils/date";
import {distributionService} from "../services";

interface ViewModel {
    forms: Forms;
    allFormsSelected: boolean;
    searchInput: string;
    display: {
        grid: boolean
    };

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

    const init = async (): Promise<void> => {
        $scope.editMode = false;
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

    vm.openForm = async (form:Form): Promise<void> => {
        vm.forms.deselectAll();
        let distrib: Distribution = $scope.getDataIf200(await distributionService.get(form.id));
        if (distrib.status != DistributionStatus.FINISHED) {
            $scope.redirectTo(`/form/${form.id}/question/1`);
        }
        $scope.safeApply();
    };

    init();
}]);