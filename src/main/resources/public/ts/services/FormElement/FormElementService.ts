import {idiom, ng, notify} from 'entcore';
import {FormElement, Question, Section} from "../../models";
import {questionService} from "./QuestionService";
import {sectionService} from "./SectionService";

export interface FormElementService {
    get(formElement: FormElement) : Promise<any>;
    save(formElement: FormElement) : Promise<any>;
    create(formElement: FormElement) : Promise<any>;
    update(formElement: FormElement) : Promise<any>;
    delete(formElement: FormElement) : Promise<any>;
}

export const formElementService: FormElementService = {

    async get(formElement: FormElement) : Promise<any> {
        try {
            if (formElement instanceof Question) {
                return await questionService.get(formElement.id);
            }
            else if (formElement instanceof Section) {
                return await sectionService.get(formElement.id);
            }
            else {
                throw new TypeError();
            }
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.sectionService.get'));
            throw err;
        }
    },

    async save(formElement: FormElement) : Promise<any> {
        return formElement.id ? await this.update(formElement) : await this.create(formElement);
    },

    async create(formElement: FormElement) : Promise<any> {
        try {
            if (formElement instanceof Question) {
                return await questionService.create(formElement);
            }
            else if (formElement instanceof Section) {
                return await sectionService.create(formElement);
            }
            else {
                throw new TypeError();
            }
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.sectionService.create'));
            throw err;
        }
    },

    async update(formElement: FormElement) : Promise<any> {
        try {
            if (formElement instanceof Question) {
                return await questionService.update(formElement);
            }
            else if (formElement instanceof Section) {
                return await sectionService.update(formElement);
            }
            else {
                throw new TypeError();
            }
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.sectionService.update'));
            throw err;
        }
    },

    async delete(formElement: FormElement) : Promise<any> {
        try {
            if (formElement instanceof Question) {
                return await questionService.delete(formElement.id);
            }
            else if (formElement instanceof Section) {
                return await sectionService.delete(formElement.id);
            }
            else {
                throw new TypeError();
            }
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.sectionService.delete'));
            throw err;
        }
    }
};

export const FormElementService = ng.service('FormElementService',(): FormElementService => formElementService);