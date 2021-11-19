import {idiom, moment, ng, notify} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {Form} from '../models';

export interface FormService {
    list() : Promise<AxiosResponse>;
    listSentForms() : Promise<AxiosResponse>;
    get(formId: number) : Promise<AxiosResponse>;
    save(form: Form) : Promise<AxiosResponse>;
    create(form: Form) : Promise<AxiosResponse>;
    createMultiple(forms: Form[]) : Promise<AxiosResponse>;
    duplicate(formIds: number[], folderId: number) : Promise<AxiosResponse>;
    update(form: Form) : Promise<AxiosResponse>;
    archive(form: Form, destinationFolderId: number) : Promise<AxiosResponse>;
    restore(form: Form, destinationFolderId: number) : Promise<AxiosResponse>;
    delete(formId: number) : Promise<AxiosResponse>;
    move(forms : Form[], parentId: number) : Promise<AxiosResponse>;
    sendReminder(formId: number, mail: {}) : Promise<AxiosResponse>;
    unshare(formId: number) : Promise<AxiosResponse>;
    getMyFormRights(formId: number) : Promise<AxiosResponse>;
    getAllMyFormRights() : Promise<AxiosResponse>;
    getInfoImage(form: Form) : Promise<AxiosResponse>;
}

export const formService: FormService = {

    async list() : Promise<AxiosResponse> {
        try {
            return http.get('/formulaire/forms');
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.list'));
            throw err;
        }
    },

    async listSentForms() : Promise<AxiosResponse> {
        try {
            return http.get('/formulaire/sentForms');
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.list'));
            throw err;
        }
    },

    async get(formId: number) : Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/forms/${formId}`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.get'));
            throw err;
        }
    },

    async save(form: Form) : Promise<AxiosResponse> {
        if (form.date_opening != null) {
            form.date_opening = moment(form.date_opening.setHours(0,0,0,0)).format();
        }
        if (form.date_ending != null) {
            form.date_ending = moment(form.date_ending.setHours(23,59,59,999)).format();
        }
        return form.id ? await this.update(form) : await this.create(form);
    },

    async create(form: Form) : Promise<AxiosResponse> {
        try {
            return http.post('/formulaire/forms', form);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.create'));
            throw err;
        }
    },

    async createMultiple(forms: Form[]) : Promise<AxiosResponse> {
        try {
            return http.post('/formulaire/forms/multiple', forms);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.createMultiple'));
            throw err;
        }
    },

    async duplicate(formIds: number[], folderId: number) : Promise<AxiosResponse> {
        try {
            return http.post(`/formulaire/forms/duplicate/${folderId}`, formIds);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.duplicate'));
            throw err;
        }
    },

    async update(form: Form) : Promise<AxiosResponse> {
        try {
            return http.put(`/formulaire/forms/${form.id}`, form);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.update'));
            throw err;
        }
    },

    async archive(form: Form, destinationFolderId: number) : Promise<AxiosResponse> {
        try {
            form.archived = true;
            await this.move([form], destinationFolderId);
            return await this.update(form);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.archive'));
            throw err;
        }
    },

    async restore(form: Form, destinationFolderId: number) : Promise<AxiosResponse> {
        try {
            form.archived = false;
            await this.move([form], destinationFolderId);
            return await this.update(form);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.restore'));
            throw err;
        }
    },

    async delete(formId: number) : Promise<AxiosResponse> {
        try {
            return await http.delete(`/formulaire/forms/${formId}`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.delete'));
            throw err;
        }
    },

    async move(forms, parentId) : Promise<AxiosResponse> {
        try {
            return http.put(`/formulaire/forms/move/${parentId}`, forms);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.formService.move'));
            throw e;
        }
    },

    async sendReminder(formId: number, mail: {}) : Promise<AxiosResponse> {
        try {
            return await http.post(`/formulaire/forms/${formId}/remind`, mail);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.remind'));
            throw err;
        }
    },


    async unshare(formId: number) : Promise<AxiosResponse> {
        try {
            let emptyBody = {"users":{},"groups":{},"bookmarks":{}};
            return await http.put(`/formulaire/share/resource/${formId}`, emptyBody);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.unshare'));
            throw err;
        }
    },

    async getMyFormRights(formId: number) : Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/forms/${formId}/rights`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.get'));
            throw err;
        }
    },

    async getAllMyFormRights() : Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/forms/rights/all`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.get'));
            throw err;
        }
    },

    async getInfoImage(form: Form) : Promise<AxiosResponse> {
        try {
            return await http.get(`/formulaire/info/image/${form.picture ? form.picture.split("/").slice(-1)[0] : null}`);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.formService.image'));
            throw e;
        }
    }
};

export const FormService = ng.service('FormService', (): FormService => formService);