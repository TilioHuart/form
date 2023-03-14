import {Selectable} from "entcore-toolkit";
import {FormElementType} from "@common/core/enums/form-element-type";

export abstract class FormElement implements Selectable {
    id: number;
    form_id: number;
    title: string;
    position: number;
    form_element_type: string;
    nb_responses: number;
    selected: boolean;

    protected constructor() {
        this.id = null;
        this.form_id = null;
        this.title = null;
        this.position = null;
        this.nb_responses = 0;
        this.selected = null;
    }

    toJson() : Object {
        return {
            id: this.id,
            form_id: this.form_id,
            title: this.title,
            position: this.position,
            form_element_type: this.form_element_type,
            nb_responses: this.nb_responses,
            selected: this.selected
        }
    }

    isSection = () : boolean => {
        return this.form_element_type === FormElementType.SECTION;
    }

    isQuestion = () : boolean => {
        return this.form_element_type === FormElementType.QUESTION;
    }
}