import {Mix, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {sectionService} from "../../services";
import {FormElement} from "./FormElement";
import {Questions} from "./Question";
import {FormElementType} from "@common/core/enums/form-element-type";

export class Section extends FormElement {
    description: string;
    questions: Questions;

    constructor() {
        super();
        this.description = null;
        this.form_element_type = FormElementType.SECTION;
        this.questions = new Questions();
    }

    toJson() : Object {
        return {
            id: this.id,
            form_id: this.form_id,
            title: this.title,
            position: this.position,
            selected: this.selected,
            description: this.description,
            form_element_type: this.form_element_type,
            questions: this.questions
        }
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