import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {DataUtils} from "../../utils/data";
import {Question} from "../../models";

export interface QuestionService {
    list(formId: number) : Promise<any>;
    // list(id: number, isForSection?: boolean) : Promise<any>;
    countQuestions(formId: number) : Promise<any>;
    get(questionId: number) : Promise<any>;
    getByPosition(idForm: number, position: number) : Promise<any>;
    save(question: Question) : Promise<any>;
    create(question: Question) : Promise<any>;
    createMultiple(questions: Question[], formId: number) : Promise<any>;
    update(question: Question) : Promise<any>;
    delete(questionId: number) : Promise<any>;
}

export const questionService: QuestionService = {

    async list (formId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/forms/${formId}/questions`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionService.list'));
            throw err;
        }
    },

    // async list (id: number, isForSection: boolean = false) : Promise<any> {
    //     try {
    //         let parentEntity = isForSection ? 'sections' : 'forms';
    //         return DataUtils.getData(await http.get(`/formulaire/${parentEntity}/${id}/questions`));
    //     } catch (err) {
    //         notify.error(idiom.translate('formulaire.error.questionService.list'));
    //         throw err;
    //     }
    // },

    async countQuestions (formId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/forms/${formId}/questions/count`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionService.count'));
            throw err;
        }
    },

    async get(questionId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/questions/${questionId}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionService.get'));
            throw err;
        }
    },

    async getByPosition(idForm : number, position: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/forms/${idForm}/questions/${position}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionService.get'));
            throw err;
        }
    },

    async save(question: Question) : Promise<any> {
        return question.id ? await this.update(question) : await this.create(question);
    },

    async create(question: Question) : Promise<any> {
        try {
            return DataUtils.getData(await http.post(`/formulaire/forms/${question.form_id}/questions`, question));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionService.create'));
            throw err;
        }
    },

    async createMultiple(questions: Question[], formId : number) : Promise<any> {
        try {
            return DataUtils.getData(await http.post(`/formulaire/forms/${formId}/questions/multiple`, questions));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionService.createMultiple'));
            throw err;
        }
    },

    async update(question: Question) : Promise<any> {
        try {
            return DataUtils.getData(await http.put(`/formulaire/questions/${question.id}`, question));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionService.update'));
            throw err;
        }
    },

    async delete(questionId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.delete(`/formulaire/questions/${questionId}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionService.delete'));
            throw err;
        }
    }
};

export const QuestionService = ng.service('QuestionService', (): QuestionService => questionService);