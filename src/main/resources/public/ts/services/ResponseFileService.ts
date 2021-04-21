import {idiom, ng, notify} from 'entcore';
import http, {AxiosResponse} from 'axios';

export interface ResponseFileService {
    list(responseId: number) : Promise<AxiosResponse>;
    get(responseId: number) : Promise<AxiosResponse>;
    download(fileId: number) : Promise<AxiosResponse>;
    zipAndDownload(questionId: number) : Promise<AxiosResponse>;
    create(responseId: number, file) : Promise<AxiosResponse>;
    deleteAll(responseId : number) : Promise<AxiosResponse>;
}

export const responseFileService: ResponseFileService = {

    async list(responseId: number) : Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/responses/${responseId}/files/all`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.list'));
            throw err;
        }
    },

    async get(responseId: number) : Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/responses/${responseId}/files`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.get'));
            throw err;
        }
    },

    async download(fileId: number) : Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/responses/files/${fileId}/download`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.download'));
            throw err;
        }
    },

    async zipAndDownload(questionId: number) : Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/responses/${questionId}/files/download/zip`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.download'));
            throw err;
        }
    },

    async create(responseId: number, file) : Promise<AxiosResponse> {
        try {
            return http.post(`/formulaire/responses/${responseId}/files`, file, {'headers': {'Content-Type': 'multipart/form-data'}});
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.create'));
            throw err;
        }
    },

    async deleteAll(responseId: number) : Promise<AxiosResponse> {
        try {
            return http.delete(`/formulaire/responses/${responseId}/files`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.delete'));
            throw err;
        }
    }
};

export const ResponseFileService = ng.service('ResponseFileService', (): ResponseFileService => responseFileService);