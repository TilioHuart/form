import {Mix, Selection} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {Question, Questions} from "./Question";
import {FormElement} from "./FormElement";
import {Section, Sections} from "./Section";
import {QuestionChoice, Types} from "@common/models";
import {questionChoiceService, questionService, sectionService} from "@common/services";

export class FormElements extends Selection<FormElement> {
    all: FormElement[];

    constructor() {
        super([]);
    }

    sync = async (formId: number) : Promise<void> => {
        // Sections
        try {
            let sections: Sections = new Sections();
            let data = await sectionService.list(formId);
            sections.all = Mix.castArrayAs(Section, data);
            this.all = sections.all;

            // Questions
            try {
                let allQuestions: Questions = new Questions();
                let data = await questionService.listAll(formId);
                allQuestions.all = Mix.castArrayAs(Question, data);

                // Choices and Children of matrix
                if (allQuestions.all.length > 0) {
                    await allQuestions.syncChoices();
                    await allQuestions.syncChildren();
                }

                for (let question of allQuestions.all) {
                    if (!question.section_id) {
                        this.all.push(question);
                    }
                    else {
                        let parentSection: Section = sections.all.find((s: Section) => s.id === question.section_id);
                        parentSection.questions.all.push(question);
                        parentSection.questions.all.sort((a: Question, b: Question) => a.section_position - b.section_position);
                    }
                }
            } catch (e) {
                notify.error(idiom.translate('formulaire.error.question.sync'));
                throw e;
            }

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

    getAllSectionsAndAllQuestions = () : FormElement[] => {
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