import {Mix} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {questionTypeService} from "../services";

export enum Types {
    FREETEXT = 1,
    SHORTANSWER = 2,
    LONGANSWER = 3,
    SINGLEANSWER = 4,
    MULTIPLEANSWER = 5,
    DATE = 6,
    TIME = 7,
    FILE = 8,
    SINGLEANSWERRADIO = 9,
    MATRIX = 10
}

export class QuestionType {
    id: number;
    code: number;
    name: string;

    constructor () {
        this.id = null;
        this.code = 0;
        this.name = "";
    }

    toJson() : Object {
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

    sync = async () : Promise<void> => {
        try {
            let data = await questionTypeService.list();
            this.all = Mix.castArrayAs(QuestionType, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.questionType.sync'));
            throw e;
        }
    }
}