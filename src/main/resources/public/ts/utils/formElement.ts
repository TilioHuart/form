import {
    Form,
    FormElement, FormElements,
    Question,
    Response, Responses,
    Section,
    Distribution, Questions
} from "../models";
import {Mix} from "entcore-toolkit";

export class FormElementUtils {
    static castFormElement = (formElement: any) : Question|Section => {
        if (formElement.statement != undefined) {
            return Mix.castAs(Question, formElement);
        }
        else {
            return Mix.castAs(Section, formElement);
        }
    };

    static isQuestion = (formElement: FormElement) : boolean => {
        return formElement instanceof Question;
    };

    //

    static getLastQuestions = (formElements: FormElements) : any => {
        let lastQuestions = [];
        for (let e of formElements.all) {
            if (e instanceof Question) {
                if (e.conditional && e.choices.all.filter(c => !c.next_section_id).length > 0) {
                    lastQuestions.push(e);
                }
                else if (!e.conditional && e.position === formElements.all.length) {
                    lastQuestions.push(e);
                }
            }
            else if (e instanceof Section) {
                let conditionalQuestions = e.questions.all.filter(q => q.conditional);
                if (conditionalQuestions.length === 1 && conditionalQuestions[0].choices.all.filter(c => !c.next_section_id).length > 0) {
                    lastQuestions.push(conditionalQuestions[0]);
                }
                else if (conditionalQuestions.length <= 0 && e.position === formElements.all.length) {
                    lastQuestions.push(e.questions.all[e.questions.all.length - 1]);
                }
            }
        }
        return lastQuestions;
    };

    static hasRespondedLastQuestion = async (form: Form, distribution: Distribution) : Promise<boolean> => {
        let formElements = new FormElements();
        let responses = new Responses();
        await formElements.sync(form.id);
        await responses.syncByDistribution(distribution.id);
        let lastQuestions = FormElementUtils.getLastQuestions(formElements);
        return responses.all.filter(r => FormElementUtils.isLastValidLastResponse(r, lastQuestions)).length > 0;
    };

    static isLastValidLastResponse = (response: Response, lastQuestions: any) : boolean => {
        let matchingQuestions = lastQuestions.filter(q => q.id === response.question_id);
        let question = matchingQuestions.length > 0 ? matchingQuestions[0] : null;
        return question && (question.conditional ? !!response.answer : true);
    };

    //

    static updateSiblingsPositions = (formElements: FormElements|Questions, isAdd: boolean, goUp: boolean, startIndex: number, endIndex?: number) : void => {
        if (formElements instanceof Questions) {
            FormElementUtils.updateSectionPositionsAfter(formElements, isAdd, goUp, startIndex, endIndex);
        }
        else {
            FormElementUtils.updatePositionsAfter(formElements, isAdd, goUp, startIndex, endIndex);
        }
    };

    static updatePositionsAfter = (formElements: FormElements|Questions, isAdd: boolean, goUp: boolean, startIndex: number, endIndex?: number) : void => {
        endIndex = endIndex ? endIndex : formElements.all.length;
        for (let i = startIndex; i < endIndex; i++) {
            let formElement = formElements.all[i];
            if (goUp === null) {
                isAdd ? formElement.position++ : formElement.position--;
            }
            else {
                goUp ? formElement.position++ : formElement.position--;
            }
        }
    };

    static updateSectionPositionsAfter = (questions: Questions, isAdd: boolean, goUp: boolean, startIndex: number, endIndex?: number) : void => {
        endIndex = endIndex ? endIndex : questions.all.length;
        for (let i = startIndex; i < endIndex; i++) {
            let question = questions.all[i];
            if (goUp === null) {
                isAdd ? question.section_position++ : question.section_position--;
            }
            else {
                goUp ? question.section_position++ : question.section_position--;
            }
        }
    };
}