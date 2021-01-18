import {Mix, Selectable, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {questionService} from "../services";

export class Question implements Selectable {
    id: number;
    form_id: number;
    title: string;
    position: number;
    question_type: any;
    statement: string;
    mandatory: string;
    selected: boolean;

    constructor() {
        this.id = null;
        this.title = null;
        this.form_id = null;
        this.position = null;
        this.question_type = null;
        this.statement = null;
        this.mandatory = null;
        this.selected = null;
    }

    toJson(): Object {
        return {
            id: this.id,
            form_id: this.form_id,
            title: this.title,
            position: this.position,
            question_type: this.question_type,
            statement: this.statement,
            mandatory: this.mandatory,
            selected: this.selected
        }
    }
}

export class Questions extends Selection<Question> {
    constructor() {
        super([]);
    }

    async sync (formId:number) {
        try {
            let { data } = await questionService.list(formId);
            this.all = Mix.castArrayAs(Question, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.question.sync'));
            throw e;
        }
    }
}