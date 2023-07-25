import {idiom, ng, notify} from 'entcore';
import http, {CancelToken, CancelTokenSource, CancelTokenStatic} from 'axios';
import {Form} from '../models';
import {DataUtils} from "../utils";
import {Exports} from "../core/enums";
import Axios from "axios";

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
    exportPdf(formIds: number[]) : Promise<any>;
    exportZip(formIds: number[]) : Promise<any>;
    verifyExportAndDownloadZip(exportId: string) : Promise<void>;
    import(zipFile: FormData) : Promise<any>;
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
            form.date_opening = new Date(form.date_opening);
            let utcTimestamp: number = Date.UTC(form.date_opening.getFullYear(), form.date_opening.getMonth(), form.date_opening.getDate(), 0, 0, 0, 0);
            form.date_opening = new Date(utcTimestamp);
        }
        if (form.date_ending != null) {
            form.date_ending = new Date(form.date_ending);
            let utcTimestamp: number = Date.UTC(form.date_ending.getFullYear(), form.date_ending.getMonth(), form.date_ending.getDate(), 23, 59, 59, 999);
            form.date_ending = new Date(utcTimestamp);
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

    async exportPdf(formIds: number[]) : Promise<any> {
        try {
            return await http.get(`/formulaire/forms/export/pdf`, { params: formIds });
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.export'));
            throw err;
        }
    },

    async exportZip(formIds: number[]) : Promise<any> {
        try {
            let res = await http.post(`/formulaire/forms/export/zip`, formIds);
            return res.data.exportId;
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.export'));
            throw err;
        }
    },

    async verifyExportAndDownloadZip(exportId: string) : Promise<void> {
        try {
            await http.get(`/archive/export/verify/${exportId}`);
            window.location.href = `/archive/export/${exportId}`;
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.export'));
            throw err;
        }
    },

    async import(zipFile: FormData) : Promise<any> {
        try {
            const CancelToken: CancelTokenStatic = Axios.CancelToken;
            let source: CancelTokenSource = CancelToken.source();
            let { data } = await http.post(`/archive/import/upload`, zipFile, { headers: { 'Content-Type': 'multipart/form-data' }, cancelToken: source.token });
            let importId: string = data.importId;

            try {
                let analyze = await http.get(`/archive/import/analyze/${importId}`);
                return await http.post(`/archive/import/${importId}/launch`, { "apps": analyze.data.apps });
            } catch (err) {
                notify.error(idiom.translate('formulaire.error.formService.import'));
                throw err;
            }
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.import'));
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