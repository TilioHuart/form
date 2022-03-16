import {Form, FormElement, FormElements, Question, Responses, Section, Distribution} from "../models";
import {Mix} from "entcore-toolkit";

export class FormElementUtils {
    static castFormElement(formElement: any) : Question|Section {
        if (formElement.statement != undefined) {
            return Mix.castAs(Question, formElement);
        }
        else {
            return Mix.castAs(Section, formElement);
        }
    };

    static isQuestion(formElement: FormElement) : boolean {
        return formElement instanceof Question;
    };

    static getLastQuestionIds = (formElements: FormElements) : any => {
        let lastQuestionIds = [];
        for (let e of formElements.all) {
            if (e instanceof Question) {
                if (e.conditional && e.choices.all.filter(c => !c.next_section_id).length > 0) {
                    lastQuestionIds.push(e.id);
                }
                else if (!e.conditional && e.position === formElements.all.length) {
                    lastQuestionIds.push(e.id);
                }
            }
            else if (e instanceof Section) {
                let conditionalQuestions = e.questions.all.filter(q => q.conditional);
                if (conditionalQuestions.length === 1 && conditionalQuestions[0].choices.all.filter(c => !c.next_section_id).length > 0) {
                    lastQuestionIds.push(conditionalQuestions[0].id);
                }
                else if (conditionalQuestions.length <= 0 && e.position === formElements.all.length) {
                    lastQuestionIds.push(e.questions.all[e.questions.all.length - 1].id);
                }
            }
        }
        return lastQuestionIds;
    };

    static hasRespondedLastQuestion = async (form: Form, distribution: Distribution) : Promise<boolean> => {
        let formElements = new FormElements();
        let responses = new Responses();
        await formElements.sync(form.id);
        await responses.syncByDistribution(distribution.id);
        let lastQuestionIds = FormElementUtils.getLastQuestionIds(formElements);
        return responses.all.filter(r => lastQuestionIds.includes(r.question_id) && r.answer).length > 0;
    };
}