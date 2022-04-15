import {angular, idiom, model, ng, notify, template} from 'entcore';
import {
    Distribution,
    Distributions,
    DistributionStatus,
    Draggable,
    Folder,
    Folders,
    Form,
    Forms,
    Question, Questions, Section, Sections
} from "../models";
import {distributionService, formElementService, formService, questionService, sectionService} from "../services";
import {FiltersFilters, FiltersOrders, FORMULAIRE_EMIT_EVENT} from "../core/enums";
import {Mix} from "entcore-toolkit";
import {folderService} from "../services/FolderService";
import {Element} from "entcore/types/src/ts/workspace/model";
import {I18nUtils} from "../utils";
import {sectionItem} from "../directives";

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
    responders: any[];
    mail: {
        link: string,
        subject: string,
        body: string
    };
    remindfilter:{
        answered : boolean,
        notanswered : boolean,
    }
    sortRemindlist: {
        filter: string,
        order: boolean
    }
    pageSize: number;
    tableSize:number;
    limitTo: number;
    limitTable:number;
    display: {
        grid: boolean,
        lightbox: {
            move: boolean,
            sending: boolean,
            sharing: boolean,
            archive: boolean,
            delete: boolean,
            reminder: boolean,
            checkremind:boolean,
            folder: {
                create: boolean,
                rename: boolean
            }
        },
        warning: boolean
    };
    loading: boolean;
    folderTree: any;
    openedFoldersIds: number[];
    selectedFolder: Element;
    draggable : Draggable;
    draggedItem : any;

    createForm() : void;

    // Toaster functions
    openForm(form: Form) : void;
    openPropertiesForm() : void;
    duplicateForms() : Promise<void>;
    shareForm() : void;
    closeShareFormLightbox() : void;
    seeResultsForm() : void;
    exportForm() : void;
    isFormOpened(): boolean;
    checkRemind() : Promise<void>;
    closeCheckRemind() : Promise<void>;
    remind() : Promise<void>;
    doRemind() : Promise<void>;
    filterResponses() : any;
    cancelRemind() : void;
    restoreForms() : Promise<void>;
    archiveForms() : void;
    doArchiveForms() : Promise<void>;
    deleteForms() : void;
    doDeleteForms() : Promise<void>;
    createFolder() : Promise<void>;
    selectItem(form: Form) : void;

    // Folders functions
    initDragAndDrop() : void;
    initFolders() : Promise<void>;
    openFolder(folder: Folder, resync?: boolean) : void;
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
    displayNbItems(folder: Folder) : string;
    switchAllFolders(value: boolean) : void;

    $onInit() : Promise<void>;
    switchAllForms(value: boolean) : void;
    sort(field: FiltersOrders) : void;
    filter(filter: FiltersFilters) : void;
    openNavMyForms() : void;
    closeNavMyForms() : void;
    displayFilterName(name: string) : string;
    getTitle(title: string) : string;
    infiniteScroll() : void;
    seeMore() : void;
    $onDestroy() : Promise<void>;
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
    vm.responders = [];
    vm.mail = {
        link: "",
        subject: "",
        body: ""
    };
    vm.remindfilter = {
      answered:false,
      notanswered:true

    };
    vm.sortRemindlist = {
        filter: 'name',
        order: true
    }
    vm.pageSize = 30;
    vm.tableSize = 10;
    vm.limitTo = vm.pageSize;
    vm.limitTable = vm.tableSize;
    vm.display = {
        grid: true,
        lightbox: {
            move: false,
            sending: false,
            sharing: false,
            archive: false,
            delete: false,
            reminder: false,
            checkremind:false,
            folder: {
                create: false,
                rename: false
            }
        },
        warning: false
    };
    vm.loading = true;
    vm.folderTree = {};
    vm.openedFoldersIds = null;
    vm.selectedFolder = null;

    vm.$onInit = async () : Promise<void> => {
        await vm.initFolders();
        vm.openFolder(vm.folder);
        vm.forms.filters.find(f => f.name === FiltersFilters.SENT).display = true;
        vm.forms.filters.find(f => f.name === FiltersFilters.SHARED).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.CREATION_DATE).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.MODIFICATION_DATE).display = true;
        vm.forms.orders.find(o => o.name === FiltersOrders.TITLE).display = true;
        (window as any).LAZY_MODE = false;
        vm.loading = false;
        vm.initDragAndDrop();
        $scope.safeApply();
    };

    vm.createForm = () : void => {
        let isFolderOkForCreation = vm.folder.id && vm.folder.id != vm.folders.sharedFormsFolder.id && vm.folder.id != vm.folders.archivedFormsFolder.id;
        let folder = isFolderOkForCreation ? vm.folder : vm.folders.myFormsFolder;
        $scope.$emit(FORMULAIRE_EMIT_EVENT.UPDATE_FOLDER, folder);
        $scope.safeApply();
        $scope.redirectTo(`/form/create`);
    };

    // Toaster

    vm.openForm = (form: Form) : void => {
        let data = {
            path: `/form/${form.id}/edit`,
            form: form
        };
        $scope.$emit(FORMULAIRE_EMIT_EVENT.REDIRECT, data);
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
                if(vm.forms.selected) {
                    formIds.push(form.id);
                }
            }
            let targetFolderId = vm.folder.id === vm.folders.sharedFormsFolder.id ? vm.folders.myFormsFolder.id : vm.folder.id;
            await formService.duplicate(formIds, targetFolderId);
            notify.success(idiom.translate('formulaire.success.forms.duplicate'));
            vm.openFolder(vm.folder);
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };

    vm.shareForm = async() : Promise<void> => {
        let questions =  Mix.castArrayAs(Question, await questionService.list(vm.forms.selected[0].id));
        let sections = new Sections();
        await sections.sync(vm.forms.selected[0].id);

        let questionsList = sections.all.map(s => s.questions.all);

        for(let questions of questionsList){
            let conditionalQuestions = questions.filter(q => q.conditional);
            let wrongQuestions = questions.filter(question => !question.title);

            if (conditionalQuestions.length >= 2) {
                notify.error(idiom.translate('formulaire.element.block.sharing.multiple.conditional'));
                return;
            }
            if (wrongQuestions.length > 0){
                notify.error(idiom.translate('formulaire.block.sharing'));
                return;
            }
        }

        let wrongQuestions = questions.filter(question => !question.title);
        if (wrongQuestions.length > 0){
            notify.error(idiom.translate('formulaire.block.sharing'));
            return;
        }

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
        window.setTimeout(async function () { await vm.openFolder(vm.folder, false); }, 100);
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
        let dateEndingOk = (!vm.forms.selected[0].date_ending || vm.forms.selected[0].date_ending > new Date());
        return dateOpeningOk && dateEndingOk;
    };

    vm.checkRemind = async () : Promise<void> => {
        let distributions = Mix.castArrayAs(Distribution, await distributionService.listByForm(vm.forms.selected[0].id));
        let uniqueDistribs : any = [];
        for (let d of distributions){
            if(!uniqueDistribs.map(d => d.responder_id).includes(d.responder_id)) {
                uniqueDistribs.push(d);
            }
        }
        vm.responders = [];
        for (let uniqueDistribution of uniqueDistribs){
            let str = uniqueDistribution.responder_name;
            let seperate = str.split(' ');
            let responderInfo = {
                name : seperate[0],
                surname: seperate[1],
                nbResponses: distributions.filter(d => d.responder_id === uniqueDistribution.responder_id && d.status==DistributionStatus.FINISHED).length,
            };
            vm.responders.push(responderInfo);
        }
        template.open('lightbox','lightbox/form-check-remind');
        vm.display.lightbox.checkremind=true;
    };

    vm.closeCheckRemind = async () : Promise<void> => {
        vm.display.lightbox.checkremind = false;
        template.close('lightbox');
        vm.limitTable= vm.tableSize;
    };

    vm.remind = async () : Promise<void> => {
        initMail();
        vm.distributions.all = Mix.castArrayAs(Distribution, await distributionService.listByForm(vm.forms.selected[0].id));
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
        await formService.sendReminder(vm.forms.selected[0].id, vm.mail);
        notify.success(idiom.translate('formulaire.success.reminder.send'));
        initMail();
        template.close('lightbox');
        vm.display.lightbox.reminder = false;
        window.setTimeout(async function () { await vm.$onInit(); }, 100);
    };

    vm.filterResponses = () : any => {
        if (vm.remindfilter.answered && !vm.remindfilter.notanswered) {
            return vm.responders.filter(a => a.nbResponses > 0);
        }
        else if(!vm.remindfilter.answered && vm.remindfilter.notanswered) {
            return vm.responders.filter(a => a.nbResponses <= 0);
        }
        else if (!vm.remindfilter.answered && !vm.remindfilter.notanswered) {
            return vm.responders;
        }
        else if(vm.remindfilter.answered && vm.remindfilter.notanswered) {
            return vm.responders;
        }
    }

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
                await formService.restore(form, vm.folders.myFormsFolder.id);
            }
            await formService.move(vm.forms.selected, vm.folders.myFormsFolder.id);
            template.close('lightbox');
            notify.success(idiom.translate('formulaire.success.forms.restore'));
            vm.openFolder(vm.folder, false);
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };

    vm.archiveForms = () : void => {
        vm.display.warning = vm.forms.selected.some(form => form.sent === true);
        template.open('lightbox', 'lightbox/form-confirm-archive');
        vm.display.lightbox.archive = true;
    };

    vm.doArchiveForms = async () : Promise<void> => {
        try {
            for (let form of vm.forms.selected) {
                await formService.archive(form, vm.folders.archivedFormsFolder.id);
            }
            template.close('lightbox');
            vm.display.lightbox.archive = false;
            vm.display.warning = false;
            notify.success(idiom.translate('formulaire.success.forms.archive'));
            vm.openFolder(vm.folder);
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
                await formService.delete(form.id);
            }
            template.close('lightbox');
            vm.display.lightbox.delete = false;
            vm.display.warning = false;
            notify.success(idiom.translate('formulaire.success.forms.delete'));
            vm.openFolder(vm.folder, false);
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

    vm.initDragAndDrop = () : void => {
        vm.draggable = {
            dragConditionHandler(event: DragEvent, content?: any): boolean { return true; },
            dragStartHandler(event: DragEvent, content?: any): void {
                try {
                    event.dataTransfer.setData('application/json', JSON.stringify(content));
                    vm.draggedItem = content;
                } catch (e) {
                    event.dataTransfer.setData('text', JSON.stringify(content));
                }
            },
            dragEndHandler(event: DragEvent, content?: any): void {},
            dropConditionHandler(event: DragEvent, content?: any): boolean {
                let folderTarget = angular.element(event.srcElement).scope().folder;
                let isTargetAlreadyMyParent = folderTarget && folderTarget.id === vm.folder.id;

                // Check if target is self or already parent
                let isTargetMyself = vm.draggedItem && folderTarget && folderTarget.id === vm.draggedItem.id;
                let isTargetSelected = vm.folders.selected.filter(f => f.id === folderTarget.id).length > 0;

                // Check if dragged item is in myForms folder
                let isArchivedItem = vm.draggedItem.archived;
                let isSharedItem = vm.draggedItem.collab && vm.draggedItem.owner_id != model.me.userId;
                let isItemInMyForms = !isArchivedItem && !isSharedItem;

                // Check if target is in myForms folder
                let isTargetSharedFolder = folderTarget.id === vm.folders.sharedFormsFolder.id;
                let isTargetArchivedFolder = folderTarget.id === vm.folders.archivedFormsFolder.id;
                let isTargetInMyForms = !isTargetSharedFolder && !isTargetArchivedFolder;

                return !isTargetAlreadyMyParent && !isTargetMyself && !isTargetSelected && isItemInMyForms && isTargetInMyForms;
            },
            async dragDropHandler(event: DragEvent, content?: any): Promise<void> {
                let originalItem = JSON.parse(event.dataTransfer.getData("application/json"));
                let targetItem = angular.element(event.srcElement).scope().folder;

                let idOriginalItem = originalItem.id;
                let idTargetItem = targetItem.id;

                if (vm.forms.selected.length > 0) { // Move several forms
                    await formService.move(vm.forms.selected, idTargetItem ? idTargetItem : 1);
                }
                else if (vm.folders.selected.length > 0) { // Move several folders
                    await folderService.move(vm.folders.selected, idTargetItem ? idTargetItem : 1);
                }
                else {
                    let isForm = !!originalItem.folder_id;
                    if (isForm) { // Move one form
                        let draggedItem = vm.forms.all.filter(f => f.id === idOriginalItem);
                        await formService.move(draggedItem, idTargetItem ? idTargetItem : 1);
                    }
                    else if (!isForm) { // Move one folder
                        let draggedItem = vm.folders.all.filter(f => f.id === idOriginalItem);
                        await folderService.move(draggedItem, idTargetItem ? idTargetItem : 1);
                    }
                }
                await vm.openFolder(vm.folder);
            }
        };
    };

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
                if (vm.display.lightbox.move) {
                    let selectedIds : any = vm.folders.selected.map(f => f.id);
                    return selectedIds.includes(folder.id);
                }
                return false;
            },
            isOpenedFolder(folder) {
                return vm.openedFoldersIds && vm.openedFoldersIds.filter(id => id === folder.id).length > 0;
            },
            isSelectedFolder(folder) {
                return vm.selectedFolder === folder;
            },
            openFolder(folder) {
                if (!vm.folderTree.isDisabled(folder)) {
                    vm.targetFolderId = folder.id;
                    vm.selectedFolder = folder;

                    let indexInOpenedFolders = vm.openedFoldersIds.indexOf(folder.id);
                    if (indexInOpenedFolders > 0) {
                        vm.openedFoldersIds.splice(indexInOpenedFolders, 1);
                    }
                    else {
                        vm.openedFoldersIds.push(folder.id);
                    }

                    if (!vm.display.lightbox.move) {
                        vm.openFolder(folder.data);
                    }
                }
            }
        }
        vm.targetFolderId = vm.folders.trees[0].id;
        vm.openedFoldersIds = [vm.targetFolderId];
        vm.selectedFolder = vm.folders.trees[0];
        vm.folder = $scope.folder.id ? $scope.folder : vm.folders.myFormsFolder;
        $scope.safeApply();
    };

    vm.openFolder = async (folder: Folder, resync: boolean = true) : Promise<void> => {
        if (resync) await vm.folders.sync();
        vm.selectedFolder = vm.folders.getTreeElement(folder.id);
        vm.folder = folder;
        $scope.folder = folder;
        vm.folder.children = vm.folders.getChildren(vm.folder.id);
        await vm.forms.sync();
        switch (vm.folder.id) {
            case vm.folders.myFormsFolder.id:
                vm.forms.all = vm.forms.all.filter(form => !form.archived && form.owner_id === model.me.userId && form.folder_id === vm.folder.id);
                break;
            case vm.folders.sharedFormsFolder.id:
                vm.forms.all = vm.forms.all.filter(form => !form.archived && form.collab && form.owner_id != model.me.userId);
                break;
            case vm.folders.archivedFormsFolder.id:
                vm.forms.all = vm.forms.all.filter(form => form.archived);
                break;
            default:
                vm.forms.all = vm.forms.all.filter(form => !form.archived && form.folder_id === vm.folder.id);
                break;
        }
        vm.switchAllFolders(false);
        $scope.$emit(FORMULAIRE_EMIT_EVENT.UPDATE_FOLDER, vm.folder);
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
        let isFolderOkForCreation = vm.folder.id && vm.folder.id != vm.folders.sharedFormsFolder.id && vm.folder.id != vm.folders.archivedFormsFolder.id;
        vm.editedFolder.parent_id = isFolderOkForCreation ? vm.folder.id : vm.folders.myFormsFolder.id;
        await folderService.create(vm.editedFolder);
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
            vm.openFolder(vm.folder); // TODO maybe just a simple vm.folders.sync() ?
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
        vm.targetFolderId = vm.folders.trees[0].id;
        vm.openedFoldersIds = [vm.targetFolderId];
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
            else{
                await formService.move(vm.forms.selected, vm.targetFolderId);
            }
            vm.display.lightbox.move = false;
            template.close('lightbox');
            notify.success(idiom.translate('formulaire.success.move'));
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
            vm.openFolder(vm.folder);
            $scope.safeApply();
        }
        catch (e) {
            throw e;
        }
    };

    vm.displayNbItems = (folder) : string => {
        if (folder.nb_folder_children + folder.nb_form_children <= 0) {
            return idiom.translate('formulaire.folder.empty');
        }
        else {
            let params = [folder.nb_folder_children.toString(), folder.nb_form_children.toString()];
            return I18nUtils.getWithParams("formulaire.folder.nbItems", params);
        }
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

    vm.seeMore = () :void =>{
        vm.limitTable += vm.tableSize;
    };

    const initMail = () : void => {
        let endPath = vm.forms.selected[0].rgpd ? 'rgpd' : 'new';
        vm.mail.link = `${window.location.origin}${window.location.pathname}#/form/${vm.forms.selected[0].id}/${endPath}`;
        vm.mail.subject = idiom.translate('formulaire.remind.default.subject');
        vm.mail.body = I18nUtils.getWithParams('formulaire.remind.default.body', [vm.mail.link, vm.mail.link]);
    };

    vm.$onDestroy = async () : Promise<void> => {
        (window as any).LAZY_MODE = true;
        $scope.safeApply();
    }

}]);