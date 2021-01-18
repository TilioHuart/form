import {Mix, Selectable, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {questionTypeService} from "../services";

export class QuestionType {
    id: number;
    code: number;
    name: string;

    constructor () {
        this.id = null;
        this.code = 0;
        this.name = "";
    }

    toJson(): Object {
        return {
            id: this.id,
            code: this.code,
            name: this.name
        }
    }
}

export class QuestionTypes {
    all: QuestionType[];

    constructor() {
        this.all = [];
    }

    async sync () {
        try {
            let { data } = await questionTypeService.list();
            this.all = Mix.castArrayAs(QuestionType, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.questionType.sync'));
            throw e;
        }
    }
}