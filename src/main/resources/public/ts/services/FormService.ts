import {idiom, ng, notify} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {Form} from '../models/Form';

export interface FormService {
    list(): Promise<AxiosResponse>;
    listSentForms(): Promise<AxiosResponse>;
    get(formId: number): Promise<AxiosResponse>;
    save(form: Form): Promise<AxiosResponse>;
    create(form: Form): Promise<AxiosResponse>;
    update(form: Form): Promise<AxiosResponse>;
    archive(form: Form): Promise<AxiosResponse>;
    restore(form: Form): Promise<AxiosResponse>;
    delete(formId: number): Promise<AxiosResponse>;
    unshare(formId: number): Promise<AxiosResponse>;
    getInfoImage(form: Form): Promise<AxiosResponse>;
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

    async get(formId : number): Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/forms/${formId}`);
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

    async archive(form : Form): Promise<AxiosResponse> {
        try {
            form.archived = true;
            form.sent = false;
            return await this.update(form);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.archive'));
            throw err;
        }
    },

    async restore(form : Form): Promise<AxiosResponse> {
        try {
            form.archived = false;
            return await this.update(form);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.restore'));
            throw err;
        }
    },

    async delete(formId : number): Promise<AxiosResponse> {
        try {
            return await http.delete(`/formulaire/forms/${formId}`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.delete'));
            throw err;
        }
    },

    async unshare(formId : number): Promise<AxiosResponse> {
        try {
            let emptyBody = {"users":{},"groups":{},"bookmarks":{}};
            return await http.put(`/formulaire/share/resource/${formId}`, emptyBody);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.unshare'));
            throw err;
        }
    },

    async getInfoImage(form : Form): Promise<AxiosResponse> {
        try {
            return await http.get(`/formulaire/info/image/${form.picture ? form.picture.split("/").slice(-1)[0] : null}`);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.formService.image'));
            throw e;
        }
    }
};

export const FormService = ng.service('FormService', (): FormService => formService);