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
    openPropertiesForm(Form): void;
    sendForm(Form): void;
    shareForm(Form): void;
    exportForms(Forms): void;
    deleteForms(Forms): void;
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

    vm.displayDate = (dateToFormat:Date) : string => {
        return "00/00/0000";
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