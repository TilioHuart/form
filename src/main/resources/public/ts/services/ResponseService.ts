import {idiom, ng, notify, moment} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {Response, Types} from "../models";

export interface ResponseService {
    list(questionId : number): Promise<AxiosResponse>;
    listMine(questionId : number): Promise<AxiosResponse>;
    get(questionId : number): Promise<AxiosResponse>;
    save(response : Response, questionType : number): Promise<AxiosResponse>;
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

    async listMine (questionId : number): Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/questions/${questionId}/responses/mine`);
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

    async save(response : Response, questionType? : number): Promise<AxiosResponse> {
        if (questionType === Types.TIME) {
            response.answer = moment(response.answer).format("HH:mm");
        } else if (questionType === Types.DATE) {
            if (typeof response.answer != "string") {
                response.answer = moment(response.answer).format("DD/MM/YYYY");
            }
        }
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