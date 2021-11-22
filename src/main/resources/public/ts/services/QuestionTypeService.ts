import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {DataUtils} from "../utils/data";

export interface QuestionTypeService {
    list() : Promise<any>;
    get(code: number) : Promise<any>;
}

export const questionTypeService: QuestionTypeService = {

    async list() : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/types`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionTypeService.list'));
            throw err;
        }
    },

    async get(code: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/types/${code}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionTypeService.get'));
            throw err;
        }
    }
};

export const QuestionTypeService = ng.service('QuestionTypeService', (): QuestionTypeService => questionTypeService);