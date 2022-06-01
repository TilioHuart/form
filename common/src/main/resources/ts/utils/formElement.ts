import {
    Form,
    FormElement, FormElements,
    Question,
    Response, Responses,
    Section,
    Distribution, Questions
} from "../models";
import {Mix} from "entcore-toolkit";
import {Direction} from "../core/enums";
import {angular, idiom, notify} from "entcore";
import {formElementService, questionService} from "../services";

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

    static switchPositions = (formElements: FormElements|Questions, index: number, direction: string) : void => {
        switch (direction) {
            case Direction.UP: {
                if (formElements instanceof Questions) {
                    formElements.all[index].section_position--;
                    formElements.all[index - 1].section_position++;
                }
                else {
                    formElements.all[index].position--;
                    formElements.all[index - 1].position++;
                }
                break;
            }
            case Direction.DOWN: {
                if (formElements instanceof Questions) {
                    formElements.all[index].section_position++;
                    formElements.all[index + 1].section_position--;
                }
                else {
                    formElements.all[index].position++;
                    formElements.all[index + 1].position--;
                }
                break;
            }
            default:
                notify.error(idiom.translate('formulaire.error.question.reorganization'));
                break;
        }
    }

    // Checking functions for validation form ending

    static hasRespondedLastQuestion = async (form: Form, distribution: Distribution) : Promise<boolean> => {
        let formElements = new FormElements();
        let responses = new Responses();
        await formElements.sync(form.id);
        if (FormElementUtils.isLastElementValid(formElements)) {
            return true;
        }
        else {
            let lastQuestions = FormElementUtils.getLastConditionalQuestions(formElements);
            await responses.syncByDistribution(distribution.id);
            return responses.all.filter(r => FormElementUtils.isValidLastResponse(r, lastQuestions)).length > 0;
        }
    };

    static isLastElementValid = (formElements: FormElements) : boolean => {
        let lastElement = formElements.all[formElements.all.length - 1];
        let isLastElementValidSection = lastElement instanceof Section && lastElement.questions.all.filter(q => q.conditional).length === 0;
        let isLastElementValidQuestion = lastElement instanceof Question && !lastElement.conditional;
        return isLastElementValidSection || isLastElementValidQuestion;
    };

    static getLastConditionalQuestions = (formElements: FormElements) : any => {
        let lastQuestions = [];
        for (let e of formElements.all) {
            if (e instanceof Question) {
                if (e.conditional && e.choices.all.filter(c => !c.next_section_id).length > 0) {
                    lastQuestions.push(e);
                }
            }
            else if (e instanceof Section) {
                let conditionalQuestions = e.questions.all.filter(q => q.conditional);
                if (conditionalQuestions.length === 1 && conditionalQuestions[0].choices.all.filter(c => !c.next_section_id).length > 0) {
                    lastQuestions.push(conditionalQuestions[0]);
                }
            }
        }
        return lastQuestions;
    };

    static isValidLastResponse = (response: Response, lastQuestions: any) : boolean => {
        let matchingQuestions = lastQuestions.filter(q => q.id === response.question_id);
        let question = matchingQuestions.length > 0 ? matchingQuestions[0] : null;
        return question && (question.conditional ? !!response.answer : true);
    };

    // Drag and drop

    static onEndDragAndDrop = async (evt: any, formElements: FormElements) : Promise<boolean> => {
        let scopElem = angular.element(evt.item.firstElementChild.firstElementChild).scope().vm;
        let itemId = scopElem.question ? scopElem.question.id : scopElem.section.id;
        let newNestedSectionId = evt.to.id.split("-")[1] != "0" ? parseInt(evt.to.id.split("-")[1]) : null;
        let oldNestedContainerId = evt.from.id.split("-")[1] != "0" ? parseInt(evt.from.id.split("-")[1]) : null;
        let oldSection = oldNestedContainerId ? (formElements.all.filter(e => e instanceof Section && e.id === oldNestedContainerId)[0]) as Section: null;
        let item = null;
        if (scopElem.section) {
            item = formElements.all.filter(q => q.id === itemId)[0] as Section;
        }
        else {
            let oldSiblings = oldSection ? oldSection.questions : formElements;
            item = (oldSiblings as any).all.filter(q => q.id === itemId)[0] as Question;
        }
        let oldIndex = evt.oldIndex;
        let newIndex = evt.newIndex;
        let indexes = FormElementUtils.getStartEndIndexes(newIndex, oldIndex);
        let cleanResidue = false;

        if (newNestedSectionId && item instanceof Section) {
            return false;
        }

        if (!newNestedSectionId) {
            if (oldSection) { // Item moved FROM oldSection TO vm.formElements
                FormElementUtils.updateSiblingsPositions(oldSection.questions, false, null, oldIndex);
                FormElementUtils.updateSiblingsPositions(formElements, true, null, newIndex);
                item.position = newIndex + 1;
                item.section_id = null;
                item.section_position = null;
                await questionService.update(oldSection.questions.all);
                await formElementService.update(formElements.all);
                cleanResidue = true;
            }
            else { // Item moved FROM vm.formElements TO vm.formElements
                FormElementUtils.updateSiblingsPositions(formElements, true, indexes.goUp, indexes.startIndex, indexes.endIndex);
                item.position = newIndex + 1;
                item.section_id = null;
                item.section_position = null;
                await formElementService.update(formElements.all);
            }
        }
        else {
            let newSection = (formElements.all.filter(e => e instanceof Section && e.id === newNestedSectionId)[0]) as Section;
            if (oldSection) { // Item moved FROM oldSection TO section with id 'newNestedSectionId'
                if (newSection.id != oldSection.id) {
                    FormElementUtils.updateSiblingsPositions(oldSection.questions, false, null, oldIndex);
                    FormElementUtils.updateSiblingsPositions(newSection.questions, true, null, newIndex);
                }
                else {
                    FormElementUtils.updateSiblingsPositions(newSection.questions, true, indexes.goUp, indexes.startIndex, indexes.endIndex);
                }
                item.position = null;
                item.section_id = newNestedSectionId;
                item.section_position = newIndex + 1;
                if (newSection.id != oldSection.id) {
                    oldSection.questions.all = oldSection.questions.all.filter(q => q.id != item.id);
                    newSection.questions.all.push(item);
                }
                await formElementService.update(newSection.questions.all.concat(oldSection.questions.all));
            }
            else { // Item moved FROM vm.formElements TO section with id 'newNestedSectionId'
                FormElementUtils.updateSiblingsPositions(formElements, false, null, oldIndex);
                FormElementUtils.updateSiblingsPositions(newSection.questions, true, null, newIndex);
                item.position = null;
                item.section_id = newNestedSectionId;
                item.section_position = newIndex + 1;
                newSection.questions.all.push(item);
                await questionService.update(newSection.questions.all);
                await formElementService.update(formElements.all);
            }
        }

        return cleanResidue;
    };

    static onEndOrgaDragAndDrop = async (evt: any, formElements: FormElements) : Promise<boolean> => {
        let elem = evt.item.firstElementChild.firstElementChild;
        let scopElem = angular.element(elem).scope().vm;
        let itemId = scopElem.formElement.id;
        let newNestedSectionId = evt.to.id.split("-")[2] != "0" ? parseInt(evt.to.id.split("-")[2]) : null;
        newNestedSectionId ? elem.classList.add("sectionChild") : elem.classList.remove("sectionChild");
        let oldNestedContainerId = evt.from.id.split("-")[2] != "0" ? parseInt(evt.from.id.split("-")[2]) : null;
        let oldSection = oldNestedContainerId ? (formElements.all.filter(e => e instanceof Section && e.id === oldNestedContainerId)[0]) as Section: null;
        let item = null;
        if (scopElem.section) {
            item = formElements.all.filter(e => e.id === itemId)[0] as Section;
        }
        else {
            let oldSiblings = oldSection ? oldSection.questions : formElements;
            item = (oldSiblings as any).all.filter(q => q.id === itemId)[0] as Question;
        }
        let oldIndex = evt.oldIndex;
        let newIndex = evt.newIndex;
        let indexes = FormElementUtils.getStartEndIndexes(newIndex, oldIndex);

        if (newNestedSectionId && item instanceof Section) {
            return true;
        }

        if (!newNestedSectionId) {
            if (oldSection) { // Item moved FROM oldSection TO vm.formElements
                FormElementUtils.updateSiblingsPositions(oldSection.questions, false, null, oldIndex);
                FormElementUtils.updateSiblingsPositions(formElements, true, null, newIndex);
                item.position = newIndex + 1;
                item.section_id = null;
                item.section_position = null;
                oldSection.questions.all = oldSection.questions.all.filter(q => q.id != item.id);
                formElements.all.push(item);
                oldSection.questions.all.sort((a, b) => a.section_position - b.section_position);
            }
            else { // Item moved FROM vm.formElements TO vm.formElements
                FormElementUtils.updateSiblingsPositions(formElements, true, indexes.goUp, indexes.startIndex, indexes.endIndex);
                item.position = newIndex + 1;
                item.section_id = null;
                item.section_position = null;
            }
        }
        else {
            let newSection = (formElements.all.filter(e => e instanceof Section && e.id === newNestedSectionId)[0]) as Section;
            if (oldSection) { // Item moved FROM oldSection TO section with id 'newNestedSectionId'
                if (newSection.id != oldSection.id) {
                    FormElementUtils.updateSiblingsPositions(oldSection.questions, false, null, oldIndex);
                    FormElementUtils.updateSiblingsPositions(newSection.questions, true, null, newIndex);
                }
                else {
                    FormElementUtils.updateSiblingsPositions(newSection.questions, true, indexes.goUp, indexes.startIndex, indexes.endIndex);
                }
                item.position = null;
                item.section_id = newNestedSectionId;
                item.section_position = newIndex + 1;
                if (newSection.id != oldSection.id) {
                    oldSection.questions.all = oldSection.questions.all.filter(q => q.id != item.id);
                    newSection.questions.all.push(item);
                    oldSection.questions.all.sort((a, b) => a.section_position - b.section_position);
                    newSection.questions.all.sort((a, b) => a.section_position - b.section_position);
                }
            }
            else { // Item moved FROM vm.formElements TO section with id 'newNestedSectionId'
                FormElementUtils.updateSiblingsPositions(formElements, false, null, oldIndex);
                FormElementUtils.updateSiblingsPositions(newSection.questions, true, null, newIndex);
                item.position = null;
                item.section_id = newNestedSectionId;
                item.section_position = newIndex + 1;
                newSection.questions.all.push(item);
                formElements.all = formElements.all.filter(e => e.id != item.id);
                newSection.questions.all.sort((a, b) => a.section_position - b.section_position);
            }
        }

        formElements.all.sort((a, b) => a.position - b.position);
        return false;
    };

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

    static getStartEndIndexes = (newIndex: number, oldIndex: number) : any => {
        let indexes = {startIndex: -1, endIndex: -1, goUp: false};
        if (newIndex < oldIndex) {
            indexes.goUp = true;
            indexes.startIndex = newIndex;
            indexes.endIndex = oldIndex;
        }
        else {
            indexes.goUp = false;
            indexes.startIndex = oldIndex;
            indexes.endIndex = newIndex + 1;
        }
        return indexes;
    }
}