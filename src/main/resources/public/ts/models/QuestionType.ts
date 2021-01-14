import {Mix, Selectable, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {questionTypeService} from "../services";

export class QuestionType implements Selectable {
    id: number;
    code: number;
    name: string;
    selected: boolean;

    constructor () {
        this.id = null;
        this.code = 0;
        this.name = "";
        this.selected = false;
    }

    toJson(): Object {
        return {
            id: this.id,
            code: this.code,
            name: this.name,
            selected: this.selected
        }
    }
}

export class QuestionTypes extends Selection<QuestionType> {
    constructor() {
        super([]);
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