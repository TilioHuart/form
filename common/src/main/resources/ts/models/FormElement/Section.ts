import {Mix, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {sectionService} from "../../services";
import {FormElement, FormElementPayload} from "./FormElement";
import {Question, QuestionPayload, Questions} from "./Question";
import {FormElementType} from "@common/core/enums/form-element-type";
import {FormElements} from "@common/models";

export class Section extends FormElement {
    description: string;
    next_form_element: FormElement;
    next_form_element_id: number;
    next_form_element_type: FormElementType;
    is_next_form_element_default: boolean;
    questions: Questions;

    constructor() {
        super();
        this.form_element_type = FormElementType.SECTION;
        this.description = null;
        this.next_form_element = null;
        this.next_form_element_id = null;
        this.next_form_element_type = null;
        this.is_next_form_element_default = true;
        this.questions = new Questions();
    }

    toJson() : Object {
        return {
            id: this.id,
            form_id: this.form_id,
            title: this.title,
            position: this.position,
            selected: this.selected,
            form_element_type: this.form_element_type,
            description: this.description,
            next_form_element: this.next_form_element,
            next_form_element_id: this.next_form_element_id,
            next_form_element_type: this.next_form_element_type,
            is_next_form_element_default: this.is_next_form_element_default,
            questions: this.questions
        }
    }

    setNextFormElementValuesWithDefault = (formElements: FormElements) : void => {
        this.next_form_element = this.getFollowingFormElement(formElements);
        this.next_form_element_id = this.next_form_element ? this.next_form_element.id : null;
        this.next_form_element_type = this.next_form_element ? this.next_form_element.form_element_type : null;
        this.is_next_form_element_default = true;
    }

    getNextFormElement = (formElements: FormElements) : FormElement => {
        return formElements.all.find((e: FormElement) =>
            e.id === this.next_form_element_id &&
            e.form_element_type === this.next_form_element_type
        );
    }

    getNextFormElementPosition = (formElements: FormElements) : number => {
        let nextFormElement: FormElement = this.getNextFormElement(formElements);
        return nextFormElement ? nextFormElement.position : null;
    }

    getPayload = () : FormElementPayload => {
        return new SectionPayload(this);
    }
}

export class Sections extends Selection<Section> {
    all: Section[];

    constructor() {
        super([]);
    }

    sync = async (formId: number) : Promise<void> => {
        try {
            let data = await sectionService.list(formId);
            this.all = Mix.castArrayAs(Section, data);
            await this.syncQuestions();
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.question.sync'));
            throw e;
        }
    }

    syncQuestions = async () : Promise<void> => {
        for (let i = 0; i < this.all.length; i++) {
            let questions = this.all[i].questions;
            await questions.sync(this.all[i].id, true);
        }
    }
}

export class SectionPayload implements FormElementPayload {
    id: number;
    form_id: number;
    title: string;
    position: number;
    form_element_type: FormElementType;
    description: string;
    next_form_element_id: number;
    next_form_element_type: FormElementType;
    is_next_form_element_default: boolean;
    questions: QuestionPayload[];

    constructor(section: Section) {
        this.id = section.id ? section.id : null;
        this.form_id = typeof section.form_id == 'number' ? section.form_id : null;
        this.title = section.title ? section.title : "";
        this.position = section.position ? section.position : null;
        this.form_element_type = section.form_element_type ? section.form_element_type : FormElementType.SECTION;
        this.description = section.description ? section.description : null;
        this.next_form_element_id = typeof section.next_form_element_id == 'number' ? section.next_form_element_id : null;
        this.next_form_element_type = section.next_form_element_type ? section.next_form_element_type : null;
        this.is_next_form_element_default = section.is_next_form_element_default ? section.is_next_form_element_default : true;
        this.questions = section.questions ? section.questions.all.map((q: Question) => new QuestionPayload(q)) : null;
    }

    toJson(): Object {
        return {
            id: this.id,
            form_id: this.form_id,
            title: this.title,
            position: this.position,
            form_element_type: this.form_element_type,
            description: this.description,
            next_form_element_id: this.next_form_element_id,
            next_form_element_type: this.next_form_element_type,
            is_next_form_element_default: this.is_next_form_element_default,
            questions: this.questions
        }
    }
}