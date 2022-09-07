import {Mix, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {questionChoiceService, questionService} from "../../services";
import {QuestionChoice, QuestionChoices} from "../QuestionChoice";
import {Types} from "../QuestionType";
import {FormElement} from "./FormElement";
import {Distributions} from "../Distribution";
import {Response} from "../Response";
import {Constants} from "@common/core/constants";

export class Question extends FormElement {
    question_type: number;
    statement: string;
    mandatory: boolean;
    section_id: number;
    section_position: number;
    conditional: boolean;
    matrix_id: number;
    choices: QuestionChoices;
    placeholder: string;
    children: Questions;

    constructor(matrixId?: number, questionType?: number) {
        super();
        this.question_type = questionType ? questionType : null;
        this.statement = null;
        this.mandatory = false;
        this.section_id = null;
        this.section_position = null;
        this.conditional = false;
        this.matrix_id = matrixId ? matrixId : null;
        this.choices = new QuestionChoices();
        this.children = new Questions();
        this.placeholder = null;
    }

    toJson() : Object {
        return {
            id: this.id,
            form_id: this.form_id,
            title: this.title,
            placeholder: this.placeholder,
            position: this.position,
            selected: this.selected,
            question_type: this.question_type,
            statement: this.statement,
            mandatory: this.mandatory,
            section_id: this.section_id,
            section_position: this.section_position,
            conditional: this.conditional,
            matrix_id: this.matrix_id,
            choices: this.choices,
            children: this.children
        }
    }

    fillChoicesInfo = (distribs: Distributions, results: Response[]) : void => {
        if (this.question_type === Types.MATRIX) {
            return this.fillChoicesInfoForMatrix(results);
        }

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
        let noResponseChoice: QuestionChoice = new QuestionChoice();
        noResponseChoice.value = idiom.translate('formulaire.response.empty');
        noResponseChoice.nbResponses = results.filter(r => !r.choice_id && finishedDistribIds.includes(r.distribution_id)).length;

        this.choices.all.push(noResponseChoice);
    }

    fillChoicesInfoForMatrix = (results: Response[]) : void => {
        if (this.question_type != Types.MATRIX) {
            return this.fillChoicesInfo(null, results);
        }

        // Count responses for each choice
        for (let child of this.children.all) {

            // Create child choices based on copy of parent choices
            child.choices.all = [];
            for (let choice of this.choices.all) {
                child.choices.all.push(new QuestionChoice(this.id, choice.value));
            }

            let matchingResults: Response[] = results.filter((r: Response) => r.question_id === child.id);
            for (let result of matchingResults) {
                for (let choice of this.choices.all) {
                    if (result.choice_id === choice.id) {
                        child.choices.all.filter(c => c.value === choice.value)[0].nbResponses++;
                    }
                }
            }
        }
    }

    isTypeGraphQuestion = () : boolean => {
        return this.question_type == Types.SINGLEANSWER
            || this.question_type == Types.MULTIPLEANSWER
            || this.question_type == Types.SINGLEANSWERRADIO
            || this.question_type == Types.MATRIX;
    }

    isTypeChoicesQuestion = () : boolean => {
        return this.question_type == Types.SINGLEANSWER
            || this.question_type == Types.MULTIPLEANSWER
            || this.question_type == Types.SINGLEANSWERRADIO
            || this.question_type == Types.MATRIX;
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
                await this.syncChildren();
            }
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.question.sync'));
            throw e;
        }
    }

    syncChoices = async () : Promise<void> => {
        let choicesQuestions: Question[] = this.all.filter((q: Question) => q.isTypeChoicesQuestion());
        if (choicesQuestions.length > 0) {
            let data = await questionChoiceService.listChoices(this.all.map((q: Question) => q.id));
            let listChoices: QuestionChoice[] = Mix.castArrayAs(QuestionChoice, data);
            for (let question of this.all) {
                question.choices.all = listChoices.filter(c => c.question_id === question.id);
                let nbChoices: number = question.choices.all.length;
                if (nbChoices <= 0) {
                    for (let j = 0; j < Constants.DEFAULT_NB_CHOICES; j++) {
                        question.choices.all.push(new QuestionChoice(question.id));
                    }
                }
            }
        }
    }

    syncChildren = async () : Promise<void> => {
        let matrixQuestions: Question[] = this.all.filter((q: Question) => q.question_type == Types.MATRIX);
        if (matrixQuestions.length > 0) {
            let data = await questionService.listChildren(matrixQuestions);
            let listChildrenQuestions: Question[] = Mix.castArrayAs(Question, data);
            for (let question of this.all) {
                question.children.all = listChildrenQuestions.filter((q: Question) => q.matrix_id === question.id);
                let nbChildren: number = question.children.all.length;
                if (nbChildren <= 0) {
                    for (let j = 0; j < Constants.DEFAULT_NB_CHILDREN; j++) {
                        question.children.all.push(new Question(question.id));
                    }
                }
            }
        }
    }
}