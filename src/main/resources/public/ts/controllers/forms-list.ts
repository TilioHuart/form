import {idiom, ng, notify, toasts} from 'entcore';
import {Form, Forms} from "../models";

interface ViewModel {
    forms: Forms;
    allFormsSelected: boolean;
    searchInput: string;
    display: {
        grid: boolean
    };
    wrongPageName: boolean;

    init(): void;
    openFolder(string): void;
    switchAll(boolean): void;
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
    vm.searchInput = "";
    vm.allFormsSelected = false;
    vm.display = {
        grid: true
    };
    vm.wrongPageName = false;

    vm.init = async (): Promise<void> => {
        $scope.edit.mode = false;
        await vm.forms.sync();

        // Check if the page if ok
        switch ($scope.page) {
            case "mine": vm.forms.all = vm.forms.all.filter(form => form.archived === false); break;
            case "shared": vm.forms.all = vm.forms.all.filter(form => form.shared === true); break;
            case "sent": vm.forms.all = vm.forms.all.filter(form => form.sent === true); break;
            case "archived": vm.forms.all = vm.forms.all.filter(form => form.archived === true); break;
            default :
                vm.wrongPageName = true;
                notify.error(idiom.translate('formulaire.error.404'));
                vm.openFolder('mine');
                break;
        }

        $scope.safeApply();
    };

    // Functions

    vm.openFolder = (pageName:string) => {
        $scope.page = pageName;
        vm.init();
        if (!vm.wrongPageName) {
            $scope.redirectTo(`/forms-list/${pageName}`);
        }
    };

    vm.switchAll = (value:boolean) => {
        value ? vm.forms.selectAll() : vm.forms.deselectAll();
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