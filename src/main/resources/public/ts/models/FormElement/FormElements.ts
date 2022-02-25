import {Mix, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {questionService, sectionService} from "../../services";
import {Question, Questions} from "./Question";
import {FormElement} from "./FormElement";
import {Section} from "./Section";

export class FormElements extends Selection<FormElement> {
    all: FormElement[];

    constructor() {
        super([]);
    }

    sync = async (formId: number) : Promise<void> => {
        try {
            // Add all questions
            let dataQuestions = await questionService.list(formId);
            let questions = new Questions();
            questions.all = Mix.castArrayAs(Question, dataQuestions);
            await questions.syncChoices();
            this.all = questions.all;

            // Add all sections
            let dataSections = await sectionService.list(formId);
            this.all = this.all.concat(Mix.castArrayAs(Section, dataSections));

            this.all.sort((a,b) => (a.position > b.position) ? 1 : ((b.position > a.position) ? -1 : 0))
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.question.sync'));
            throw e;
        }
    }
}