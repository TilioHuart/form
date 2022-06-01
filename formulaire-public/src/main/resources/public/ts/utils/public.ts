import {
    FormElement,
    FormElements,
    Question,
    QuestionChoice,
    QuestionChoices,
    Questions,
    Response, Responses,
    Section
} from "@common/models";
import {Mix} from "entcore-toolkit";

export class PublicUtils {
    /**
     * Returns an array of colors interpolated between a list of given colors
     * @param dataFormElements sessionStorage data transformed into JSON
     * @param formElements formElements to fill
     * @param dataResponsesInfos sessionStorage data transformed into JSON
     * @param allResponsesInfos allResponsesInfos to fill
     */
    static formatStorageData = (dataFormElements: any, formElements: FormElements, dataResponsesInfos: any, allResponsesInfos: any) : void => {
        // Format form elements
        PublicUtils.formatFormElements(dataFormElements, formElements);

        // Format mapping
        allResponsesInfos.clear();
        for (let responseInfo of dataResponsesInfos) {
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

    // Cookies

    static getCookie = (name: string) : string => {
        let indexOfCookieName = document.cookie.indexOf(name+'=');
        if (indexOfCookieName >= 0) {
            let startIndexOfCookieValue = indexOfCookieName + name.length + 1;
            let endIndexOfCookieValue = document.cookie.indexOf(';', startIndexOfCookieValue);
            if (endIndexOfCookieValue < 0) { endIndexOfCookieValue = document.cookie.length; }

            if (startIndexOfCookieValue >= 0 && endIndexOfCookieValue > startIndexOfCookieValue) {
                return document.cookie.substring(startIndexOfCookieValue, endIndexOfCookieValue);
            }
        }

        return null;
    };

    static setCookie = (name: string, value: string, expires?: Date, path= '/', domain?: string, secure = false, sameSite = 'strict') : void => {
        let cookie = `${name} = ${value}`;
        if (expires) { cookie += `; expires = ${expires.toUTCString()}` }
        if (path) { cookie += `; path = ${path}` }
        if (domain) { cookie += `; domain = ${domain}` }
        if (secure) { cookie += `; secure = ${secure}` }
        if (sameSite) { cookie += `; samesite = ${sameSite}` }
        document.cookie = cookie;
    };
}