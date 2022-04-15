import {Mix, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {questionService, sectionService} from "../../services";
import {Question, Questions} from "./Question";
import {FormElement} from "./FormElement";
import {Section, Sections} from "./Section";

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

            this.all.sort((a, b) => a.position - b.position);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.formElements.sync'));
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

    getSections = (minPosition?: number) : Sections => {
        let sections = new Sections();
        sections.all = this.all.filter(e => e instanceof Section && (minPosition != undefined ? e.position > minPosition : true)) as Section[];
        return sections;
    }

    getAllQuestions = () : Questions => {
        let questions = this.getQuestions();
        let sectionQuestions = this.getSections().all.map(s => s.questions.all) as any;
        questions.all = questions.all.concat(sectionQuestions.flat());
        return questions;
    }

    getQuestionById = (questionId: number) : Question => {
        let question = this.getQuestions().all.filter(question => question.id === questionId)[0];
        if (question) { return question; }

        let sectionQuestions: any = this.getSections().all.map(s => (s as Section).questions.all);
        return sectionQuestions.flat().filter(q => q.id === questionId)[0];
    }

    getSelectedElement = () : FormElement => {
        let selectedElement = this.selected[0];
        if (!selectedElement) {
            let sectionQuestionsSelected = (this.getSections().all.map(s => s.questions.selected) as any).flat();
            selectedElement = sectionQuestionsSelected.length > 0 ? sectionQuestionsSelected[0] : null;
        }
        return selectedElement;
    }

    hasSelectedElement = () : boolean => {
        return !!this.getSelectedElement();
    }
}