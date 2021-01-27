import {Mix, Selectable, Selection} from "entcore-toolkit";
import {idiom, notify, Rights, Shareable} from "entcore";
import {formService} from "../services";

export class Form implements Selectable, Shareable  {
    shared: any;
    owner: { userId: string; displayName: string };
    rights: Rights<Form>;
    _id: number;

    id: number;
    title: string;
    description: string;
    picture: string;
    owner_id: string;
    owner_name: string;
    date_creation: Date;
    date_modification: Date;
    sent: boolean;
    collab: boolean;
    archived: boolean;
    selected: boolean;

    constructor() {
        this.id = null;
        this.title = null;
        this.description = null;
        this.picture = null;
        this.owner_id = null;
        this.owner_name = null;
        this.date_creation = null;
        this.date_modification = null;
        this.sent = false;
        this.collab = false;
        this.archived = false;
        this.selected = null;
    }

    toJson(): Object {
        return {
            id: this.id,
            title: this.title,
            description: this.description,
            picture: this.picture,
            owner_id: this.owner_id,
            owner_name: this.owner_name,
            date_creation: this.date_creation,
            date_modification: this.date_modification,
            sent: this.sent,
            collab: this.collab,
            archived: this.archived,
            selected: this.selected
        }
    }

    get myRights(){
        return this.rights.myRights;
    }

    setFromJson(data: any) {
        for (let key in data) {
            this[key] = data[key];
        }
        this._id = this.id;
        this.owner = { userId: this.owner_id, displayName: this.owner_name };
        this.rights = new Rights<Form>(this);
    }
}

export class Forms extends Selection<Form> {
    all: Form[];

    constructor() {
        super([]);
    }

    async sync () : Promise<void> {
        this.all = [];
        try {
            let { data } = await formService.list();
            for (let i = 0; i < data.length; i++) {
                let tempForm = new Form();
                tempForm.setFromJson(data[i]);
                this.all.push(tempForm);
            }
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.form.sync'));
            throw e;
        }
    }

    async syncSent () : Promise<void> {
        try {
            let { data } = await formService.listSentForms();
            this.all = Mix.castArrayAs(Form, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.form.sync'));
            throw e;
        }
    }
}