import {idiom, ng, notify, template} from 'entcore';
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
            sending: boolean,
            sharing: boolean,
            archive: boolean,
            delete: boolean
        },
        warning: boolean
    };

    openFolder(string): void;
    switchAll(boolean): void;
    displayFolder(): string;
    displayDate(Date): string;
    checkOpenButton(): boolean;
    openForm(Form): void;
    openPropertiesForm(): void;
    duplicateForms(): void;
    sendForm(): void;
    closeSendFormLightbox(): void;
    shareForm(): void;
    seeResultsForm(): void;
    exportForm(): void;
    restoreForms(): Promise<void>;
    archiveForms(): void;
    doArchiveForms(): Promise<void>;
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
            sending: false,
            sharing: false,
            archive: false,
            delete: false
        },
        warning: false
    };

    const init = async (): Promise<void> => {
        await vm.forms.sync();

        // Check if the folder is ok
        switch (vm.folder) {
            case "mine": vm.forms.all = vm.forms.all.filter(form => form.archived === false); break;
            case "shared": vm.forms.all = vm.forms.all.filter(form => form.collab === true); break;
            case "sent": vm.forms.all = vm.forms.all.filter(form => form.sent === true); break;
            case "archived": vm.forms.all = vm.forms.all.filter(form => form.archived === true); break;
            default : vm.openFolder('mine'); break;
        }

        $scope.safeApply();
    };


    // Global functions

    vm.openFolder = (folderName:string) : void => {
        vm.folder = folderName;
        init();
    };

    vm.switchAll = (value:boolean) : void => {
        value ? vm.forms.selectAll() : vm.forms.deselectAll();
    };


    // Display functions

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

    vm.checkOpenButton = (): boolean => {
        return vm.forms.selected.length === 1;
    };

    vm.openForm = (form: Form): void => {
        $scope.form = form;
        $scope.redirectTo(`/form/${form.id}`);
        $scope.safeApply();
    };

    vm.openPropertiesForm = (): void => {
        $scope.redirectTo(`/form/${vm.forms.selected[0].id}/properties`);
        $scope.safeApply();
    };

    vm.duplicateForms = async (): void => {
        try {
            for (let form of vm.forms.selected) {
                await formService.create(form);
            }
            notify.success(idiom.translate('formulaire.success.forms.duplicate'));
            init();
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };

    vm.sendForm = async (): Promise<void> => {
        let nbQuestions = $scope.getDataIf200(await questionService.countQuestions(vm.forms.selected[0].id)).count;
        if (nbQuestions < 1) {
            notify.info(idiom.translate('formulaire.warning.send.form.empty'));
            return;
        }
        vm.forms.selected[0].generateRights();
        template.open('lightbox', 'lightbox/form-sending');
        vm.display.lightbox.sending = true;
        let checker = window.setInterval(function() {
            let sharePanel = document.getElementsByTagName('share-panel')[0];
            if (!!sharePanel) {
                clearInterval(checker);

                sharePanel.getElementsByTagName('h2')[0].textContent = idiom.translate('formulaire.sendTo');
                sharePanel.getElementsByClassName('panel-button')[0].textContent = idiom.translate('formulaire.send');
                let rows = sharePanel.getElementsByTagName('table')[0].rows;
                rows[0].cells[1].textContent = idiom.translate('formulaire.send');
                // for (let i = 0; i < rows.length; i++) {
                //     for (let j = 2; j < rows[i].cells.length - 1; j++) {
                //         rows[i].deleteCell(j);
                //     }
                // }
            }
        }, 200);
    };

    vm.closeSendFormLightbox = (): void => {
        template.close('lightbox');
        vm.display.lightbox.sending = false;
        window.setTimeout(async function () { await init(); }, 3000);
    };

    vm.shareForm = (): void => {
        //TODO : Lightbox pour confirmation du partage
        template.open('lightbox', 'lightbox/form-sharing');
        vm.display.lightbox.sharing = true;
    };

    vm.seeResultsForm = (): void => {
        // TODO display results d'un form
    };

    vm.exportForm = (): void => {
        window.open(window.location.pathname + `/export/${vm.forms.selected[0].id}`);
    };

    vm.restoreForms = async (): Promise<void> => {
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

    vm.archiveForms = (): void => {
        vm.display.warning = !!vm.forms.selected.find(form => form.sent === true);
        template.open('lightbox', 'lightbox/form-confirm-archive');
        vm.display.lightbox.archive = true;
    };

    vm.doArchiveForms = async (): Promise<void> => {
        try {
            for (let form of vm.forms.selected) {
                if ($scope.isStatusXXX(await formService.unshare(form.id), 200)) {
                    await formService.archive(form);
                }
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

    vm.deleteForms = (): void => {
        vm.display.warning = true;
        template.open('lightbox', 'lightbox/form-confirm-delete');
        vm.display.lightbox.delete = true;
    };

    vm.doDeleteForms = async (): Promise<void> => {
        try {
            for (let form of vm.forms.selected) {
                await formService.delete(form.id);
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

    init();
}]);