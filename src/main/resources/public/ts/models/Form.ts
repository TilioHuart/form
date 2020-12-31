import {Mix, Selectable, Selection} from "entcore-toolkit";
import notify from "entcore";
import http from "axios";

export class Form implements Selectable {

    owner_id: string;
    owner_name: string;
    title: string;
    description: string;
    picture: string;
    created: Date;
    modified: Date;
    sent: boolean;
    shared: boolean;
    selected: boolean;

    constructor() {
        this.owner_id = null;
        this.owner_name = null;
        this.title = null;
        this.description = null;
        this.picture = null;
        this.created = null;
        this.modified = null;
        this.sent = null;
        this.shared = null;
        this.selected = null;
    }

    toJson(): Object {
        return {
            owner_id: this.owner_id,
            owner_name: this.owner_name,
            title: this.title,
            description: this.description,
            picture: this.picture,
            created: this.created,
            modified: this.modified,
            sent: this.sent,
            shared: this.shared,
            selected: this.selected
        }
    }
}

export class Forms extends Selection<Form> {
    publicBankCategoryId: number;
    deleteCategoryId: number;
    allByFolder: Form[];
    formsShared: Form[];
    formsPublished: Form[];
    formsSharedToFollow: Form[];
    formsByUser: Form[];
    isSynchronized: boolean;
    allForms: Form[];
    lastCreation: boolean;
    toDo: boolean;
    toCome: boolean;
    formsToDoSort: any;
    formsToComeSort: any;
    formsToDo: Form[];
    formsToCome: Form[];
    formsMyForms: Form[];
    showForms: Form[];
    folderId: number;
    categoryType: String;
    searchInput: any;
    order: any;
    typeShow: any;

    constructor() {
        super([]);
    }

    async sync () {
        try {
            let { data } = await http.get(`/formulaire/forms`);
            this.allForms = Mix.castArrayAs(Form, data);
        } catch (e) {
            notify.error('formulaire.error');
        }
    }
}