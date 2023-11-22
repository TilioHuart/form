import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {DataUtils} from "../utils";

export interface ResponseFileService {
    list(responseId: number) : Promise<any>;
    listByQuestion(questionId: number) : Promise<any>;
    get(fileId: number) : Promise<any>;
    download(fileId: number) : Promise<any>;
    zipAndDownload(questionId: number) : Promise<any>;
    create(responseId: number, file) : Promise<any>;
    deleteAll(responseId : number) : Promise<any>;
    deleteAllMultiple(responseIds: number[]) : Promise<void>;
}

export const responseFileService: ResponseFileService = {

    async list(responseId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/responses/${responseId}/files/all`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.list'));
            throw err;
        }
    },

    async listByQuestion(questionId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/questions/${questionId}/files/all`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.list'));
            throw err;
        }
    },

    async get(fileId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/responses/files/${fileId}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.get'));
            throw err;
        }
    },

    async download(fileId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/responses/files/${fileId}/download`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.download'));
            throw err;
        }
    },

    async zipAndDownload(questionId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/responses/${questionId}/files/download/zip`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.download'));
            throw err;
        }
    },

    async create(responseId: number, file) : Promise<any> {
        try {
            return DataUtils.getData(await http.post(`/formulaire/responses/${responseId}/files`, file, {'headers': {'Content-Type': 'multipart/form-data'}}));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.create'));
            throw err;
        }
    },

    async deleteAll(responseId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.delete(`/formulaire/responses/${responseId}/files`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.delete'));
            throw err;
        }
    },

    async deleteAllMultiple(responseIds: number[]) : Promise<void> {
        try {
            return DataUtils.getData(await http.delete(`/formulaire/responses/files/multiple`, { data: responseIds }));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.delete'));
            throw err;
        }
    }
};

export const ResponseFileService = ng.service('ResponseFileService', (): ResponseFileService => responseFileService);