import {Selectable, Selection} from "entcore-toolkit";

export class Form implements Selectable {
    owner_id: string;
    owner_name: string;
    title: string;
    selected: boolean;

    constructor() {
        this.owner_id = null;
        this.owner_name = null;
        this.title = null;
    }

    toJson(): Object {
        return {
            owner_id: this.owner_id,
            owner_name: this.owner_name,
            title: this.title,
            selected: this.selected
        }
    }
}

export class Forms extends Selection<Form> {
    constructor() {
        super([]);
    }
}