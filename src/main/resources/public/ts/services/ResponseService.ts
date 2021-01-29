import {idiom, ng, notify} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {Question, Response} from "../models";

export interface ResponseService {
    list(questionId : number): Promise<AxiosResponse>;
    get(questionId : number): Promise<AxiosResponse>;
    save(response : Response): Promise<AxiosResponse>;
    create(response : Response): Promise<AxiosResponse>;
    update(response : Response): Promise<AxiosResponse>;
    delete(responseId : number): Promise<AxiosResponse>;
}

export const responseService: ResponseService = {

    async list (questionId : number): Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/questions/${questionId}/responses`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.list'));
            throw err;
        }
    },

    async get(questionId : number): Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/questions/${questionId}/response`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.get'));
            throw err;
        }
    },

    async save(response : Response): Promise<AxiosResponse> {
        return response.id ? await this.update(response) : await this.create(response);
    },

    async create(response : Response): Promise<AxiosResponse> {
        try {
            return http.post(`/formulaire/questions/${response.question_id}/responses`, response);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.create'));
            throw err;
        }
    },

    async update(response : Response): Promise<AxiosResponse> {
        try {
            return http.put(`/formulaire/responses/${response.id}`, response);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.update'));
            throw err;
        }
    },

    async delete(responseId : number): Promise<AxiosResponse> {
        try {
            return http.delete(`/formulaire/responses/${responseId}`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.delete'));
            throw err;
        }
    }
};

export const ResponseService = ng.service('ResponseService', (): ResponseService => responseService);