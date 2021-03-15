import {idiom, model, ng, notify, template} from 'entcore';
import {Form, Forms, QuestionChoice, Questions, Types} from "../models";
import {formService, questionService, questionChoiceService} from "../services";

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

    openFolder(folderName : string): void;
    switchAll(value : boolean): void;
    sort() : void;
    filter() : void;
    displayFolder(): string;
    checkOpenButton(): boolean;
    openForm(form : Form): void;
    openPropertiesForm(): void;
    duplicateForms(): Promise<void>;
    shareForm(): void;
    closeShareFormLightbox(): void;
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
            case "shared": vm.forms.all = vm.forms.all.filter(form => form.collab === true && form.owner_id != model.me.userId); break;
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
        vm.allFormsSelected = value;
    };

    vm.sort = () : void => {
        vm.forms.orderForms();
        $scope.safeApply();
    };

    vm.filter = () : void => {
        vm.forms.filterForms();
        $scope.safeApply();
    }

    // Display functions

    vm.displayFolder = () : string => {
        return idiom.translate("formulaire.forms." + vm.folder);
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

    vm.duplicateForms = async (): Promise<void> => {
        try {
            for (let form of vm.forms.selected) {
                let duplicata = form;
                duplicata.title += " - copie";
                duplicata = $scope.getDataIf200(await formService.create(duplicata));
                let questions = new Questions();
                await questions.sync(form.id);
                for (let question of questions.all) {
                    question.form_id = duplicata.id;
                    let newQuestion = $scope.getDataIf200(await questionService.create(question));
                    if (question.question_type === Types.SINGLEANSWER || question.question_type === Types.MULTIPLEANSWER) {
                        for (let choice of question.choices.all) {
                            if (!!choice.value) {
                                await questionChoiceService.create(new QuestionChoice(newQuestion.id, choice.value));
                            }
                        }
                    }
                }
            }
            notify.success(idiom.translate('formulaire.success.forms.duplicate'));
            init();
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };

    vm.shareForm = (): void => {
        if (!isFormEmpty(vm.forms.selected[0].id)) return;
        vm.forms.selected[0].generateShareRights();
        template.open('lightbox', 'lightbox/form-sharing');
        vm.display.lightbox.sharing = true;
    };

    vm.closeShareFormLightbox = (): void => {
        template.close('lightbox');
        vm.display.lightbox.sharing = false;
        window.setTimeout(async function () { await init(); }, 100);
    };

    vm.seeResultsForm = (): void => {
        $scope.redirectTo(`/form/${vm.forms.selected[0].id}/results/1`);
        $scope.safeApply();
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

    vm.deleteForms = (): void => {
        vm.display.warning = true;
        template.open('lightbox', 'lightbox/form-confirm-delete');
        vm.display.lightbox.delete = true;
    };

    vm.doDeleteForms = async (): Promise<void> => {
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

    const isFormEmpty = async (formId: number) : Promise<boolean> => {
        let nbQuestions = $scope.getDataIf200(await questionService.countQuestions(formId)).count;
        if (nbQuestions < 1) {
            notify.info(idiom.translate('formulaire.warning.send.form.empty'));
            return true;
        }
        return false;
    };

    init();
}]);