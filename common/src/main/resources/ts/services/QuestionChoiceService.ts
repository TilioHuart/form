import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {QuestionChoice} from '../models';
import {DataUtils} from "../utils";

export interface QuestionChoiceService {
    list(questionId: number) : Promise<any>;
    listChoices(questionIds) : Promise<any>;
    save(choice: QuestionChoice) : Promise<any>;
    create(choice: QuestionChoice) : Promise<any>;
    update(choice: QuestionChoice) : Promise<any>;
    delete(choiceId: number) : Promise<any>;
}

export const questionChoiceService: QuestionChoiceService = {

    async list(questionId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/questions/${questionId}/choices`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionChoiceService.list'));
            throw err;
        }
    },

    async listChoices(questionIds) : Promise<any>{
        try{
            return DataUtils.getData(await http.get(`/formulaire/questions/choices/all`, { params: questionIds }));
        } catch(err){
            notify.error(idiom.translate('formulaire.error.questionChoiceService.list'));
            throw err;
        }
    },

    async save(choice: QuestionChoice) : Promise<any> {
        return choice.id ? await this.update(choice) : await this.create(choice);
    },

    async create(choice: QuestionChoice) : Promise<any> {
        try {
            return DataUtils.getData(await http.post(`/formulaire/questions/${choice.question_id}/choices`, choice));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionChoiceService.create'));
            throw err;
        }
    },

    async update(choice: QuestionChoice) : Promise<any> {
        try {
            return DataUtils.getData(await http.put(`/formulaire/choices/${choice.id}`, choice));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionChoiceService.update'));
            throw err;
        }
    },

    async delete(choiceId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.delete(`/formulaire/choices/${choiceId}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionChoiceService.delete'));
            throw err;
        }
    }
};

export const QuestionChoiceService = ng.service('QuestionChoiceService',(): QuestionChoiceService => questionChoiceService);