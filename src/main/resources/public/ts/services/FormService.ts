import {idiom, ng, notify} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {Form} from '../models/Form';

export interface FormService {
    list(): Promise<AxiosResponse>;
    listSentForms(): Promise<AxiosResponse>;
    get(number): Promise<AxiosResponse>;
    save(Form): Promise<AxiosResponse>;
    create(Form): Promise<AxiosResponse>;
    update(Form): Promise<AxiosResponse>;
    delete(number): Promise<AxiosResponse>;
    archive(number): Promise<AxiosResponse>;
}

export const formService: FormService = {

    async list (): Promise<AxiosResponse> {
        try {
            return http.get('/formulaire/forms');
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.list'));
            throw err;
        }
    },

    async listSentForms (): Promise<AxiosResponse> {
        try {
            return http.get('/formulaire/sentForms');
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.list'));
            throw err;
        }
    },

    async get(id : number): Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/forms/${id}`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.get'));
            throw err;
        }
    },

    async save(form : Form): Promise<AxiosResponse> {
        return form.id ? await this.update(form) : await this.create(form);
    },

    async create(form : Form): Promise<AxiosResponse> {
        try {
            return http.post('/formulaire/forms', form);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.create'));
            throw err;
        }
    },

    async update(form : Form): Promise<AxiosResponse> {
        try {
            return http.put(`/formulaire/forms/${form.id}`, form);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.update'));
            throw err;
        }
    },

    async delete(id : number): Promise<AxiosResponse> {
        try {
            return await http.delete(`/formulaire/forms/${id}`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.delete'));
            throw err;
        }
    },

    async archive(form : Form): Promise<AxiosResponse> {
        try {
            form.archived = true;
            return await this.update(form);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.update'));
            throw err;
        }
    }
};

export const FormService = ng.service('FormService', (): FormService => formService);