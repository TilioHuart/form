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
    position: number;
    type: ChoiceTypes;
    next_section_id: number;
    nbResponses: number;

    constructor (questionId?: number, position: number = 0, value?: string, next_section_id?: number, type?: ChoiceTypes) {
        this.id = null;
        this.question_id = questionId ? questionId : null;
        this.value = value ? value : "";
        this.position = position;
        this.type = type ? type : ChoiceTypes.TXT;
        this.next_section_id = next_section_id ? next_section_id : null;
        this.nbResponses = 0;
    }

    toJson() : Object {
        return {
            id: this.id,
            question_id: this.question_id,
            value: this.value,
            position: this.position,
            type: this.type,
            next_section_id: this.next_section_id
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