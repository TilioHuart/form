import {Question, Section} from "../models";
import {Mix} from "entcore-toolkit";

export class FormElementUtil {
    static castFormElement(formElement: any) : Question|Section {
        if (formElement.statement != undefined) {
            return Mix.castAs(Question, formElement);
        }
        else {
            return Mix.castAs(Section, formElement);
        }
    };
}