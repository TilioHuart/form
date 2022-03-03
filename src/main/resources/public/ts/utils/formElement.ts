import {FormElement, Question, Section} from "../models";
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
}