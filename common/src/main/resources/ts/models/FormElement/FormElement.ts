import {Selectable} from "entcore-toolkit";

export abstract class FormElement implements Selectable {
    id: number;
    form_id: number;
    title: string;
    position: number;
    nb_responses: number;
    selected: boolean;
    question_type: number;
    cursor_min_val: number;
    cursor_max_val: number;
    cursor_step: number;

    protected constructor() {
        this.id = null;
        this.form_id = null;
        this.title = null;
        this.position = null;
        this.nb_responses = 0;
        this.selected = null;
        this.question_type = 0;
        this.cursor_min_val = 0;
        this.cursor_max_val = 0;
        this.cursor_step = 0;
    }

    toJson() : Object {
        return {
            id: this.id,
            form_id: this.form_id,
            title: this.title,
            position: this.position,
            nb_responses: this.nb_responses,
            selected: this.selected,
            types: this.question_type,
            cursor_min_val: this.cursor_min_val,
            cursor_max_val: this.cursor_max_val,
            cursor_step: this.cursor_step
        }
    }
}