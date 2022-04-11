import {Mix, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {questionChoiceService, questionService} from "../../services";
import {QuestionChoice, QuestionChoices} from "../QuestionChoice";
import {Types} from "../QuestionType";
import {FormElement} from "./FormElement";
import {Distributions} from "../Distribution";
import {Response} from "../Response";

export class Question extends FormElement {
    question_type: any;
    statement: string;
    mandatory: boolean;
    section_id: number;
    section_position: number;
    conditional: boolean;
    choices: QuestionChoices;

    constructor() {
        super();
        this.question_type = null;
        this.statement = null;
        this.mandatory = null;
        this.section_id = null;
        this.section_position = null;
        this.conditional = false;
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
            conditional: this.conditional,
            choices: this.choices
        }
    }

    fillChoicesInfo = async (distribs: Distributions, results: Response[]) : Promise<void> => {
        // Count responses for each choice
        for (let result of results) {
            for (let choice of this.choices.all) {
                if (result.choice_id === choice.id) {
                    choice.nbResponses++;
                }
            }
        }

        // Deal with no choice responses
        let finishedDistribIds : any = distribs.all.map(d => d.id);
        let noResponseChoice = new QuestionChoice();
        noResponseChoice.value = idiom.translate('formulaire.response.empty');
        noResponseChoice.nbResponses = results.filter(r => !r.choice_id && finishedDistribIds.includes(r.distribution_id)).length;

        this.choices.all.push(noResponseChoice);
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
            if (this.all.length > 0) {
                await this.syncChoices();
            }
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.question.sync'));
            throw e;
        }
    }

    syncChoices = async () : Promise<void> => {
        let data = await questionChoiceService.listChoices(this.all.map(q => q.id));
        if (data.length > 0) {
            let listChoices = Mix.castArrayAs(QuestionChoice, data);
            for (let question of this.all) {
                if (question.question_type === Types.SINGLEANSWER
                    || question.question_type === Types.MULTIPLEANSWER
                    || question.question_type === Types.SINGLEANSWERRADIO) {

                    question.choices.all = listChoices.filter(c => c.question_id === question.id);
                    let nbChoices = question.choices.all.length;
                    if (nbChoices <= 0) {
                        for (let j = 0; j < 3; j++) {
                            question.choices.all.push(new QuestionChoice(question.id));
                        }
                    }
                }
            }
        }
    }
}