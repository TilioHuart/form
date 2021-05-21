import {idiom, model, ng, notify, template} from 'entcore';
import {Distribution, Distributions, Form, Forms} from "../models";
import {distributionService, formService} from "../services";
import {FiltersFilters, FiltersOrders} from "../core/enums";
import {Mix} from "entcore-toolkit";

interface ViewModel {
    forms: Forms;
    distributions: Distributions;
    folder: string;
    allFormsSelected: boolean;
    searchInput: string;
    mail: {
        subject: string,
        body: string
    };
    display: {
        grid: boolean,
        lightbox: {
            sending: boolean,
            sharing: boolean,
            archive: boolean,
            delete: boolean,
            reminder: boolean
        },
        warning: boolean
    };

    openFolder(folderName: string) : void;
    switchAll(value: boolean) : void;
    sort(field: FiltersOrders) : void;
    filter(filter: FiltersFilters) : void;
    openNavMyForms() : void;
    closeNavMyForms() : void;
    displayFilterName(name: string) : string;
    displayFolder() : string;
    getTitle(title: string): string;
    openForm(form: Form) : void;
    openPropertiesForm() : void;
    duplicateForms() : Promise<void>;
    shareForm() : void;
    closeShareFormLightbox() : void;
    seeResultsForm() : void;
    exportForm() : void;
    checkIfFormOpen(): boolean;
    remind() : Promise<void>;
    doRemind() : Promise<void>;
    cancelRemind() : void;
    restoreForms() : Promise<void>;
    archiveForms() : void;
    doArchiveForms() : Promise<void>;
    deleteForms() : void;
    doDeleteForms() : Promise<void>;
}


