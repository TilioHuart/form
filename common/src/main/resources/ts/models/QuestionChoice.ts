import {Mix} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {questionChoiceService} from "../services";
import {FormElementType} from "@common/core/enums/form-element-type";
import {FormElement, FormElements} from "@common/models/FormElement";

export enum ChoiceTypes {
    TXT = 'TXT',
    IMAGE = 'IMAGE',
    VIDEO = 'VIDEO'
}

export class QuestionChoice {
    id: number;
    question_id: number;
    value: string;
    position: number;
    type: ChoiceTypes;
    next_form_element: FormElement;
    next_form_element_id: number;
    next_form_element_type: FormElementType;
    is_custom: boolean;
    nbResponses: number;

    constructor (questionId?: number, position: number = 0, value?: string, next_form_element?: FormElement, type?: ChoiceTypes) {
        this.id = null;
        this.question_id = questionId ? questionId : null;
        this.value = value ? value : "";
        this.position = position;
        this.type = type ? type : ChoiceTypes.TXT;
        this.next_form_element = next_form_element ? next_form_element : null;
        this.next_form_element_id = next_form_element ? next_form_element.id : null;
        this.next_form_element_type = next_form_element ? next_form_element.form_element_type : null;
        this.is_custom = false;
        this.nbResponses = 0;
    }

    toJson() : Object {
        return {
            id: this.id,
            question_id: this.question_id,
            value: this.value,
            position: this.position,
            type: this.type,
            next_form_element: this.next_form_element,
            next_form_element_id: this.next_form_element_id,
            next_form_element_type: this.next_form_element_type,
            is_custom: this.is_custom
        }
    }
}

export class QuestionChoices {
    all: QuestionChoice[];

    constructor() {
        this.all = [];
    }

    sync = async (questionId: number) : Promise<void> => {
        try {
            let data = await questionChoiceService.list(questionId);
            this.all = Mix.castArrayAs(QuestionChoice, data);
            this.all.sort((a: QuestionChoice, b: QuestionChoice) => a.position - b.position);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.questionChoice.sync'));
            throw e;
        }
    }

    replaceSpace = () : void => {
        for (let i = 0; i < this.all.length; i++) {
            this.all[i].value = this.all[i].value.replace(/\u00A0/," ");
        }
    }

}