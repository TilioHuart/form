import {
    FormElement,
    FormElements,
    Question,
    QuestionChoice,
    QuestionChoices,
    Questions,
    Response, Responses,
    Section
} from "../models";
import {Mix} from "entcore-toolkit";

export class PublicUtils {
    /**
     * Returns an array of colors interpolated between a list of given colors
     * @param data sessionStorage data transformed into JSON
     * @param formElements formElements to fill
     * @param allResponsesInfos allResponsesInfos to fill
     */
    static formatStorageData = (data: any, formElements: FormElements, allResponsesInfos: any) : void => {
        // Format form elements
        PublicUtils.formatFormElements(data.formElements, formElements);

        // Format mapping
        allResponsesInfos.clear();
        for (let responseInfo of data.allResponsesInfos) {
            let isSection = responseInfo[0].description !== undefined;
            let key = formElements.filter(e => e instanceof (isSection ? Section : Question) && e.id === responseInfo[0].id)[0];

            let responses = new Responses();
            responses.all = Mix.castArrayAs(Response, responseInfo[1].responses.all);
            let selectedIndexList = responseInfo[1].selectedIndexList;
            let responsesChoicesList = responseInfo[1].responsesChoicesList;
            let value = {
                responses: responses,
                selectedIndexList: selectedIndexList,
                responsesChoicesList: responsesChoicesList
            };

            allResponsesInfos.set(key, value);
        }
    }

    static formatFormElements = (dataFormElements: any, formElements: FormElements) : void => {
        formElements.all = [];

        for (let e of dataFormElements.arr) {
            if (e['description'] !== undefined) {
                formElements.all.push(PublicUtils.formatIntoSection(e));
            }
            else {
                formElements.all.push(PublicUtils.formatIntoQuestion(e));
            }
        }
        formElements.all.sort((a, b) => a.position - b.position);
    };

    static formatIntoSection = (e: FormElement) : Section => {
        let questions = new Questions();
        if (e['questions'] && e['questions'].arr.length > 0) {
            for (let q of e['questions'].arr) {
                questions.all.push(PublicUtils.formatIntoQuestion(q));
            }
        }
        questions.all.sort((a, b) => a.section_position - b.section_position);

        let section = Mix.castAs(Section, e);
        section.questions = questions;
        return section;
    };

    static formatIntoQuestion = (e: FormElement) : Question => {
        let questionChoices = new QuestionChoices();
        if (e['choices'] && e['choices'].all.length > 0) {
            questionChoices.all = Mix.castArrayAs(QuestionChoice, e['choices'].all);
        }
        questionChoices.all.sort((a, b) => a.id - b.id);

        let question = Mix.castAs(Question, e);
        question.choices = questionChoices;
        return question;
    };
}