export const formsListController = ng.controller('FormsListController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.forms = new Forms();
    vm.distributions = new Distributions();
    vm.folder = "mine";
    vm.searchInput = "";
    vm.allFormsSelected = false;
    vm.mail = {
        subject: "",
        body: ""
    };
    vm.display = {
        grid: true,
        lightbox: {
            sending: false,
            sharing: false,
            archive: false,
            delete: false,
            reminder: false
        },
        warning: false
    };

    const init = async () : Promise<void> => {
        await vm.forms.sync();

        vm.forms.filters.find(f => f.name === FiltersFilters.SENT).display = true;
        vm.forms.filters.find(f => f.name === FiltersFilters.SHARED).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.CREATION_DATE).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.MODIFICATION_DATE).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.TITLE).display = true;

        // Check if the folder is ok
        switch (vm.folder) {
            case "mine":
                vm.forms.all = vm.forms.all.filter(form => !form.archived && form.owner_id === model.me.userId);
                break;
            case "shared":
                vm.forms.all = vm.forms.all.filter(form => !form.archived && form.collab && form.owner_id != model.me.userId);
                break;
            case "archived":
                vm.forms.all = vm.forms.all.filter(form => form.archived);
                break;
            default : vm.openFolder('mine'); break;
        }

        $scope.safeApply();
    };


    // Global functions

    vm.openFolder = (folderName: string) : void => {
        vm.folder = folderName;
        init();
    };

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

    vm.openNavMyForms = () : void => {
        document.getElementById("mySidenavForms").style.width = "200px";
    };

    vm.closeNavMyForms = () : void => {
        document.getElementById("mySidenavForms").style.width = "0";
    };

    // Display functions

    vm.displayFilterName = (name: string) : string => {
        return idiom.translate("formulaire.filter." + name.toLowerCase());
    };

    vm.displayFolder = () : string => {
        return idiom.translate("formulaire.forms." + vm.folder);
    };

    vm.getTitle = (title: string) : string => {
        return idiom.translate('formulaire.' + title);
    };

    // Toaster

    vm.openForm = (form: Form) : void => {
        $scope.form = form;
        $scope.redirectTo(`/form/${form.id}/edit`);
        $scope.safeApply();
    };

    vm.openPropertiesForm = () : void => {
        $scope.redirectTo(`/form/${vm.forms.selected[0].id}/properties`);
        $scope.safeApply();
    };

    vm.duplicateForms = async () : Promise<void> => {
        try {
            let formIds = [];
            for (let form of vm.forms.selected) {
                formIds.push(form.id);
            }
            await formService.duplicate(formIds);
            notify.success(idiom.translate('formulaire.success.forms.duplicate'));
            init();
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };

    vm.shareForm = () : void => {
        vm.forms.selected[0].generateShareRights();
        template.open('lightbox', 'lightbox/form-sharing');
        vm.display.lightbox.sharing = true;
    };

    vm.closeShareFormLightbox = () : void => {
        template.close('lightbox');
        vm.display.lightbox.sharing = false;
        window.setTimeout(async function () { await init(); }, 100);
    };

    vm.seeResultsForm = () : void => {
        $scope.redirectTo(`/form/${vm.forms.selected[0].id}/results/1`);
        $scope.safeApply();
    };

    vm.exportForm = () : void => {
        window.open(window.location.pathname + `/export/${vm.forms.selected[0].id}`);
    };

    vm.checkIfFormOpen = () : boolean => {
        let dateOpeningOk = vm.forms.selected[0].date_opening < new Date();
        let dateEndingOk = (!!!vm.forms.selected[0].date_ending || vm.forms.selected[0].date_ending > new Date());
        return dateOpeningOk && dateEndingOk;
    };

    vm.remind = async () : Promise<void> => {
        initMail();
        vm.distributions.all = Mix.castArrayAs(Distribution, $scope.getDataIf200(await distributionService.listByForm(vm.forms.selected[0].id)));
        await template.open('lightbox', 'lightbox/reminder');
        vm.display.lightbox.reminder = true;
        // Set CSS to show text on editor
        window.setTimeout(async function () {
            let toolbar = document.getElementsByTagName('editor-toolbar')[0] as HTMLElement;
            let editor = document.getElementsByTagName('editor')[0] as HTMLElement;
            let text = document.getElementsByClassName('drawing-zone')[0] as HTMLElement;
            editor.style.setProperty('padding-top', `${toolbar.offsetHeight.toString()}px`, "important");
            text.style.setProperty('min-height', `150px`, "important");
        }, 500);
        $scope.safeApply();
    };

    vm.doRemind = async () : Promise<void> => {
        let { status } = await formService.sendReminder(vm.forms.selected[0].id, vm.mail);
        if (status === 200) {
            notify.success(idiom.translate('formulaire.success.reminder.send'));
            initMail();
            template.close('lightbox');
            vm.display.lightbox.reminder = false;
            window.setTimeout(async function () { await init(); }, 100);
        }
    };

    vm.cancelRemind = () : void => {
        initMail();
        vm.distributions = new Distributions();
        template.close('lightbox');
        vm.display.lightbox.reminder = false;
        $scope.safeApply();
    };

    vm.restoreForms = async () : Promise<void> => {
        try {
            for (let form of vm.forms.selected) {
                await formService.restore(form);
            }
            template.close('lightbox');
            notify.success(idiom.translate('formulaire.success.forms.restore'));
            init();
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };

    vm.archiveForms = () : void => {
        vm.display.warning = !!vm.forms.selected.find(form => form.sent === true);
        template.open('lightbox', 'lightbox/form-confirm-archive');
        vm.display.lightbox.archive = true;
    };

    vm.doArchiveForms = async () : Promise<void> => {
        try {
            for (let form of vm.forms.selected) {
                await formService.archive(form);
            }
            template.close('lightbox');
            vm.display.lightbox.archive = false;
            vm.display.warning = false;
            notify.success(idiom.translate('formulaire.success.forms.archive'));
            init();
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };

    vm.deleteForms = () : void => {
        vm.display.warning = true;
        template.open('lightbox', 'lightbox/form-confirm-delete');
        vm.display.lightbox.delete = true;
    };

    vm.doDeleteForms = async () : Promise<void> => {
        try {
            for (let form of vm.forms.selected) {
                if ($scope.isStatusXXX(await formService.unshare(form.id), 200)) {
                    await formService.delete(form.id);
                }
            }
            template.close('lightbox');
            vm.display.lightbox.delete = false;
            vm.display.warning = false;
            notify.success(idiom.translate('formulaire.success.forms.delete'));
            init();
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };

    // Utils

    const initMail = () : void => {
        let link = `${$scope.config.host}/formulaire#/form/${vm.forms.selected[0].id}`;
        vm.mail.subject = idiom.translate('formulaire.remind.default.subject');
        vm.mail.body = $scope.getI18nWithParams('formulaire.remind.default.body', [link, link]);
    };

    init();
}]);