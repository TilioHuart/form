import {idiom, ng, notify} from 'entcore';
import {FormElement, Question, Section} from "../../models";
import {questionService} from "./QuestionService";
import {sectionService} from "./SectionService";
import {DataUtils, FormElementUtils} from "../../utils";
import http from "axios";

export interface FormElementService {
    countFormElements(formId: number) : Promise<any>;
    get(formElement: FormElement) : Promise<any>;
    getByPosition(idForm: number, position: number) : Promise<any>;
    save(formElement: FormElement) : Promise<any>;
    create(formElement: FormElement) : Promise<any>;
    update(formElement: FormElement) : Promise<any>;
    delete(formElement: FormElement) : Promise<any>;
}

export const formElementService: FormElementService = {

    async countFormElements (formId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/forms/${formId}/elements/count`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formElementService.count'));
            throw err;
        }
    },

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
            notify.error(idiom.translate('formulaire.error.formElementService.get'));
            throw err;
        }
    },

    async getByPosition(idForm : number, position: number) : Promise<any> {
        try {
            let formElement = DataUtils.getData(await http.get(`/formulaire/forms/${idForm}/elements/${position}`));
            return FormElementUtils.castFormElement(formElement);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formElementService.get'));
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
            notify.error(idiom.translate('formulaire.error.formElementService.create'));
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
            notify.error(idiom.translate('formulaire.error.formElementService.update'));
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
            notify.error(idiom.translate('formulaire.error.formElementService.delete'));
            throw err;
        }
    }
};

export const FormElementService = ng.service('FormElementService',(): FormElementService => formElementService);