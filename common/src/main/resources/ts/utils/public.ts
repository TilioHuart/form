import {
    FormElement,
    FormElements, IQuestionResponse,
    Question,
    QuestionChoice,
    QuestionChoices,
    Questions,
    Response, Responses,
    Section, Types
} from "@common/models";
import {Mix} from "entcore-toolkit";
import {Fields} from "@common/core/constants";
import {QuestionSpecificFields} from "@common/models/QuestionSpecificFields";
import {FormElementType} from "@common/core/enums/form-element-type";

export class PublicUtils {
    /**
     * Format storage data
     * @param dataFormElements sessionStorage data transformed into JSON
     * @param formElements formElements to fill
     * @param dataResponsesInfos sessionStorage data transformed into JSON
     * @param allResponsesInfos allResponsesInfos to fill
     */
    static formatStorageData = (dataFormElements: any, formElements: FormElements, dataResponsesInfos: any, allResponsesInfos: Map<FormElement, Map<Question, Responses>>) : void => {
        PublicUtils.formatFormElements(dataFormElements, formElements); // Format form elements
        PublicUtils.formatAllResponsesInfos(formElements, dataResponsesInfos, allResponsesInfos); // Format mapping
    }

    static formatAllResponsesInfos = (formElements: FormElements, dataResponsesInfos: any, allResponsesInfos: Map<FormElement, Map<Question, Responses>>) : void => {
        allResponsesInfos.clear();
        for (let formElementMap of dataResponsesInfos) {
            let key: any = formElementMap[0];
            let isSection: boolean = key.description !== undefined;
            let formElement: FormElement = formElements.all.find((e: FormElement) => e instanceof (isSection ? Section : Question) && e.id === key.id);
            allResponsesInfos.set(formElement, new Map());

            for (let questionMap of formElementMap[1]) {
                let question: Question = isSection
                    ? (formElement as Section).questions.all.filter((q:Question) => q.id == questionMap[0].id)[0]
                    : (formElement as Question);
                let questionResponses: Responses = new Responses();
                questionResponses.all = Mix.castArrayAs(Response, questionMap[1].arr);
                allResponsesInfos.get(formElement).set(question, questionResponses);
            }
        }
    }

    static formatFormElements = (dataFormElements: any, formElements: FormElements) : void => {
        formElements.all = [];

        for (let e of dataFormElements.arr) {
            if (e[Fields.DESCRIPTION] !== undefined) {
                formElements.all.push(PublicUtils.formatIntoSection(e));
            }
            else {
                formElements.all.push(PublicUtils.formatIntoQuestion(e));
            }
        }
        formElements.all.sort((a: FormElement, b: FormElement) => a.position - b.position);
    };

    static formatIntoSection = (e: FormElement) : Section => {
        let questions: Questions = new Questions();
        if (e[Fields.QUESTIONS] && e[Fields.QUESTIONS].arr.length > 0) {
            for (let q of e[Fields.QUESTIONS].arr) {
                questions.all.push(PublicUtils.formatIntoQuestion(q));
            }
        }
        questions.all.sort((a: Question, b: Question) => a.section_position - b.section_position);

        let section: Section = Mix.castAs(Section, e);
        section.questions = questions;
        section.form_element_type = FormElementType.SECTION;
        return section;
    };

    static formatIntoQuestion = (e: FormElement) : Question => {
        let questionChoices: QuestionChoices = new QuestionChoices();
        if (e[Fields.CHOICES] && e[Fields.CHOICES].all.length > 0) {
            questionChoices.all = Mix.castArrayAs(QuestionChoice, e[Fields.CHOICES].all);
        }
        questionChoices.all.sort((a: QuestionChoice, b: QuestionChoice) => a.position - b.position);

        let question: Question = Mix.castAs(Question, e);
        if (question.question_type === Types.CURSOR) this.formatIntoQuestionCursor(question, e);

        question.form_element_type = FormElementType.QUESTION;
        question.choices = questionChoices;
        return question;
    };

    static formatIntoQuestionCursor = (q: Question, e: FormElement) : void => {
        q.specific_fields = new QuestionSpecificFields(q.id);
        q.specific_fields.cursor_min_val = e[Fields.SPECIFIC_FIELDS][Fields.CURSOR_MIN_VAL];
        q.specific_fields.cursor_max_val = e[Fields.SPECIFIC_FIELDS][Fields.CURSOR_MAX_VAL];
        q.specific_fields.cursor_step = e[Fields.SPECIFIC_FIELDS][Fields.CURSOR_STEP];
        q.specific_fields.cursor_min_label = e[Fields.SPECIFIC_FIELDS][Fields.CURSOR_MIN_LABEL];
        q.specific_fields.cursor_max_label = e[Fields.SPECIFIC_FIELDS][Fields.CURSOR_MAX_LABEL];
    }

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

    static setCookie = (name: string, value: string, expires?: Date, path: string= '/', domain?: string, secure: boolean = false, sameSite: string = 'strict') : void => {
        let cookie = `${name} = ${value}`;
        if (expires) { cookie += `; ${Fields.EXPIRES} = ${expires.toUTCString()}` }
        if (path) { cookie += `; ${Fields.PATH} = ${path}` }
        if (domain) { cookie += `; ${Fields.DOMAIN} = ${domain}` }
        if (secure) { cookie += `; ${Fields.SECURE} = ${secure}` }
        if (sameSite) { cookie += `; ${Fields.SAMESITE} = ${sameSite}` }
        document.cookie = cookie;
    };
}