import {Mix, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {FormService, questionService, sectionService} from "../../services";
import {Question, Questions} from "./Question";
import {FormElement} from "./FormElement";
import {Section, Sections} from "./Section";
import {Types} from "../QuestionType";
import {QuestionChoice} from "../QuestionChoice";
import {FormElementUtils} from "../../utils";

export class FormElements extends Selection<FormElement> {
    all: FormElement[];

    constructor() {
        super([]);
    }

    sync = async (formId: number) : Promise<void> => {
        try {
            // Add all questions
            let dataQuestions = await questionService.list(formId, false);
            let questions = new Questions();
            questions.all = Mix.castArrayAs(Question, dataQuestions);
            await questions.syncChoices();
            this.all = questions.all;

            // Add all sections
            let dataSections = await sectionService.list(formId);
            this.all = this.all.concat(Mix.castArrayAs(Section, dataSections));
            await this.syncSectionQuestions();

            this.all.sort((a, b) => (a.position > b.position) ? 1 : ((b.position > a.position) ? -1 : 0));
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.question.sync'));
            throw e;
        }
    }

    syncSectionQuestions = async () : Promise<void> => {
        for (let i = 0; i < this.all.length; i++) {
            let formElement = this.all[i];
            if (formElement instanceof Section) {
                await formElement.questions.sync(formElement.id, true);
            }
        }
    }

    getQuestions = () : Questions => {
        let questions = new Questions();
        questions.all = this.all.filter(e => e instanceof Question) as Question[];
        return questions;
    }

    getSections = () : Sections => {
        let sections = new Sections();
        sections.all = this.all.filter(e => e instanceof Section) as Section[];
        return sections;
    }

    getQuestionById = (questionId: number) : Question => {
        let question = this.getQuestions().all.filter(question => question.id === questionId)[0];
        if (question) { return question; }

        let sectionQuestions: any = this.getSections().all.map(s => (s as Section).questions.all);
        return sectionQuestions.flat().filter(q => q.id === questionId)[0];
    }

    hasSelectedElement = () : boolean => {
        let hasSelected = this.selected.length > 0;
        for (let section of this.getSections().all) {
            hasSelected = hasSelected || section.questions.selected.length > 0;
        }
        return hasSelected;
    }
}