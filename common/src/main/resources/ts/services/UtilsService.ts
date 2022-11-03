import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {Form} from '../models';
import {DataUtils} from "../utils";
import {Constants} from "@common/core/constants";

export interface UtilsService {
    getInfoImage(form: Form) : Promise<any>;
    postMultipleFiles(formData: any, config: any) : Promise<any>;
    getUserPreferences() : Promise<any>;
}

export const utilsService: UtilsService = {
    async getInfoImage(form: Form) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/files/${form.picture ? form.picture.split("/").slice(-1)[0] : null}/info`));
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.utilsService.getInfoImage'));
            throw e;
        }
    },

    async postMultipleFiles(formData: any, config: any) : Promise<any> {
        try {
            return DataUtils.getData(await http.post('/formulaire/files', formData, config));
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.utilsService.postMultipleFiles'));
            throw e;
        }
    },

    async getUserPreferences() : Promise<any> {
        try {
            let data = DataUtils.getData(await http.get('/userbook/preference/language'));
            return data.preference ? JSON.parse(data.preference)[Constants.DEFAULT_DOMAIN] : null;
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.utilsService.getUserPreferences'));
            throw e;
        }
    }
};

export const UtilsService = ng.service('UtilsService', (): UtilsService => utilsService);