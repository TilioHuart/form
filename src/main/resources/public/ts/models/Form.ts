import {Mix, Selectable, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {formService} from "../services";

export class Form implements Selectable {
    id: number;
    title: string;
    description: string;
    picture: string;
    owner_id: string;
    owner_name: string;
    created: Date;
    modified: Date;
    sent: boolean;
    shared: boolean;
    archived: boolean; // TODO: comment on gère les form archivé ? Autre table dans bdd ou simple prop comme ici ?
    selected: boolean;

    constructor() {
        this.id = null;
        this.title = null;
        this.description = null;
        this.picture = null;
        this.owner_id = null;
        this.owner_name = null;
        this.created = null;
        this.modified = null;
        this.sent = null;
        this.shared = null;
        this.archived = null; // TODO
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
            created: this.created,
            modified: this.modified,
            sent: this.sent,
            shared: this.shared,
            archived: this.archived, // TODO
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
}