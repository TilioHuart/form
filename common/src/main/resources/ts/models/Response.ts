import {idiom, moment, notify} from "entcore";
import {responseFileService, responseService} from "../services";
import {Mix, Selectable, Selection} from "entcore-toolkit";
import {ResponseFile, ResponseFiles} from "./ResponseFile";
import {Question} from "./FormElement";
import {Types} from "@common/models/QuestionType";
import {Constants} from "@common/core/constants";

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
    choice_position: number; // For question type ranking to order
    image?: string; // For question type multiple answer

    constructor(question_id?: number, choice_id?: number, answer?: string|Date|number, distribution_id?: number, choice_position?: number) {
        this.id = null;
        this.question_id = question_id ? question_id : null;
        this.choice_id = choice_id ? choice_id : null;
        this.answer =  answer ? answer : "";
        this.distribution_id = distribution_id ? distribution_id : null;
        this.original_id = null;
        this.custom_answer = null;
        this.files = new ResponseFiles();
        this.selected = false;
        this.choice_position = choice_position ? choice_position : null;
        this.image = null;
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
            selected: this.selected,
            choice_position: this.choice_position,
            image: this.image
        }
    }

    build(data: IResponseResponse) : Response {
        this.id = data.id ? data.id : null;
        this.question_id = data.questionId ? data.questionId : null;
        this.choice_id = data.choiceId ? data.choiceId : null;
        this.answer = data.answer ? data.answer : "";
        this.distribution_id = data.distributionId ? data.distributionId : null;
        this.original_id = data.originalId ? data.originalId : null;
        this.custom_answer = data.customAnswer ? data.customAnswer : null;
        this.choice_position = data.choicePosition ? data.choicePosition : null;
        this.image = data.image ? data.image : null;
        return this;
    }

    formatBeforeSaving = (questionType: Types) : void => {
        if (this.answer == undefined) {
            this.answer = "";
        }
        else {
            if (questionType === Types.TIME && typeof this.answer != Constants.STRING) {
                this.answer = moment(this.answer).format(Constants.HH_MM);
            }
            else if (questionType === Types.DATE && typeof this.answer != Constants.STRING) {
                this.answer = moment(this.answer).format(Constants.DD_MM_YYYY);
            }
            else if (questionType === Types.CURSOR && typeof this.answer != Constants.STRING) {
                this.answer = this.answer.toString();
            }
        }
    }
}

export class Responses extends Selection<Response> {
    all: Response[];
    hasLoaded: boolean;

    constructor() {
        super([]);
    }

    build(data: IResponseResponse[]) : Responses {
        this.all = data.map((rr: IResponseResponse) => new Response().build(rr));
        return this;
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

    syncMineByQuestionIds = async (questionIds: number[], distributionId: number) : Promise<void> => {
        try{
            this.all = await responseService.listMineByDistributionAndQuestions(questionIds, distributionId);
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
            this.all = await responseService.listByForm(formId);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.response.sync'));
            throw e;
        }
    }
}

export interface IResponseResponse {
    id: number;
    questionId: number;
    choiceId: number;
    answer: string|Date|number;
    distributionId: number;
    originalId: number;
    customAnswer: string;
    choicePosition: number; // For question type ranking to order
    image: string;
    responderId: string;
}