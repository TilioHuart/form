import {idiom, moment, ng, notify} from 'entcore';
import http from 'axios';
import {Form} from '../models';
import {DataUtils} from "../utils";
import {Exports} from "../core/enums";
import {DateFormats} from "@common/core/constants";

export interface FormService {
    list() : Promise<any>;
    listSentForms() : Promise<any>;
    listForLinker() : Promise<any>;
    get(formId: number) : Promise<any>;
    save(form: Form) : Promise<any>;
    create(form: Form) : Promise<any>;
    createMultiple(forms: Form[]) : Promise<any>;
    duplicate(formIds: number[], folderId: number) : Promise<any>;
    update(form: Form) : Promise<any>;
    archive(form: Form, destinationFolderId: number) : Promise<any>;
    restore(forms: Form[]) : Promise<any>;
    delete(formId: number) : Promise<any>;
    move(formIds : number[], parentId: number) : Promise<any>;
    sendReminder(formId: number, mail: {}) : Promise<any>;
    export(formId: number, type: string, images?: any) : Promise<any>;
    unshare(formId: number) : Promise<any>;
    getMyFormRights(formId: number) : Promise<any>;
    getAllMyFormRights() : Promise<any>;
}

export const formService: FormService = {
    async list() : Promise<any> {
        try {
            return DataUtils.getData(await http.get('/formulaire/forms'));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.list'));
            throw err;
        }
    },

    async listSentForms() : Promise<any> {
        try {
            return DataUtils.getData(await http.get('/formulaire/forms/sent'));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.list'));
            throw err;
        }
    },

    async listForLinker() : Promise<any> {
        try {
            return DataUtils.getData(await http.get('/formulaire/forms/linker'));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.list'));
            throw err;
        }
    },

    async get(formId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/forms/${formId}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.get'));
            throw err;
        }
    },

    async save(form: Form) : Promise<any> {
        if (form.date_opening != null) {
            form.date_opening = moment(form.date_opening.setHours(0,0,0,0)).format(DateFormats.YYYY_MM_DD_T_HH_mm_ss);
        }
        if (form.date_ending != null) {
            form.date_ending = moment(form.date_ending.setHours(23,59,59,999)).format(DateFormats.YYYY_MM_DD_T_HH_mm_ss);
        }
        return form.id ? await this.update(form) : await this.create(form);
    },

    async create(form: Form) : Promise<any> {
        try {
            return DataUtils.getData(await http.post('/formulaire/forms', form));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.create'));
            throw err;
        }
    },

    async createMultiple(forms: Form[]) : Promise<any> {
        try {
            return DataUtils.getData(await http.post('/formulaire/forms/multiple', forms));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.createMultiple'));
            throw err;
        }
    },

    async duplicate(formIds: number[], folderId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.post(`/formulaire/forms/duplicate/${folderId}`, formIds));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.duplicate'));
            throw err;
        }
    },

    async update(form: Form) : Promise<any> {
        try {
            return DataUtils.getData(await http.put(`/formulaire/forms/${form.id}`, form));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.update'));
            throw err;
        }
    },

    async archive(form: Form, destinationFolderId: number) : Promise<any> {
        try {
            form.archived = true;
            await this.move([form.id], destinationFolderId);
            return await this.update(form);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.archive'));
            throw err;
        }
    },

    async restore(forms: Form[]) : Promise<any> {
        try {
            return DataUtils.getData(await http.put(`/formulaire/forms/restore`, forms.map(f => f.id)));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.restore'));
            throw err;
        }
    },

    async delete(formId: number) : Promise<any> {
        try {
            await this.unshare(formId);
            return DataUtils.getData(await http.delete(`/formulaire/forms/${formId}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.delete'));
            throw err;
        }
    },

    async move(formIds, parentId) : Promise<any> {
        try {
            return DataUtils.getData(await http.put(`/formulaire/forms/move/${parentId}`, formIds));
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.formService.move'));
            throw e;
        }
    },

    async sendReminder(formId: number, mail: {}) : Promise<any> {
        try {
            return DataUtils.getData(await http.post(`/formulaire/forms/${formId}/remind`, mail));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.remind'));
            throw err;
        }
    },

    async export(formId: number, type: string, images: any) : Promise<any> {
        try {
            if (type === Exports.CSV) {
                return await http.post(`/formulaire/forms/${formId}/export/csv`, {});
            }
            else if (type === Exports.PDF) {
                return await http.post(`/formulaire/forms/${formId}/export/pdf`, images, {responseType: "arraybuffer"});
            }
            else {
                notify.error(idiom.translate('formulaire.error.formService.export'));
            }
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.export'));
            throw err;
        }
    },

    async unshare(formId: number) : Promise<any> {
        try {
            let emptyBody = {"users":{},"groups":{},"bookmarks":{}};
            return DataUtils.getData(await http.put(`/formulaire/share/resource/${formId}`, emptyBody));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.unshare'));
            throw err;
        }
    },

    async getMyFormRights(formId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/forms/${formId}/rights`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.get'));
            throw err;
        }
    },

    async getAllMyFormRights() : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/forms/rights/all`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.get'));
            throw err;
        }
    }
};

export const FormService = ng.service('FormService', (): FormService => formService);