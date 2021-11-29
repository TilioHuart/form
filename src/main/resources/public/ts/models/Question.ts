import {Mix, Selectable, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {questionService} from "../services";
import {QuestionChoice, QuestionChoices} from "./QuestionChoice";
import {Types} from "./QuestionType";

export class Question implements Selectable {
    id: number;
    form_id: number;
    title: string;
    position: number;
    question_type: any;
    statement: string;
    mandatory: boolean;
    choices: QuestionChoices;
    selected: boolean;

    constructor() {
        this.id = null;
        this.title = null;
        this.form_id = null;
        this.position = null;
        this.question_type = null;
        this.statement = null;
        this.mandatory = null;
        this.choices = new QuestionChoices();
        this.selected = null;
    }

    toJson() : Object {
        return {
            id: this.id,
            form_id: this.form_id,
            title: this.title,
            position: this.position,
            question_type: this.question_type,
            statement: this.statement,
            mandatory: this.mandatory,
            choices: this.choices,
            selected: this.selected
        }
    }
}

export class Questions extends Selection<Question> {
    all: Question[];

    constructor() {
        super([]);
    }

    sync = async (formId: number) : Promise<void> => {
        try {
            let data = await questionService.list(formId);
            this.all = Mix.castArrayAs(Question, data);
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
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.question.sync'));
            throw e;
        }
    }
}