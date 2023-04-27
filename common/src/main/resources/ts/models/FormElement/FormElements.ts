import {Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {Question, Questions} from "./Question";
import {FormElement} from "./FormElement";
import {Section, Sections} from "./Section";
import {Types} from "@common/models";

export class FormElements extends Selection<FormElement> {
    all: FormElement[];

    constructor() {
        super([]);
    }

    sync = async (formId: number) : Promise<void> => {
        try {
            // Add all questions
            let questions: Questions = new Questions();
            await questions.sync(formId, false);
            this.all = questions.all;

            // Add all sections
            let sections: Sections = new Sections();
            await sections.sync(formId);
            this.all = this.all.concat(sections.all);

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
        let questions: Questions = this.getQuestions();
        let sectionQuestions: any = this.getSections().all.map((s: Section) => s.questions.all);
        questions.all = questions.all.concat(sectionQuestions.flat());
        return questions;
    }

    getAllQuestionsAndChildren = () : Questions => {
        let questions: Questions = this.getAllQuestions();

        let matrixQuestions: Question[] = questions.all.filter((q: Question) => q.question_type === Types.MATRIX);
        let children: any = matrixQuestions.map((q: Question) => q.children.all);
        questions.all = questions.all.concat(children.flat());

        return questions;
    }

    getAllSectionsAndQuestions = () : FormElement[] => {
        let sections: FormElement[] = this.getSections().all;
        let allQuestions: FormElement[] = this.getAllQuestions().all;
        return sections.concat(allQuestions);
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