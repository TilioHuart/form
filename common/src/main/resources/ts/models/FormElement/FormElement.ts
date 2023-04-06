import {Selectable} from "entcore-toolkit";
import {FormElementType} from "@common/core/enums/form-element-type";
import {Question, Types} from "@common/models";

export abstract class FormElement implements Selectable {
    id: number;
    form_id: number;
    title: string;
    position: number;
    form_element_type: FormElementType;
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

    isSameQuestionType = (formElement: FormElement) : boolean => {
        return this instanceof Question && this.isSameQuestionType(formElement);
    }

    isSameQuestionTypeOfType = (formElement: FormElement, type: Types) : boolean => {
        return this instanceof Question && this.isSameQuestionTypeOfType(formElement, type);
    }
}