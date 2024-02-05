import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {DataUtils} from "../../utils";
import {IQuestionResponse, Question, QuestionPayload} from "../../models";

export interface QuestionService {
    list(id: number, isForSection?: boolean) : Promise<Question[]>;
    listAll(id: number) : Promise<Question[]>;
    listChildren(questions: Question[]) : Promise<Question[]>;
    get(questionId: number) : Promise<any>;
    save(questions: Question[]) : Promise<Question[]>;
    create(questions: Question[]) : Promise<Question[]>;
    update(questions: Question[]) : Promise<Question[]>;
    saveSingle(question: Question) : Promise<Question>;
    createSingle(question: Question) : Promise<Question>;
    updateSingle(question: Question) : Promise<Question>;
    delete(questionId: number) : Promise<any>;
}

export const questionService: QuestionService = {

    async list(id: number, isForSection: boolean = false) : Promise<Question[]> {
        try {
            let parentEntity = isForSection ? 'sections' : 'forms';
            let data: IQuestionResponse[] = DataUtils.getData(await http.get(`/formulaire/${parentEntity}/${id}/questions`, { headers: { Accept: 'application/json;version=2.0'} }));
            return data.map((qr: IQuestionResponse) => new Question().build(qr));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionService.list'));
            throw err;
        }
    },

    async listAll(formId: number) : Promise<Question[]> {
        try {
            let data: IQuestionResponse[] = DataUtils.getData(await http.get(`/formulaire/forms/${formId}/questions/all`, { headers: { Accept: 'application/json;version=2.0'} }));
            return data.map((qr: IQuestionResponse) => new Question().build(qr));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionService.list'));
            throw err;
        }
    },

    async listChildren(questions: Question[]) : Promise<Question[]> {
        try {
            if (!questions || questions.length <= 0) return [];
            let questionIds: number[] = questions.map((q: Question) => q.id);
            let data: IQuestionResponse[] = DataUtils.getData(await http.get(`/formulaire/questions/children`, { params: questionIds, headers: { Accept: 'application/json;version=2.0'} }));
            return data.map((qr: IQuestionResponse) => new Question().build(qr));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionService.list'));
            throw err;
        }
    },

    async get(questionId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/questions/${questionId}`, { headers: { Accept: 'application/json;version=2.0'} }));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionService.get'));
            throw err;
        }
    },

    async save(questions: Question[]) : Promise<Question[]> {
        let questionsToUpdate: Question[] = questions.filter((q: Question) => q.id);
        let questionsToCreate: Question[] = questions.filter((q: Question) => !q.id);
        let promises: Promise<Question[]>[] = [this.update(questionsToUpdate), this.create(questionsToCreate)];
        try {
            let results: Question[][] = await Promise.all(promises);
            return results[0].concat(results[1]);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionService.save'));
            throw err;
        }
    },

    async create(questions: Question[]) : Promise<Question[]> {
        try {
            if (!questions || questions.length <= 0) return [];
            let questionsPayload: QuestionPayload[] = questions.map((q: Question) => new QuestionPayload(q));
            let data: IQuestionResponse[] = DataUtils.getData(await http.post(`/formulaire/forms/${questionsPayload[0].form_id}/questions`, questionsPayload));
            return data.map((qr: IQuestionResponse) => new Question().build(qr));
        } catch (err) {
            let specificError: string = DataUtils.getSpecificError(err);
            notify.error(specificError ? specificError : idiom.translate('formulaire.error.questionService.create'));
            throw err;
        }
    },

    async update(questions: Question[]) : Promise<Question[]> {
        try {
            if (!questions || questions.length <= 0) return [];
            let questionsPayload: QuestionPayload[] = questions.map((q: Question) => new QuestionPayload(q));
            let data: IQuestionResponse[] = DataUtils.getData(await http.put(`/formulaire/forms/${questions[0].form_id}/questions`, questionsPayload));
            return data.map((qr: IQuestionResponse) => new Question().build(qr));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.questionService.update'));
            throw err;
        }
    },

    async saveSingle(question: Question) : Promise<Question> {
        return (question.id ? await this.update([question]) : await this.create([question]))[0];
    },

    async createSingle(question: Question) : Promise<Question> {
        return (await this.create([question]))[0];
    },

    async updateSingle(question: Question) : Promise<Question> {
        return (await this.update([question]))[0];
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