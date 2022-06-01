import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {Form} from '../models';
import {DataUtils} from "../utils";

export interface UtilsService {
    getInfoImage(form: Form) : Promise<any>;
    postMultipleFiles(formData: any, config: any) : Promise<any>;
}

export const utilsService: UtilsService = {
    async getInfoImage(form: Form) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/files/${form.picture ? form.picture.split("/").slice(-1)[0] : null}/info`));
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.utilsService.get'));
            throw e;
        }
    },

    async postMultipleFiles(formData: any, config: any) : Promise<any> {
        try {
            return DataUtils.getData(await http.post('/formulaire/files', formData, config));
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.utilsService.create'));
            throw e;
        }
    }
};

export const UtilsService = ng.service('UtilsService', (): UtilsService => utilsService);