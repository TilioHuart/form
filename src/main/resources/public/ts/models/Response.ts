import {idiom, notify} from "entcore";
import {responseService} from "../services/ResponseService";

export class Response {
    id: number;
    question_id: number;
    distribution_id: number;
    answer: string;

    constructor() {
        this.id = null;
        this.question_id = null;
        this.distribution_id = null;
        this.answer = null;
    }

    toJson(): Object {
        return {
            id: this.id,
            question_id: this.question_id,
            distribution_id: this.distribution_id,
            answer: this.answer,
        }
    }

    setFromJson(data : any) {
        for (let key in data) {
            this[key] = data[key];
        }
    }

    async get (question_id : number) : Promise<void> {
        try {
            let { data } = await responseService.get(question_id);
            this.setFromJson(data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.responseService.get'));
            throw e;
        }
    }

    async send () : Promise<void> {
        try {
            await responseService.save(this);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.response.send'));
            throw e;
        }
    }
}