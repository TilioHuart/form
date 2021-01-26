import {Mix, Selectable, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {formService} from "../services";

export class Form implements Selectable  {
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
}

export class Forms extends Selection<Form> {
    constructor() {
        super([]);
    }

    async sync () {
        try {
            let { data } = await formService.list();
            this.all = Mix.castArrayAs(Form, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.form.sync'));
            throw e;
        }
    }

    async syncSent () {
        try {
            let { data } = await formService.listSentForms();
            this.all = Mix.castArrayAs(Form, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.form.sync'));
            throw e;
        }
    }
}