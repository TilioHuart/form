import {idiom, notify} from "entcore";
import {responseFileService, responseService} from "../services";
import {Mix, Selectable, Selection} from "entcore-toolkit";
import {ResponseFile, ResponseFiles} from "./ResponseFile";
import {Question} from "./FormElement";
import {Form} from "@common/models/Form";

export class Response implements Selectable {
    id: number;
    question_id: number;
    choice_id: number;
    answer: string|Date|number;
    distribution_id: number;
    original_id: number;
    custom_answer: string;
    files: ResponseFiles;
    selected: boolean;
    selectedIndex: boolean[]; // For multiple answer in preview

    constructor(question_id?: number, choice_id?: number, answer?: string|Date|number, distribution_id?: number) {
        this.id = null;
        this.question_id = question_id ? question_id : null;
        this.choice_id = choice_id ? choice_id : null;
        this.answer =  answer ? answer : "";
        this.distribution_id = distribution_id ? distribution_id : null;
        this.original_id = null;
        this.custom_answer = null;
        this.files = new ResponseFiles();
        this.selected = false;
    }

    toJson() : Object {
        return {
            id: this.id,
            question_id: this.question_id,
            choice_id: this.choice_id,
            answer: this.answer,
            distribution_id: this.distribution_id,
            original_id: this.original_id,
            custom_answer: this.custom_answer,
            files: this.files,
            selected: this.selected
        }
    }
}

export class Responses extends Selection<Response> {
    all: Response[];

    constructor() {
        super([]);
    }

    sync = async (question: Question, isFileQuestion: boolean, nbLines: number = null) : Promise<void> => {
        try {
            let data = await responseService.list(question, nbLines);
            this.all = nbLines && nbLines > 0 ? this.all.concat(Mix.castArrayAs(Response, data)) : Mix.castArrayAs(Response, data);
            if (isFileQuestion) {
                let files = await responseFileService.listByQuestion(question.id);
                for (let i = this.all.length - 1; i >= this.all.length - data.length; i--) {
                    this.all[i].files.all = Mix.castArrayAs(ResponseFile, files.filter(r => r.response_id === this.all[i].id));
                }
            }
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.response.sync'));
            throw e;
        }
    }

    syncMine = async (questionId: number, distributionId: number) : Promise<void> => {
        try {
            let data = await responseService.listMineByDistribution(questionId, distributionId);
            this.all = Mix.castArrayAs(Response, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.response.sync'));
            throw e;
        }
    }

    syncByDistribution = async (distributionId: number) : Promise<void> => {
        try {
            let data = await responseService.listByDistribution(distributionId);
            this.all = Mix.castArrayAs(Response, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.response.sync'));
            throw e;
        }
    }

    syncForForm = async (formId: number) : Promise<void> => {
        try {
            let data = await responseService.listByForm(formId);
            this.all = Mix.castArrayAs(Response, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.response.sync'));
            throw e;
        }
    }
}