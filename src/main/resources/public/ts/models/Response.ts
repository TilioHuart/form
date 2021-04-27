import {idiom, notify} from "entcore";
import {responseService} from "../services/ResponseService";
import {Mix} from "entcore-toolkit";
import {ResponseFiles} from "./ResponseFile";
import {Types} from "./QuestionType";
import {QuestionChoice} from "./QuestionChoice";

export class Response {
    id: number;
    question_id: number;
    choice_id: number;
    answer: string|Date;
    distribution_id: number;
    files: ResponseFiles;

    constructor(question_id?: number, choice_id?: number, answer?: string|Date, distribution_id?: number) {
        this.id = null;
        this.question_id = question_id ? question_id : null;
        this.choice_id = choice_id ? choice_id : null;
        this.answer =  answer ? answer : null;
        this.distribution_id = distribution_id ? distribution_id : null;
        this.files = new ResponseFiles();
    }

    toJson() : Object {
        return {
            id: this.id,
            question_id: this.question_id,
            choice_id: this.choice_id,
            answer: this.answer,
            distribution_id: this.distribution_id,
            files: this.files
        }
    }
}

export class Responses {
    all: Response[];

    constructor() {
        this.all = [];
    }

    sync = async (questionId: number, isFileQuestion: boolean) : Promise<void> => {
        try {
            let { data } = await responseService.list(questionId);
            this.all = Mix.castArrayAs(Response, data);
            if (isFileQuestion) {
                for (let i = 0; i < this.all.length; i++) {
                    await this.all[i].files.sync(this.all[i].id);
                }
            }
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.response.sync'));
            throw e;
        }
    }

    syncMine = async (questionId: number, distributionId: number) : Promise<void> => {
        try {
            let { data } = await responseService.listMine(questionId, distributionId);
            this.all = Mix.castArrayAs(Response, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.response.sync'));
            throw e;
        }
    }
}