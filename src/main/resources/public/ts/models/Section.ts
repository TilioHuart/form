import {Mix} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {sectionService} from "../services";

export class Section {
    id: number;
    title: string;
    description: string;
    form_id: number;
    position: number;

    constructor() {
        this.id = null;
        this.title = null;
        this.description = null;
        this.form_id = null;
        this.position = null;
    }

    toJson() : Object {
        return {
            id: this.id,
            title: this.title,
            description: this.description,
            form_id: this.form_id,
            position: this.position
        }
    }
}

export class Sections {
    all: Section[];

    constructor() {
        this.all = [];
    }

    sync = async (formId: number) : Promise<void> => {
        try {
            let data = await sectionService.list(formId);
            this.all = Mix.castArrayAs(Section, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.question.sync'));
            throw e;
        }
    }
}