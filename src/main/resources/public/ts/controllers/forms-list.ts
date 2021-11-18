import {idiom, model, ng, notify, template, workspace} from 'entcore';
import {Distribution, Distributions, Form, Forms} from "../models";
import {distributionService, formService} from "../services";
import {FiltersFilters, FiltersOrders} from "../core/enums";
import {Mix} from "entcore-toolkit";
import {folderService} from "../services/FolderService";
import {Folder, Folders} from "../models/Folder";
import {Tree, Element} from "entcore/types/src/ts/workspace/model";
import {i18nUtils} from "../utils";

interface ViewModel {
    forms: Forms;
    distributions: Distributions;
    folders: Folders;
    folder: Folder;
    editedFolder: Folder;
    allFormsSelected: boolean;
    allFoldersSelected: boolean;
    targetFolderId: number;
    searchInput: string;
    mail: {
        link: string,
        subject: string,
        body: string
    };
    pageSize: number;
    limitTo: number;
    display: {
        grid: boolean,
        lightbox: {
            move: boolean,
            sending: boolean,
            sharing: boolean,
            archive: boolean,
            delete: boolean,
            reminder: boolean,
            folder: {
                create: boolean,
                rename: boolean
            }
        },
        warning: boolean
    };
    loading: boolean;
    folderTree: any;
    openedFolder: Element;
    selectedFolder: Element;

    initFolders() : Promise<void>;
    openFolder(folder: Folder) : void;
    openCreateFolder() : void;
    closeCreateFolder() : void;
    renameFolder() : void;
    doRenameFolder() : Promise<void>;
    closeRenameFolder() : void;
    moveItems() : void;
    doMoveItems() : Promise<void>;
    closeMoveItems() : void;
    deleteFolders() : void;
    doDeleteFolders() : Promise<void>;
    displayFolder(folder: Folder) : string;
    displayNbItems(folder: Folder) : string;
    dropped(dragEl, dropEl) : Promise<void>;
    switchAllFolders(value: boolean) : void;

    openForm(form: Form) : void;
    openPropertiesForm() : void;
    duplicateForms() : Promise<void>;
    shareForm() : void;
    closeShareFormLightbox() : void;
    seeResultsForm() : void;
    exportForm() : void;
    isFormOpened(): boolean;
    remind() : Promise<void>;
    doRemind() : Promise<void>;
    cancelRemind() : void;
    restoreForms() : Promise<void>;
    archiveForms() : void;
    doArchiveForms() : Promise<void>;
    deleteForms() : void;
    doDeleteForms() : Promise<void>;
    createFolder() : Promise<void>;
    selectItem(form: Form) : void;

    switchAllForms(value: boolean) : void;
    sort(field: FiltersOrders) : void;
    filter(filter: FiltersFilters) : void;
    openNavMyForms() : void;
    closeNavMyForms() : void;
    displayFilterName(name: string) : string;
    getTitle(title: string): string;
    infiniteScroll() : void;
}


