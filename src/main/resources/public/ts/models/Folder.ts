import {Selectable, Selection} from "entcore-toolkit";
import {Mix} from 'entcore-toolkit';
import {folderService} from "../services/FolderService";
import {idiom, workspace} from "entcore";
import {Element} from "entcore/types/src/ts/workspace/model";

export class Folder implements Selectable {
    id: number;
    parent_id: number;
    name: string;
    user_id: string;
    nb_folder_children: number;
    nb_form_children: number;
    selected: boolean;
    children: any;

    constructor(id?: number, name?: string) {
        this.id = id != null ? id : null;
        this.parent_id = null;
        this.name = name ? name : null;
        this.user_id = null;
        this.nb_folder_children = 0;
        this.nb_form_children = 0;
        this.selected = null;
        this.children = [];
    };

    toJson() : Object {
        return {
            parent_id : this.parent_id,
            name : this.name,
            user_id : this.user_id,
            nb_folder_children: this.nb_folder_children,
            nbFormChildren: this.nb_form_children
        }
    };
}

export class Folders extends Selection<Folder> {
    all: Folder[];
    myFormsFolder?: Folder;
    sharedFormsFolder?: Folder;
    archivedFormsFolder?: Folder;
    trees: any[];

    constructor() {
        super([]);
        this.all = [];
        this.trees = [];
    };

    async sync (setDefaultFolders: boolean = true) {
        try {
            let data = await folderService.list();
            this.all = Mix.castArrayAs(Folder, data);
            this.setTree();
            if (setDefaultFolders) this.setDefaultFolders();
        } catch (e) {
            throw e;
        }
    };

    setDefaultFolders = () : void => {
        this.myFormsFolder = new Folder(1, idiom.translate("formulaire.forms.mine"));
        this.sharedFormsFolder = new Folder(2, idiom.translate("formulaire.forms.shared"));
        this.archivedFormsFolder = new Folder(3, idiom.translate("formulaire.forms.archived"));

        let localTree = this.trees;
        this.trees = [];
        let mineElem = new workspace.v2.models.Element(this.myFormsFolder);
        let sharedElem = new workspace.v2.models.Element(this.sharedFormsFolder);
        let archivedElem = new workspace.v2.models.Element(this.archivedFormsFolder);
        mineElem.children = localTree;
        this.trees.push(mineElem, sharedElem, archivedElem);
    };

    setTree = () : void => {
        if (this.all) {
            let lookup = {};
            let elements = [];

            // Create a copy of folders but as entcore Elements and fill lookup json
            for (let folder of this.all) {
                let elem = new workspace.v2.models.Element(folder);
                elements.push(elem);
                lookup[folder.id] = elem;
            }
            let filter = elements.sort((e1, e2) => e1.name < e2.name ? -1 : 1);
            // Use the lookup to order children
            for (let elem of filter) {
                if (elem.data.parent_id !== null && lookup[elem.data.parent_id]) {
                    lookup[elem.data.parent_id].children.push(elem);
                }
            }

            // Push root elements into final array
            this.trees = [];


            for (let key in filter) {
                let elem = filter[key];
                if (filter[key].data.parent_id === null || !lookup[elem.data.parent_id]) {
                    this.trees.push(elem);
                }
            }
        }
    };

    getChildren = (folderId: number) : Folder[] => {
        if (this.all) {
            return this.all.filter(f => f.parent_id == folderId);
        }
        return [];
    };

    getTreeElement = (folderId: number) : Element => {
        let elem = null;
        for (let tree of this.trees) {
            elem = !elem ? this.getElementByTree(folderId, tree) : elem;
        }
        return elem;
    };

    getElementByTree = (folderId: number, elem = this.trees[0]) : Element => {
        if (elem.id === folderId) {
            return elem;
        }
        else if (elem.children.length && elem.children.length > 0) {
            let result = null;
            let i = 0;
            while (!result && i < elem.children.length) {
                result = this.getElementByTree(folderId, elem.children[i]);
                i++;
            }
            return result;
        }
        return null;
    };

    // getParentFolder(): Folder[] {
    //     if (this.all) {
    //         return this.all.filter(folder => folder.parent_id === 0);
    //     }
    //     return [];
    // };

    // getFoldersToShow(currentFolder, searching): Folder[] {
    //     currentFolder = this.listOfSubfolder.filter(folder => folder.id == currentFolder);
    //     if (currentFolder && currentFolder[0]) {
    //         if (searching !== null && searching !== undefined && searching !== '') {
    //             if (currentFolder[0].id == 0) {
    //                 return this.all.filter(folder => folder.name.toLowerCase().includes(searching.toLowerCase()));
    //             } else {
    //                 let subfolderSearch = [];
    //                 for (let i = 0; i < currentFolder[0].subfolder.length; i++) {
    //                     subfolderSearch.push(currentFolder[0].subfolder[i]);
    //                     if (currentFolder[0].subfolder[i].subfolder.length != 0) {
    //                         subfolderSearch.push(...this.getFoldersToShow(currentFolder[0].subfolder[i].id, searching));
    //                     }
    //                 }
    //                 return subfolderSearch.filter(folder => folder.name.toLowerCase().includes(searching.toLowerCase()));
    //             }
    //         } else if (currentFolder[0].id == 0) {
    //             return this.getParentFolder();
    //         } else {
    //             return this.getSubfolder(currentFolder[0].id);
    //         }
    //     }
    // };

    // getAllSubfolder() {
    //     if (this.all) {
    //         this.listOfSubfolder.length = 0;
    //         this.listOfSubfolder.push(this.myForms);
    //         this.getParentFolder().forEach(folder => {
    //             if (!folder.select) {
    //                 this.listOfSubfolder.push(folder);
    //                 folder.subfolder = this.getSubfolder(folder.id);
    //                 this.insertSubfolder(folder);
    //             }
    //         });
    //     }
    // };

    // insertSubfolder(folder: Folder) {
    //     if (folder.subfolder && folder.subfolder.length) {
    //         this.getSubfolder(folder.id).forEach(subfolder => {
    //             if (!(subfolder.select)) {
    //                 this.listOfSubfolder.push(subfolder);
    //                 subfolder.subfolder = this.getSubfolder(subfolder.id);
    //                 this.insertSubfolder(subfolder);
    //             }
    //         });
    //     }
    // };
}