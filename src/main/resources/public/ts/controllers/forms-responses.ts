import {idiom, ng, template} from 'entcore';
import {Distribution, Distributions, DistributionStatus, Form, Forms} from "../models";
import {distributionService} from "../services";
import {FiltersFilters, FiltersOrders} from "../core/enums";

interface ViewModel {
    forms: Forms;
    distributions: Distributions;
    allFormsSelected: boolean;
    searchInput: string;
    display: {
        grid: boolean,
        lightbox: {
            myResponses: boolean
        }
    };
    filtersOrders: typeof FiltersOrders;

    switchAll(value: boolean) : void;
    sort(field: FiltersOrders) : void;
    filter() : void;
    displayFilterName(name: string) : string;
    displayDate(date: Date) : string;
    checkOpenButton() : boolean;
    checkMyResponsesButton() : boolean;
    openForm(form: Form) : void;
    myResponses() : void;
    closeMyResponses() : void;
    getMyResponses(form: Form) : Array<Distribution>;
}


export const formsResponsesController = ng.controller('FormsResponsesController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.forms = new Forms();
    vm.distributions = new Distributions();
    vm.searchInput = "";
    vm.allFormsSelected = false;
    vm.display = {
        grid: true,
        lightbox: {
            myResponses: false
        }
    };
    vm.filtersOrders = FiltersOrders;

    const init = async (): Promise<void> => {
        await vm.forms.syncSent();
        vm.distributions = new Distributions();

        vm.forms.filters.find(f => f.name === FiltersFilters.TO_DO).display = true;
        vm.forms.filters.find(f => f.name === FiltersFilters.IN_PROGRESS).display = true;
        vm.forms.filters.find(f => f.name === FiltersFilters.FINISHED).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.SENDING_DATE).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.TITLE).display = true;

        vm.sort(FiltersOrders.SENDING_DATE);

        try {
            for (let form of vm.forms.all) {
                let distribs = $scope.getDataIf200(await distributionService.listByFormAndResponder(form.id));
                for (let d of distribs) {
                    vm.distributions.all.push(d);
                }
                form.date_sending = distribs[0].date_sending;
                form.status = distribs[0].status;
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

    vm.filter = () : void => {
        vm.forms.filterForms();
        $scope.safeApply();
    };

    vm.displayFilterName = (name: string) : string => {
        return idiom.translate("formulaire.filter." + name.toLowerCase());
    };

    // Utils

    vm.displayDate = (dateToFormat: Date) : string => {
        return new Date(dateToFormat + "Z").toLocaleString();
    };

    vm.getMyResponses = (form: Form) : Array<Distribution> => {
        return vm.distributions.all.filter(d => d.form_id === form.id && d.status === DistributionStatus.FINISHED);
    };

    // Toaster

    vm.checkOpenButton = () : boolean => {
        let formId = vm.forms.selected[0].id;
        let status = vm.distributions.all.filter(distrib => distrib.form_id === formId)[0].status;
        return (vm.forms.selected.length === 1 &&
            (vm.forms.selected[0].multiple || status != DistributionStatus.FINISHED));
    };

    vm.checkMyResponsesButton = () : boolean => {
        return (vm.forms.selected.length === 1 && vm.forms.selected[0].multiple);
    };

    vm.openForm = async (form: Form) : Promise<void> => {
        vm.forms.deselectAll();
        if (!form.multiple) {
            let distrib: Distribution = vm.distributions.all.filter(d => d.form_id === form.id)[0];
            if (distrib.status != DistributionStatus.FINISHED) {
                $scope.redirectTo(`/form/${form.id}/question/1`);
            }
        } else {
            let distribs = vm.distributions.all.filter(distrib => distrib.form_id === form.id);
            let distrib: Distribution = null;
            for (let d of distribs) {
                if (d.status != DistributionStatus.FINISHED) {
                    distrib = d;
                }
            }
            if (distrib == null) {
                await distributionService.newDist(distribs[0]);
            }
            $scope.redirectTo(`/form/${form.id}/question/1`);
        }
        $scope.safeApply();
    };

    vm.myResponses = () : void => {
        template.open('lightbox', 'lightbox/my-responses');
        vm.display.lightbox.myResponses = true;
    };

    vm.closeMyResponses = () : void => {
        template.close('lightbox');
        vm.display.lightbox.myResponses = false;
    };

    init();
}]);