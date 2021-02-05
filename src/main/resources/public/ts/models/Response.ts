import {idiom, notify} from "entcore";
import {responseService} from "../services/ResponseService";
import {Mix} from "entcore-toolkit";

export class Response {
    id: number;
    question_id: number;
    choice_id: number;
    answer: string|Date;

    constructor(question_id?: number, choice_id?: number, answer?: string|Date) {
        this.id = null;
        this.question_id = question_id ? question_id : null;
        this.choice_id = choice_id ? choice_id : null;
        this.answer =  answer ? answer : null;
    }

    toJson(): Object {
        return {
            id: this.id,
            question_id: this.question_id,
            choice_id: this.choice_id,
            answer: this.answer,
        }
    }
}

export class Responses {
    all: Response[];

    constructor() {
        this.all = [];
    }

    async sync (questionId: number) : Promise<void> {
        try {
            let { data } = await responseService.list(questionId);
            this.all = Mix.castArrayAs(Response, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.response.sync'));
            throw e;
        }
    }

    async syncMine (questionId: number) : Promise<void> {
        try {
            let { data } = await responseService.listMine(questionId);
            this.all = Mix.castArrayAs(Response, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.response.sync'));
            throw e;
        }
    }
}