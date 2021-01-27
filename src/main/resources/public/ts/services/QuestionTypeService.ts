import {idiom, ng, notify} from 'entcore';
import http, {AxiosResponse} from 'axios';

export interface QuestionTypeService {
    list(): Promise<AxiosResponse>;
    get(code): Promise<AxiosResponse>;
}

export const questionTypeService: QuestionTypeService = {

    async list (): Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/types`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionTypeService.list'));
            throw err;
        }
    },

    async get(code : number): Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/types/${code}`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionTypeService.get'));
            throw err;
        }
    },
};

export const QuestionTypeService = ng.service('QuestionTypeService', (): QuestionTypeService => questionTypeService);