import {Mix} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {questionChoiceService} from "../services";

export enum ChoiceTypes {
    TXT = 'TXT',
    IMAGE = 'IMAGE',
    VIDEO = 'VIDEO'
}

export class QuestionChoice {
    id: number;
    question_id: number;
    value: string;
    type: ChoiceTypes;

    constructor (questionId: number, type?: ChoiceTypes) {
        this.id = null;
        this.question_id = questionId;
        this.value = "";
        this.type = type ? type : ChoiceTypes.TXT;
    }

    toJson(): Object {
        return {
            id: this.id,
            question_id: this.question_id,
            value: this.value,
            type: this.type
        }
    }
}

export class QuestionChoices {
    all: QuestionChoice[];

    constructor() {
        this.all = [];
    }

    async sync (questionId: number) : Promise<void> {
        try {
            let { data } = await questionChoiceService.list(questionId);
            this.all = Mix.castArrayAs(QuestionChoice, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.questionChoice.sync'));
            throw e;
        }
    }
}