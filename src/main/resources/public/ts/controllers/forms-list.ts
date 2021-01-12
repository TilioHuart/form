import {idiom, ng, notify, toasts} from 'entcore';
import {Form, Forms} from "../models";
import {DateUtils} from "../utils/date";

interface ViewModel {
    forms: Forms;
    folder: string;
    allFormsSelected: boolean;
    searchInput: string;
    display: {
        grid: boolean
    };

    init(): void;
    openFolder(string): void;
    switchAll(boolean): void;
    displayPage(): string;
    displayDate(Date): string;
    openForm(Form): void;
    openPropertiesForm(Form): void;
    sendForm(Form): void;
    shareForm(Form): void;
    exportForms(Forms): void;
    deleteForms(Forms): void;
}


export const formsListController = ng.controller('FormsListController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.forms = new Forms();
    vm.folder = "mine";
    vm.searchInput = "";
    vm.allFormsSelected = false;
    vm.display = {
        grid: true
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

    vm.openFolder = (pageName:string) : void => {
        vm.folder = pageName;
        vm.init();
    };

    vm.switchAll = (value:boolean) : void => {
        value ? vm.forms.selectAll() : vm.forms.deselectAll();
    };

    // Utils

    vm.displayPage = () : string => {
        return idiom.translate("formulaire.forms." + vm.folder);
    };

    vm.displayDate = (dateToFormat:Date) : string => {
        let date = DateUtils.format(dateToFormat, DateUtils.FORMAT["DAY-MONTH-YEAR"]);
        let time = DateUtils.format(dateToFormat, DateUtils.FORMAT["HOUR-MINUTES"]);
        return date + idiom.translate('formulaire.at') + time;
    };


    // Toaster

    vm.openForm = (form:Form): void => {
        vm.forms.deselectAll();
        $scope.edit.mode = true;
        $scope.edit.form = form;
        $scope.redirectTo(`/form/${form.id}`);
        $scope.safeApply();
    };

    vm.openPropertiesForm = (form:Form): void => {
        vm.forms.deselectAll();
        //TODO : Open lightbox avec prop ? Ou juste open form mais pas en mode edit ? -> ask Marylou
        toasts.info('displayPropertiesForm to do');
    };

    vm.sendForm = (form:Form): void => {
        vm.forms.deselectAll();
        //TODO : Open lightbox pour confirmation de l'envoi
        toasts.info('sendForm to do');
    };

    vm.shareForm = (form:Form): void => {
        vm.forms.deselectAll();
        //TODO : Open lightbox pour confirmation du partage
        toasts.info('shareForm to do');
    };

    vm.exportForms = (): void => {
        let formsToExport = vm.forms.selected;
        vm.forms.deselectAll();
        //TODO : Open lightbox pour confirmation de l'export (afficher la liste des forms  concernés)
        toasts.info('exportForms to do');
    };

    vm.deleteForms = (): void => {
        let formsToDelete = vm.forms.selected;
        vm.forms.deselectAll();
        //TODO : Open lightbox pour confirmation de la suppression (afficher la liste des forms concernés)
        toasts.info('deleteForms to do');
    };

    vm.init();
}]);