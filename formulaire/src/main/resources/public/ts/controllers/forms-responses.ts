import {idiom, ng, template} from 'entcore';
import {Distribution, Distributions, DistributionStatus, Form, Forms} from "../models";
import {distributionService} from "../services";
import {FiltersFilters, FiltersOrders, FORMULAIRE_EMIT_EVENT} from "@common/core/enums";
import {Mix} from "entcore-toolkit";

interface ViewModel {
    forms: Forms;
    distributions: Distributions;
    allFormsSelected: boolean;
    searchInput: string;
    pageSize: number;
    limitTo: number;
    sortRemindlist:{
        filter:string,
        order:boolean
    }
    display: {
        grid: boolean,
        lightbox: {
            myResponses: boolean
        }
    };
    loading: boolean;

    $onInit() : Promise<void>;
    sort(field: FiltersOrders) : void;
    filter(filter: FiltersFilters) : void;
    displayFilterName(name: string) : string;
    openDistribution(distrib: Distribution) : void;
    infiniteScroll() : void;
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
        vm.sortRemindlist={
            filter:"name",
            order: true
        }
        vm.display = {
            grid: true,
            lightbox: {
                myResponses: false
            }
        };
        vm.loading = true;

    vm.$onInit = async (): Promise<void> => {
        await vm.forms.syncSent();
        vm.forms.all = vm.forms.all.filter(form => !form.archived);
        vm.distributions = new Distributions();

        vm.forms.filters.find(f => f.name === FiltersFilters.TO_DO).display = true;
        vm.forms.filters.find(f => f.name === FiltersFilters.FINISHED).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.SENDING_DATE).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.TITLE).display = true;

        try {
            vm.distributions.all = Mix.castArrayAs(Distribution, await distributionService.listByResponder());
            for (let form of vm.forms.all) {
                form.distributions = new Distributions();
                form.distributions.all = vm.distributions.all.filter(d => d.form_id === form.id);
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

    vm.openDistribution = (distrib) : void => {
        $scope.$emit(FORMULAIRE_EMIT_EVENT.REDIRECT, { path: `/form/${distrib.form_id}/${distrib.id}/questions/recap` });
    }

    vm.infiniteScroll = () : void => {
        vm.limitTo += vm.pageSize;
    };

    // Toaster

    vm.openForm = async (form: Form) : Promise<void> => {
        vm.forms.deselectAll();
        if (!form.multiple) {
            let distrib = vm.distributions.all.filter(d => d.form_id === form.id)[0];
            if (distrib.status == DistributionStatus.TO_DO) {
                if (form.rgpd) {
                    $scope.redirectTo(`/form/${form.id}/rgpd`);
                }
                else {
                    $scope.$emit(FORMULAIRE_EMIT_EVENT.REDIRECT, { path: `/form/${form.id}/${distrib.id}` });
                }
            }
            else {
                $scope.redirectTo(`/form/${form.id}/${distrib.id}/questions/recap`);
            }
        }
        else if (form.rgpd) {
            $scope.redirectTo(`/form/${form.id}/rgpd`);
        }
        else {
            let distribs = vm.distributions.all.filter(d => d.form_id === form.id);
            let distrib = distribs.filter(d => d.status == DistributionStatus.TO_DO)[0];
            distrib = distrib ? distrib : await distributionService.add(distribs[0].id);
            $scope.$emit(FORMULAIRE_EMIT_EVENT.REDIRECT, { path: `/form/${form.id}/${distrib.id}` });
        }
        $scope.safeApply();
    };

    vm.selectForm = async(form : Form):Promise <void> =>{
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
}]);