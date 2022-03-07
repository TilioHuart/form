import {Mix, Selectable, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {questionService} from "../../services";
import {QuestionChoice, QuestionChoices} from "../QuestionChoice";
import {Types} from "../QuestionType";
import {FormElement} from "./FormElement";

export class Question extends FormElement {
    question_type: any;
    statement: string;
    mandatory: boolean;
    section_id: number;
    section_position: number;
    choices: QuestionChoices;

    constructor() {
        super();
        this.question_type = null;
        this.statement = null;
        this.mandatory = null;
        this.section_id = null;
        this.section_position = null;
        this.choices = new QuestionChoices();
    }

    toJson() : Object {
        return {
            id: this.id,
            form_id: this.form_id,
            title: this.title,
            position: this.position,
            selected: this.selected,
            question_type: this.question_type,
            statement: this.statement,
            mandatory: this.mandatory,
            section_id: this.section_id,
            section_position: this.section_position,
            choices: this.choices
        }
    }
}

export class Questions extends Selection<Question> {
    all: Question[];

    constructor() {
        super([]);
    }

    sync = async (id: number, isForSection= false) : Promise<void> => {
        try {
            let data = await questionService.list(id, isForSection);
            this.all = Mix.castArrayAs(Question, data);
            await this.syncChoices();
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.question.sync'));
            throw e;
        }
    }

    syncChoices = async () : Promise<void> => {
        for (let i = 0; i < this.all.length; i++) {
            if (this.all[i].question_type === Types.SINGLEANSWER
                || this.all[i].question_type === Types.MULTIPLEANSWER
                || this.all[i].question_type === Types.SINGLEANSWERRADIO) {
                let questionChoices = this.all[i].choices;
                await questionChoices.sync(this.all[i].id);
                let nbChoices = questionChoices.all.length;
                if (nbChoices <= 0) {
                    for (let j = 0; j < 3; j++) {
                        questionChoices.all.push(new QuestionChoice(this.all[i].id));
                    }
                }
            }
        }
    }
}