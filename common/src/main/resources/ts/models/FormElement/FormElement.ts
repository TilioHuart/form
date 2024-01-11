import {Selectable} from "entcore-toolkit";
import {FormElementType} from "@common/core/enums/form-element-type";
import {FormElements, Question, QuestionChoice, Section, Types} from "@common/models";

export class FormElementIdType {
    id: number;
    type: FormElementType;

    constructor(id: number, type: FormElementType) {
        this.id = id ? id : null;
        this.type = type ? type : null;
    }

    equals = (formElementIdType: FormElementIdType) : boolean => {
        let areBothUndefined: boolean = this === undefined && formElementIdType === undefined;
        return areBothUndefined || this && formElementIdType && this.id === formElementIdType.id && this.type === formElementIdType.type;
    }

    toString = () : string => {
        return JSON.stringify(this);
    }
}

export abstract class FormElement implements Selectable {
    id: number;
    form_id: number;
    title: string;
    position: number;
    form_element_type: FormElementType;
    selected: boolean;
    label: string;

    protected constructor() {
        this.id = null;
        this.form_id = null;
        this.title = null;
        this.position = null;
        this.selected = null;
        this.label = null;
    }

    build(data: IFormElementResponse) : FormElement {
        this.id = data.id ? data.id : null;
        this.form_id = data.formId ? data.formId : null;
        this.title = data.title ? data.title : null;
        this.position = data.position ? data.position : null;
        this.selected = data.selected ? data.selected : false;
        this.label = data.label ? data.label : null;
        return this;
    }

    toJson() : Object {
        return {
            id: this.id,
            form_id: this.form_id,
            title: this.title,
            position: this.position,
            form_element_type: this.form_element_type,
            selected: this.selected,
            label: this.label
        }
    }

    isSection = () : boolean => {
        return this.form_element_type === FormElementType.SECTION || this instanceof Section;
    }

    isQuestion = () : boolean => {
        return this.form_element_type === FormElementType.QUESTION || this instanceof Question;
    }

    isSameQuestionType = (formElement: FormElement) : boolean => {
        return this instanceof Question && this.isSameQuestionType(formElement);
    }

    isSameQuestionTypeOfType = (formElement: FormElement, type: Types) : boolean => {
        return this instanceof Question && this.isSameQuestionTypeOfType(formElement, type);
    }

    equals = (formElement: FormElement) : boolean => {
        return formElement && this.form_element_type === formElement.form_element_type && this.id === formElement.id;
    }

    getPosition = (formElements: FormElements) : number => {
        return this.position ?
            this.position :
            formElements.all.filter((e: FormElement) => this instanceof Question && e.id === this.section_id)[0].position;
    }

    getFollowingFormElement = (formElements: FormElements) : FormElement => {
        // Case formElement is not formElement but question inside a section
        if (this instanceof Question && this.section_id) {
            let parentSection: Section = this.getParentSection(formElements);
            let followingPosition: number = parentSection ? parentSection.position + 1 : null;
            return formElements.all.find((e: FormElement) => e.position === followingPosition);
        }

        // Case formElement is section without target or just a solo question
        let followingPosition: number = this.position + 1;
        return formElements.all.find((e: FormElement) => e.position === followingPosition);
    }

    getFollowingFormElementPosition = (formElements: FormElements) : number => {
        let nextFormElement: FormElement = this.getFollowingFormElement(formElements);
        return nextFormElement ? nextFormElement.position : null;
    }

    getNextFormElementId = (formElements: FormElements) : number => {
        let nextFormElement: FormElement = this instanceof Section ?
            this.getNextFormElement(formElements) :
            this.getFollowingFormElement(formElements);
        return nextFormElement ? nextFormElement.id : null;
    }

    setTreeNodeLabel = () : void => {
        let label: string = this.title;
        if (this.id && this instanceof Section) {
            if (this.questions.all.length > 0) label += "\n";
            for (let question of this.questions.all) {
                label += "\n- " + question.title;
            }
        }
        this.label = label;
    }

    getIdType = () : FormElementIdType => {
        return new FormElementIdType(this.id, this.form_element_type);
    }

    getAllPotentialNextFormElements = (formElements: FormElements) : FormElement[] => {
        if (this instanceof Section) {
            let conditionalQuestions: Question[] = (this as Section).questions.all.filter((q: Question) => q.conditional);
            if (conditionalQuestions.length > 0) {
                let choices: QuestionChoice[] = (conditionalQuestions as any).flatMap((q: Question) => q.choices.all);
                return choices.map((qc: QuestionChoice) => qc.getNextFormElement(formElements));
            }
            return [this.getFollowingFormElement(formElements)];
        }
        else if (this instanceof Question) {
            return this.conditional ? this.getNextFormElements(formElements) : [this.getFollowingFormElement(formElements)];
        }
        else {
            return [];
        }
    }

    getAllPotentialNextFormElementsIdTypes = (formElements: FormElements) : FormElementIdType[] => {
        let formElementIdTypes: FormElementIdType[] = this.getAllPotentialNextFormElements(formElements).map((e: FormElement) => e ? e.getIdType() : (e as any));
        let isThereUndefined: boolean = formElementIdTypes.some((feit: FormElementIdType) => !feit);
        formElementIdTypes = formElementIdTypes.filter((feit: FormElementIdType) => feit); // filter undefined values
        let uniqueFormElementIdTypes: FormElementIdType[] = [];

        for (let formElementIdType of formElementIdTypes) {
            let match: FormElementIdType = uniqueFormElementIdTypes.find((feit: FormElementIdType) => feit && feit.equals(formElementIdType));
            if (!match) uniqueFormElementIdTypes.push(formElementIdType);
        }

        if (isThereUndefined) uniqueFormElementIdTypes.push(undefined);
        return uniqueFormElementIdTypes;
    }

    getCurrentLongestPath = (longestPathsMap: Map<string, number>) : number => {
        return longestPathsMap.get(new FormElementIdType(this.id, this.form_element_type).toString());
    }

    abstract getPayload(): FormElementPayload;
}

export interface FormElementPayload {
    id: number;
    form_id: number;
    title: string;
    position: number;
    form_element_type: FormElementType;
}

export interface IFormElementResponse {
    id: number;
    formId: number;
    title: string;
    position: number,
    selected: boolean;
    label: string;
}