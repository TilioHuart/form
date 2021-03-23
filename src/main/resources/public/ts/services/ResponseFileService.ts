import {idiom, ng, notify} from 'entcore';
import http, {AxiosResponse} from 'axios';

export interface ResponseFileService {
    list(questionId : number): Promise<AxiosResponse>;
    get(responseId : number): Promise<AxiosResponse>;
    download(responseId : number): Promise<AxiosResponse>;
    zipAndDownload(questionId : number): Promise<AxiosResponse>;
    create(responseId: number, file): Promise<AxiosResponse>;
    update(responseId: number, file): Promise<AxiosResponse>;
    delete(responseId : number, fileId : number): Promise<AxiosResponse>;
}

export const responseFileService: ResponseFileService = {

    async list (questionId: number): Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/responses/${questionId}/files/all`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionChoiceService.list'));
            throw err;
        }
    },

    async get(responseId : number): Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/responses/${responseId}/files`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.get'));
            throw err;
        }
    },

    async download(responseId : number): Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/responses/${responseId}/files/download`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.download'));
            throw err;
        }
    },

    async zipAndDownload(questionId : number): Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/responses/${questionId}/files/download/zip`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.download'));
            throw err;
        }
    },

    async create(responseId : number, file): Promise<AxiosResponse> {
        try {
            return http.post(`/formulaire/responses/${responseId}/files`, file, {'headers': {'Content-Type': 'multipart/form-data'}});
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.create'));
            throw err;
        }
    },

    async update(responseId : number, file): Promise<AxiosResponse> {
        try {
            await this.delete(responseId);
            return this.create(responseId, file);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.update'));
            throw err;
        }
    },

    async delete(responseId : number): Promise<AxiosResponse> {
        try {
            return http.delete(`/formulaire/responses/${responseId}/files`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseFileService.delete'));
            throw err;
        }
    }
};

export const ResponseFileService = ng.service('ResponseFileService', (): ResponseFileService => responseFileService);