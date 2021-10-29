import {idiom, ng, template} from 'entcore';
import {Distribution, Distributions, DistributionStatus, Form, Forms} from "../models";
import {distributionService} from "../services";
import {FiltersFilters, FiltersOrders} from "../core/enums";

interface ViewModel {
    forms: Forms;
    distributions: Distributions;
    allFormsSelected: boolean;
    searchInput: string;
    pageSize: number;
    limitTo: number;
    display: {
        grid: boolean,
        lightbox: {
            myResponses: boolean
        }
    };
    loading: boolean;

    switchAll(value: boolean) : void;
    sort(field: FiltersOrders) : void;
    filter(filter: FiltersFilters) : void;
    displayFilterName(name: string) : string;
    getMyResponses(form: Form) : Array<Distribution>;
    infiniteScroll() : void;
    checkOpenButton() : boolean;
    checkMyResponsesButton() : boolean;
    openForm(form: Form) : void;
    selectForm(form : Form):void;
    openMyResponses() : void;
    closeMyResponses() : void;
}


export const formsResponsesController = ng.controller('FormsResponsesController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.forms = new Forms();
    vm.distributions = new Distributions();
    vm.searchInput = "";
    vm.allFormsSelected = false;
    vm.pageSize = 40;
    vm.limitTo = vm.pageSize;
    vm.display = {
        grid: true,
        lightbox: {
            myResponses: false
        }
    };
    vm.loading = true;

    const init = async (): Promise<void> => {
        await vm.forms.syncSent();
        vm.forms.all = vm.forms.all.filter(form => !form.archived);
        vm.distributions = new Distributions();

        vm.forms.filters.find(f => f.name === FiltersFilters.TO_DO).display = true;
        vm.forms.filters.find(f => f.name === FiltersFilters.IN_PROGRESS).display = true;
        vm.forms.filters.find(f => f.name === FiltersFilters.FINISHED).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.SENDING_DATE).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.TITLE).display = true;

        try {
            let allMyDistribs = $scope.getDataIf200(await distributionService.listByResponder());
            for (let form of vm.forms.all) {
                let distribs = allMyDistribs.filter(d => d.form_id === form.id);
                for (let d of distribs) {
                    vm.distributions.all.push(d);
                }
                form.date_sending = distribs[distribs.length - 1].date_sending;
                form.status = distribs[distribs.length - 1].status;
                if (form.multiple) {
                    for (let d of distribs) {
                        if (d.status === DistributionStatus.FINISHED) {
                            form.status = DistributionStatus.FINISHED;
                        }
                    }
                }
            }
        }
        catch (e) {
            throw e;
        }

        vm.sort(FiltersOrders.SENDING_DATE);
        vm.loading = false;
        $scope.safeApply();
    };

    // Functions

    vm.switchAll = (value: boolean) : void => {
        value ? vm.forms.selectAll() : vm.forms.deselectAll();
        vm.allFormsSelected = value;
    };

    vm.sort = (field: FiltersOrders) : void => {
        vm.forms.orderByField(field);
        vm.forms.orderForms();
        $scope.safeApply();
    };

    vm.filter = (filter: FiltersFilters) : void => {
        vm.forms.switchFilter(filter);
        vm.forms.filterForms();
        $scope.safeApply();
    };

    vm.displayFilterName = (name: string) : string => {
        return idiom.translate("formulaire.filter." + name.toLowerCase());
    };

    // Utils

    vm.getMyResponses = (form: Form) : Array<Distribution> => {
        return vm.distributions.all.filter(d => d.form_id === form.id && d.status === DistributionStatus.FINISHED);
    };

    vm.infiniteScroll = () : void => {
        vm.limitTo += vm.pageSize;
    };

    // Toaster

    vm.checkOpenButton = () : boolean => {
        let formId = vm.forms.selected[0].id;
        let status = vm.distributions.all.filter(distrib => distrib.form_id === formId)[0].status;
        return (vm.forms.selected[0].multiple || status != DistributionStatus.FINISHED);
    };

    vm.checkMyResponsesButton = () : boolean => {
        return vm.forms.selected[0].multiple;
    };

    vm.openForm = async (form: Form) : Promise<void> => {
        vm.forms.deselectAll();
        if (!form.multiple) {
            let distrib: Distribution = vm.distributions.all.filter(d => d.form_id === form.id)[0];
            if (distrib.status != DistributionStatus.FINISHED) {
                $scope.redirectTo(`/form/${form.id}/question/1`);
            }
        }
        else {
            let distribs = vm.distributions.all.filter(distrib => distrib.form_id === form.id);
            let distrib: Distribution = null;
            for (let d of distribs) {
                if (d.status != DistributionStatus.FINISHED) {
                    distrib = d;
                }
            }
            if (distrib == null) {
                await distributionService.add(form.id, distribs[0]);
            }
            $scope.redirectTo(`/form/${form.id}/question/1`);
        }
        $scope.safeApply();
    };

    vm.selectForm= async(form : Form):Promise <void> =>{
        if (!form.selected) {
            vm.forms.deselectAll();
            if (form.multiple || status != DistributionStatus.FINISHED) {
                form.selected = true;
            }
        }
        else {
            vm.forms.deselectAll();
        }
    }

    vm.openMyResponses = () : void => {
        template.open('lightbox', 'lightbox/my-responses');
        vm.display.lightbox.myResponses = true;
    };

    vm.closeMyResponses = () : void => {
        template.close('lightbox');
        vm.display.lightbox.myResponses = false;
    };

    init();
}]);