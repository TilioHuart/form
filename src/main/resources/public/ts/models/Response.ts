import {idiom, notify} from "entcore";
import {responseService} from "../services/ResponseService";

export class Response {
    id: number;
    question_id: number;
    answer: string | Date;

    constructor() {
        this.id = null;
        this.question_id = null;
        this.answer = null;
    }

    toJson(): Object {
        return {
            id: this.id,
            question_id: this.question_id,
            answer: this.answer,
        }
    }
}