export const formsListController = ng.controller('FormsListController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.forms = new Forms();
    vm.distributions = new Distributions();
    vm.searchInput = "";
    vm.editedFolder = new Folder();
    vm.allFormsSelected = false;
    vm.allFoldersSelected = false;
    vm.targetFolderId = 0;
    vm.mail = {
        link: "",
        subject: "",
        body: ""
    };
    vm.pageSize = 30;
    vm.limitTo = vm.pageSize;
    vm.display = {
        grid: true,
        lightbox: {
            move: false,
            sending: false,
            sharing: false,
            archive: false,
            delete: false,
            reminder: false,
            folder: {
                create: false,
                rename: false
            }
        },
        warning: false
    };
    vm.loading = true;
    vm.folderTree = {};
    vm.openedFolder = null;
    vm.selectedFolder = null;

    const init = async () : Promise<void> => {
        await vm.initFolders();
        vm.openFolder(vm.folder);

        vm.forms.filters.find(f => f.name === FiltersFilters.SENT).display = true;
        vm.forms.filters.find(f => f.name === FiltersFilters.SHARED).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.CREATION_DATE).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.MODIFICATION_DATE).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.TITLE).display = true;

        vm.loading = false;
        $scope.safeApply();
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
            await formService.duplicate(formIds, vm.folder.id);
            notify.success(idiom.translate('formulaire.success.forms.duplicate'));
            vm.openFolder(vm.folder);
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
        window.setTimeout(async function () {
            let contribs = document.querySelectorAll('[data-label="Contribuer"]');
            let gestions = document.querySelectorAll('[data-label="Gérer"]');
            for (let i = 1; i < contribs.length; i++) {
                let contribValue = contribs[i].children[0].children[0] as HTMLInputElement;
                let gestionValue = gestions[i].children[0].children[0] as HTMLInputElement;
                contribValue.addEventListener('change', (e) => {
                    let newValue = e.target as HTMLInputElement;
                    if (!newValue.checked && gestionValue.checked) {
                        gestionValue.checked = false;
                    }
                    $scope.safeApply();
                });
                gestionValue.addEventListener('change', (e) => {
                    let newValue = e.target as HTMLInputElement;
                    if (newValue.checked && !contribValue.checked) {
                        contribValue.checked = true;
                    }
                    $scope.safeApply();
                });
            }
        }, 500);
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

    vm.isFormOpened = () : boolean => {
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
            await formService.move(vm.forms.selected, 0);
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
            init(); // TODO need that ?
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };

    vm.selectItem = (item : any) : void => {
        item.selected = !item.selected;
        if (item instanceof Form) {
            vm.folders.deselectAll();
        }
        else if (item instanceof Folder) {
            vm.forms.deselectAll();
        }
    };


    // Folders

    vm.initFolders = async () : Promise<void> => {
        vm.folders = new Folders();
        await vm.folders.sync();
        vm.switchAllFolders(false);

        vm.folderTree = {
            cssTree: "folders-tree",
            get trees() {
                return vm.folders.trees;
            },
            isDisabled(folder) {
                if (vm.folders.selected.length > 0) {
                    let selectedIds : any = vm.folders.selected.map(f => f.id);
                    return selectedIds.includes(folder.id);
                }
                return false;
            },
            isOpenedFolder(folder) {
                if (vm.openedFolder === folder) {
                    return true;
                }
                else if ((folder as Tree).filter) {
                    if (!workspace.v2.service.isLazyMode()){
                        return true;
                    }
                }
                return vm.openedFolder && workspace.v2.service.findFolderInTreeByRefOrId(folder, vm.openedFolder);
            },
            isSelectedFolder(folder) {
                return vm.selectedFolder === folder;
            },
            openFolder(folder) {
                if (vm.folders.selected.length > 0) {
                    let selectedIds : any = vm.folders.selected.map(f => f.id);
                    if (!selectedIds.includes(folder.id)) {
                        vm.targetFolderId = folder.id;
                        vm.openedFolder = vm.selectedFolder = folder;
                    }
                }
                vm.targetFolderId = folder.id;
                vm.openedFolder = vm.selectedFolder = folder;

                if (!vm.display.lightbox.move) {
                    vm.openFolder(folder.data);
                }
            }
        }

        vm.openedFolder = vm.folders.trees[0];
        vm.selectedFolder = vm.folders.trees[0];
        vm.folder = vm.folders.myFormsFolder;

        $scope.safeApply();
    };

    vm.openFolder = async (folder: Folder) : Promise<void> => {
        let folderElem = vm.folders.getTreeElement(folder.id);
        vm.openedFolder = folderElem;
        vm.selectedFolder = folderElem;

        vm.folder = folder;
        vm.folder.children = vm.folders.getChildren(vm.folder.id);
        await vm.forms.sync();

        switch (vm.folder.id) {
            case 1:
                vm.forms.all = vm.forms.all.filter(form => !form.archived && form.owner_id === model.me.userId && form.folder_id === vm.folder.id);
                break;
            case 2:
                vm.forms.all = vm.forms.all.filter(form => !form.archived && form.collab && form.owner_id != model.me.userId);
                break;
            case 3:
                vm.forms.all = vm.forms.all.filter(form => form.archived);
                break;
            default:
                vm.forms.all = vm.forms.all.filter(form => !form.archived && form.folder_id === vm.folder.id);
                break;
        }
        vm.switchAllFolders(false);
        $scope.safeApply();
    };

    vm.openCreateFolder = () : void => {
        vm.display.lightbox.folder.create = true;
        template.open('lightbox', 'lightbox/folder-creation');
        vm.editedFolder = new Folder();
    };

    vm.closeCreateFolder = () : void => {
        vm.display.lightbox.folder.create = false;
        template.close('lightbox');
        vm.editedFolder = new Folder();
    };

    vm.createFolder = async () : Promise<void> => {
        vm.editedFolder.parent_id = vm.folder.id;
        await folderService.create(vm.editedFolder);
        await vm.folders.sync();
        vm.openFolder(vm.folder);
        vm.closeCreateFolder();
    };

    vm.renameFolder = () : void => {
        template.open('lightbox', 'lightbox/folder-rename');
        vm.display.lightbox.folder.rename = true;
        vm.editedFolder = vm.folders.selected[0];
    };

    vm.doRenameFolder = async () : Promise<void> => {
        try {
            await folderService.update(vm.editedFolder);
            vm.closeRenameFolder();
            notify.success(idiom.translate('formulaire.success.folders.update'));
            await vm.folders.sync();
            vm.openFolder(vm.folder);
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
        $scope.safeApply();
    };

    vm.closeRenameFolder = () : void => {
        vm.display.lightbox.folder.rename = false;
        template.close('lightbox');
        vm.editedFolder = new Folder();
    };

    vm.moveItems = () : void => {
        vm.openedFolder = vm.folders.trees[0];
        vm.selectedFolder = vm.folders.trees[0];
        delete vm.folders.trees[1].name;
        delete vm.folders.trees[2].name;
        template.open('lightbox', 'lightbox/item-move');
        vm.display.lightbox.move = true;
    };

    vm.doMoveItems = async () : Promise<void> => {
        try {
            if (vm.folders.selected.length > 0) {
                await folderService.move(vm.folders.selected, vm.targetFolderId);
            }
            else {
                await formService.move(vm.forms.selected, vm.targetFolderId);
            }
            vm.display.lightbox.move = false;
            template.close('lightbox');
            notify.success(idiom.translate('formulaire.success.move'));
            await vm.folders.sync();
            vm.openFolder(vm.folder);
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
        $scope.safeApply();
    };

    vm.closeMoveItems = () : void => {
        vm.display.lightbox.move = false;
        template.close('lightbox');
        vm.targetFolderId = vm.folders.trees[0].children[0].id;
    };

    vm.deleteFolders = () : void => {
        vm.display.warning = true;
        template.open('lightbox', 'lightbox/folder-confirm-delete');
        vm.display.lightbox.delete = true;
    };

    vm.doDeleteFolders = async () : Promise<void> => {
        try {
            await folderService.delete(vm.folders.selected);
            template.close('lightbox');
            vm.display.lightbox.delete = false;
            vm.display.warning = false;
            notify.success(idiom.translate('formulaire.success.folders.delete'));
            await vm.folders.sync();
            vm.openFolder(vm.folder);
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };

    vm.displayFolder = (folder) : string => {
        return idiom.translate(folder.name);
    };

    vm.displayNbItems = (folder) : string => {
        if (folder.nb_folder_children + folder.nb_form_children <= 0) {
            return idiom.translate('formulaire.folder.empty');
        }
        else {
            let params = [folder.nb_folder_children.toString(), folder.nb_form_children.toString()];
            return i18nUtils.getWithParams("formulaire.folder.nbItems", params);
        }
    };

    vm.dropped = async (dragged, target) : Promise<void> => {
        if (dragged == target) return;

        let originalItem = $('#' + dragged);
        let targetItem = $('#' + target);
        let idOriginalItem = parseInt(originalItem[0].children[0].textContent);
        let idTargetItem = parseInt(targetItem[0].children[0].textContent);
        let typeOriginalItem = originalItem[0].classList[0];

        if (typeOriginalItem == "folder") {
            let draggedItems = vm.folders.all.filter(f => f.id === idOriginalItem);
            await folderService.move(draggedItems, idTargetItem ? idTargetItem : 1);
        }
        else if (typeOriginalItem == "form") {
            let draggedItems = vm.forms.all.filter(f => f.id === idOriginalItem);
            await formService.move(draggedItems, idTargetItem ? idTargetItem : 1);
        }
        await vm.folders.sync();
        await vm.openFolder(vm.folder);
    };

    vm.switchAllFolders = (value: boolean) : void => {
        value ? vm.folders.selectAll() : vm.folders.deselectAll();
        vm.allFoldersSelected = value;
    };


    // Utils

    vm.switchAllForms = (value) : void => {
        value ? vm.forms.selectAll() : vm.forms.deselectAll();
        vm.allFormsSelected = value;
    };

    vm.sort = (field) : void => {
        vm.forms.orderByField(field);
        vm.forms.orderForms();
        $scope.safeApply();
    };

    vm.filter = (filter) : void => {
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

    vm.displayFilterName = (name) : string => {
        return idiom.translate("formulaire.filter." + name.toLowerCase());
    };

    vm.getTitle = (title) : string => {
        return idiom.translate('formulaire.' + title);
    };

    vm.infiniteScroll = () : void => {
        vm.limitTo += vm.pageSize;
    };

    const initMail = () : void => {
        vm.mail.link = `${window.location.origin}${window.location.pathname}#/form/${vm.forms.selected[0].id}`;
        vm.mail.subject = idiom.translate('formulaire.remind.default.subject');
        vm.mail.body = i18nUtils.getWithParams('formulaire.remind.default.body', [vm.mail.link, vm.mail.link]);
    };

    init();
}]);