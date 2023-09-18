import {Mix} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {questionChoiceService} from "../services";
import {FormElementType} from "@common/core/enums/form-element-type";
import {FormElement, FormElements, Question} from "@common/models/FormElement";

export enum ChoiceTypes {
    TXT = 'TXT',
    IMAGE = 'IMAGE',
    VIDEO = 'VIDEO'
}

export class QuestionChoice {
    id: number;
    question_id: number;
    value: string;
    position: number;
    type: ChoiceTypes;
    next_form_element: FormElement;
    next_form_element_id: number;
    next_form_element_type: FormElementType;
    is_next_form_element_default: boolean;
    is_custom: boolean;
    nbResponses: number;
    image?: string;

    constructor (questionId?: number, position: number = 0, value?: string, image?: string, isCustom?: boolean) {
        this.id = null;
        this.question_id = questionId ? questionId : null;
        this.value = value ? value : "";
        this.position = position;
        this.type = ChoiceTypes.TXT;
        this.next_form_element = null;
        this.next_form_element_id = null;
        this.next_form_element_type = null;
        this.is_next_form_element_default = true;
        this.is_custom = isCustom ? isCustom : false;
        this.nbResponses = 0;
        this.image = image ? image : null;
    }

    build(data: IQuestionChoiceResponse) : QuestionChoice {
        this.id = data.id ? data.id : null;
        this.question_id = data.questionId ? data.questionId : null;
        this.value = data.value ? data.value : "";
        this.position = data.position ? data.position : null;
        this.type = ChoiceTypes.TXT;
        this.next_form_element = null;
        this.next_form_element_id = data.nextFormElementId ? data.nextFormElementId : null;
        this.next_form_element_type = data.nextFormElementType ? data.nextFormElementType : null;
        this.is_next_form_element_default = data.isNextFormElementDefault ? data.isNextFormElementDefault : true;
        this.is_custom = data.isCustom ? data.isCustom : null;
        this.nbResponses = 0;
        this.image = data.image ? data.image : null;
        return this;
    }

    toJson() : Object {
        return {
            id: this.id,
            question_id: this.question_id,
            value: this.value,
            position: this.position,
            type: this.type,
            next_form_element: this.next_form_element,
            next_form_element_id: this.next_form_element_id,
            next_form_element_type: this.next_form_element_type,
            is_next_form_element_default: this.is_next_form_element_default,
            is_custom: this.is_custom,
            image: this.image
        }
    }

    setNextFormElementValuesWithDefault = (formElements: FormElements, parentQuestion: Question) : void => {
        this.next_form_element = parentQuestion.getFollowingFormElement(formElements);
        this.next_form_element_id = this.next_form_element ? this.next_form_element.id : null;
        this.next_form_element_type = this.next_form_element ? this.next_form_element.form_element_type : null;
        this.is_next_form_element_default = true;
    }

    getNextFormElement = (formElements: FormElements, parentQuestion?: Question) : FormElement => {
        if (this.is_next_form_element_default) return parentQuestion.getFollowingFormElement(formElements);

        return formElements.all.find((e: FormElement) =>
            e.id === this.next_form_element_id &&
            e.form_element_type === this.next_form_element_type
        );
    }

    getNextFormElementPosition = (formElements: FormElements) : number => {
        let nextFormElement: FormElement = this.getNextFormElement(formElements);
        return nextFormElement ? nextFormElement.position : null;
    }
}

export class QuestionChoices {
    all: QuestionChoice[];

    constructor() {
        this.all = [];
    }

    build(data: IQuestionChoiceResponse[]) : QuestionChoices {
        this.all = data.map((qcr: IQuestionChoiceResponse) => new QuestionChoice().build(qcr));
        return this;
    }

    sync = async (questionId: number) : Promise<void> => {
        try {
            let data = await questionChoiceService.list(questionId);
            this.all = Mix.castArrayAs(QuestionChoice, data);
            this.all.sort((a: QuestionChoice, b: QuestionChoice) => a.position - b.position);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.questionChoice.sync'));
            throw e;
        }
    }

    replaceSpace = () : void => {
        for (let i = 0; i < this.all.length; i++) {
            this.all[i].value = this.all[i].value.replace(/\u00A0/," ");
        }
    }
}

export class QuestionChoicePayload {
    id: number;
    question_id: number;
    value: string;
    position: number;
    type: ChoiceTypes;
    next_form_element_id: number;
    next_form_element_type: FormElementType;
    is_next_form_element_default: boolean;
    is_custom: boolean;
    image: string;

    constructor (questionChoice: QuestionChoice) {
        this.id = questionChoice.id ? questionChoice.id : null;
        this.question_id = questionChoice.question_id ? questionChoice.question_id : null;
        this.value = questionChoice.value ? questionChoice.value : "";
        this.position = questionChoice.position ? questionChoice.position : null;
        this.type = questionChoice.type ? questionChoice.type : ChoiceTypes.TXT;
        this.next_form_element_id = questionChoice.next_form_element_id ? questionChoice.next_form_element_id : null;
        this.next_form_element_type = questionChoice.next_form_element_type ? questionChoice.next_form_element_type : null;
        this.is_next_form_element_default = questionChoice.is_next_form_element_default ? questionChoice.is_next_form_element_default : false;
        this.is_custom = questionChoice.is_custom ? questionChoice.is_custom : false;
        this.image = questionChoice.image ? questionChoice.image : null;
    }

    toJson() : Object {
        return {
            id: this.id,
            question_id: this.question_id,
            value: this.value,
            position: this.position,
            type: this.type,
            next_form_element_id: this.next_form_element_id,
            next_form_element_type: this.next_form_element_type,
            is_next_form_element_default: this.is_next_form_element_default,
            is_custom: this.is_custom,
            image: this.image
        }
    }
}

export interface IQuestionChoiceResponse {
    id: number;
    questionId: number;
    value: string;
    position: number,
    type: ChoiceTypes;
    nextFormElementId: number;
    nextFormElementType: FormElementType;
    isNextFormElementDefault: boolean;
    isCustom: boolean;
    image: string;
}