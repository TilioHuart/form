import {idiom, ng, notify, template, toasts} from 'entcore';
import {Form, Forms} from "../models";
import {DateUtils} from "../utils/date";
import {formService, questionService} from "../services";

interface ViewModel {
    forms: Forms;
    folder: string;
    allFormsSelected: boolean;
    searchInput: string;
    display: {
        grid: boolean,
        lightbox: {
            prop: boolean,
            sending: boolean,
            sharing: boolean,
            export: boolean,
            delete: boolean
        },
        warning: boolean
    };

    init(): void;
    openFolder(string): void;
    switchAll(boolean): void;
    displayFolder(): string;
    displayDate(Date): string;
    openForm(Form): void;
    openPropertiesForm(): void;
    sendForm(): void;
    doSendForm(): void;
    shareForm(): void;
    exportForms(): void;
    deleteForms(): void;
    doDeleteForms(): Promise<void>;
}


export const formsListController = ng.controller('FormsListController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.forms = new Forms();
    vm.folder = "mine";
    vm.searchInput = "";
    vm.allFormsSelected = false;
    vm.display = {
        grid: true,
        lightbox: {
            prop: false,
            sending: false,
            sharing: false,
            export: false,
            delete: false
        },
        warning: false
    };

    vm.init = async (): Promise<void> => {
        $scope.edit.mode = false;
        await vm.forms.sync();


        // Check if the folder is ok
        switch (vm.folder) {
            case "mine": vm.forms.all = vm.forms.all.filter(form => form.archived === false); break;
            case "shared": vm.forms.all = vm.forms.all.filter(form => form.shared === true); break;
            case "sent": vm.forms.all = vm.forms.all.filter(form => form.sent === true); break;
            case "archived": vm.forms.all = vm.forms.all.filter(form => form.archived === true); break;
            default : vm.openFolder('mine'); break;
        }

        $scope.safeApply();
    };

    // Functions

    vm.openFolder = (foldeName:string) : void => {
        vm.folder = foldeName;
        vm.init();
    };

    vm.switchAll = (value:boolean) : void => {
        value ? vm.forms.selectAll() : vm.forms.deselectAll();
    };

    // Utils

    vm.displayFolder = () : string => {
        return idiom.translate("formulaire.forms." + vm.folder);
    };

    vm.displayDate = (dateToFormat:Date) : string => {
        let localDateTime = DateUtils.localise(dateToFormat);
        let date = DateUtils.format(localDateTime, DateUtils.FORMAT["DAY-MONTH-YEAR"]);
        let time = DateUtils.format(localDateTime, DateUtils.FORMAT["HOUR-MINUTES"]);
        return date + idiom.translate('formulaire.at') + time;
    };


    // Toaster

    vm.openForm = (form: Form): void => {
        $scope.edit.mode = true;
        $scope.edit.form = form;
        $scope.redirectTo(`/form/${form.id}`);
        $scope.safeApply();
    };

    vm.openPropertiesForm = (): void => {
        //TODO : Open lightbox avec props
        template.open('lightbox', 'lightbox/form-prop');
        vm.display.lightbox.prop = true;
    };

    vm.sendForm = (): void => {
        //TODO : Lightbox pour confirmation de l'envoi
        template.open('lightbox', 'lightbox/form-sending');
        vm.display.lightbox.sending = true;
    };

    vm.doSendForm = async (): Promise<void> => {
        try {
            template.close('lightbox');
            vm.display.lightbox.sending = false;
            // notify.success(idiom.translate('formulaire.success.forms.delete'));
            vm.init();
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };

    vm.shareForm = (): void => {
        //TODO : Lightbox pour confirmation du partage
        template.open('lightbox', 'lightbox/form-sharing');
        vm.display.lightbox.sharing = true;
    };

    vm.exportForms = (): void => {
        //TODO : Lightbox pour confirmation de l'export (afficher la liste des forms  concernés)
        template.open('lightbox', 'lightbox/form-confirm-export');
        vm.display.lightbox.export = true;

    };

    vm.deleteForms = (): void => {
        vm.display.warning = !!vm.forms.selected.find(form => form.sent === true);
        template.open('lightbox', 'lightbox/form-confirm-delete');
        vm.display.lightbox.delete = true;
    };

    vm.doDeleteForms = async (): Promise<void> => {
        try {
            for (let form of vm.forms.selected) {
                let response = await formService.archive(form);
            }
            template.close('lightbox');
            vm.display.lightbox.delete = false;
            vm.display.warning = false;
            notify.success(idiom.translate('formulaire.success.forms.delete'));
            vm.init();
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };

    vm.init();
}]);