import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {
    FormElement,
    IResponseResponse,
    Question,
    Response,
    Responses,
    Section
} from "../models";
import {DataUtils} from "../utils";
import {Exports} from "@common/core/enums";
export interface ResponseService {
    list(question: Question, nbLines: number) : Promise<any>;
    listByForm(formId: number) : Promise<any>;
    listMineByDistribution(questionId: number, distributionId: number) : Promise<any>;
    listMineByDistributionAndQuestions(questionIds: number[], distributionId: number) : Promise<Response[]>;
    listByDistribution(distributionId: number) : Promise<any>;
    countByFormElement(formElement: FormElement) : Promise<any>;
    save(response: Response, questionType: number) : Promise<any>;
    saveMultiple(mapQuestionResponsesToSave: Map<Question, Response[]>, distributionId: number) : Promise<Response[]>;
    create(response: Response) : Promise<any>;
    createMultiple(distributionId: number, responses: Response[]) : Promise<Response[]>;
    update(response: Response) : Promise<any>;
    updateMultiple(distributionId: number, responses: Response[]) : Promise<Response[]>;
    delete(formId: number, responses: Response[]) : Promise<any>;
    deleteByQuestionAndDistribution(questionId: number, distributionId: number) : Promise<any>;
    deleteMultipleByQuestionAndDistribution(questionIds: number[], distributionId: number) : Promise<any>;
    export(formId: number, type: string, images?: any) : Promise<any>;
}

export const responseService: ResponseService = {
    async list(question: Question, nbLines: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/questions/${question.id}/responses?nbLines=${nbLines}&formId=${question.form_id}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.list'));

            throw err;
        }
    },

    async listByForm(formId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/forms/${formId}/responses`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.list'));
            throw err;
        }
    },

    async listMineByDistribution(questionId: number, distributionId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/questions/${questionId}/distributions/${distributionId}/responses`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.list'));
            throw err;
        }
    },

    async listMineByDistributionAndQuestions(questionIds: number[], distributionId: number) : Promise<Response[]> {
        try {
            let data: IResponseResponse[] = DataUtils.getData(await http.get(`/formulaire/distributions/${distributionId}/responses/multiple`, { params: questionIds }));
            return data.map((rr: IResponseResponse) => new Response().build(rr));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.list'));
            throw err;
        }
    },

    async listByDistribution(distributionId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/distributions/${distributionId}/responses`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.list'));
            throw err;
        }
    },

    async countByFormElement (formElement: FormElement) : Promise<any> {
        try {
            let questionIds = [];
            if (formElement instanceof Section) {
                for (let question of formElement.questions.all) {
                    questionIds.push(question.id);
                }
            }
            else if (formElement instanceof Question) {
                questionIds.push(formElement.id);
            }
            return DataUtils.getData(await http.get(`/formulaire/responses/count`, { params: questionIds }));
        } catch(err) {
            notify.error(idiom.translate('formulaire.error.responseService.get'));
            throw err;
        }
    },

    async save(response: Response, questionType: number) : Promise<any> {
        response.formatBeforeSaving(questionType);
        return response.id ? await this.update(response) : await this.create(response);
    },

    async saveMultiple(mapQuestionResponsesToSave: Map<Question, Response[]>, distributionId: number) : Promise<Response[]> {
        if (!mapQuestionResponsesToSave || mapQuestionResponsesToSave.size <= 0) {
            return [];
        }

        let  responsesToUpdate: Response[] = [];
        let responsesToCreate: Response[] = [];

        mapQuestionResponsesToSave.forEach((responses: Response[], question: Question) => {
            for (let response of responses) {
                response.formatBeforeSaving(question.question_type);
                response.id ? responsesToUpdate.push(response) : responsesToCreate.push(response);
            }
        });

        let promises: Promise<Response[]>[] = [this.updateMultiple(distributionId, responsesToUpdate), this.createMultiple(distributionId, responsesToCreate)];
        try {
            let results: Response[][] = await Promise.all(promises);
            return results[0].concat(results[1]);
        } catch (err) {
            notify.error(idiom.translate( 'formulaire.error.questionService.save'));
            throw err;
        }
    },

    async create(response: Response) : Promise<any> {
        try {
            return DataUtils.getData(await http.post(`/formulaire/questions/${response.question_id}/responses`, response));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.create'));
            throw err;
        }
    },

    async createMultiple(distributionId: number, responses: Response[]) : Promise<Response[]> {
        try {
            if (!responses || responses.length <= 0) {
                return [];
            }
            let data: IResponseResponse[] = DataUtils.getData(await http.post(`/formulaire/distributions/${distributionId}/responses/multiple`, responses));
            return data.map((rr: IResponseResponse) => new Response().build(rr));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.create'));
            throw err;
        }
    },

    async update(response: Response) : Promise<any> {
        try {
            return DataUtils.getData(await http.put(`/formulaire/responses/${response.id}`, response));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.update'));
            throw err;
        }
    },

    async updateMultiple(distributionId: number, responses: Response[]) : Promise<Response[]> {
        try {
            if (!responses || responses.length <= 0) {
                return [];
            }
            let data: IResponseResponse[] = DataUtils.getData(await http.put(`/formulaire/distributions/${distributionId}/responses`, responses));
            return data.map((rr: IResponseResponse) => new Response().build(rr));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.update'));
            throw err;
        }
    },

    async delete(formId, responses) : Promise<any> {
        try {
            return DataUtils.getData(await http.delete(`/formulaire/responses/${formId}`, { data: responses } ));
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.responseService.delete'));
            throw e;
        }
    },

    async deleteByQuestionAndDistribution(questionId: number, distributionId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.delete(`/formulaire/responses/${distributionId}/questions/${questionId}`));
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.responseService.delete'));
            throw e;
        }
    },

    async deleteMultipleByQuestionAndDistribution(questionIds: number[], distributionId: number) : Promise<any> {
        try {
            if (!questionIds || questionIds.length <= 0) {
                return [];
            }
            return DataUtils.getData(await http.delete(`/formulaire/responses/${distributionId}/questions`, { data: questionIds }));
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.responseService.delete'));
            throw e;
        }
    },

    async export(formId: number, type: string, images: any) : Promise<any> {
        try {
            if (type === Exports.CSV) {
                return await http.post(`/formulaire/responses/export/${formId}/csv`, {});
            }
            else if (type === Exports.PDF) {
                return await http.post(`/formulaire/responses/export/${formId}/pdf`, images, { responseType: "arraybuffer" });
            }
            else {
                notify.error(idiom.translate('formulaire.error.responseService.export'));
            }
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.export'));
            throw err;
        }
    }
};

export const ResponseService = ng.service('ResponseService', (): ResponseService => responseService);