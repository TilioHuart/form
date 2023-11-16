import {idiom, ng, notify, moment} from 'entcore';
import http from 'axios';
import {
    FormElement,
    IQuestionChoiceResponse,
    IResponseResponse,
    Question,
    QuestionChoice,
    Response,
    Section,
    Types
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
    save(response: Response, questionType?: number) : Promise<any>;
    create(response: Response) : Promise<any>;
    update(response: Response) : Promise<any>;
    delete(formId: number, responses: Response[]) : Promise<any>;
    deleteByQuestionAndDistribution(questionId: number, distributionId: number) : Promise<any>;
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
            let data: IResponseResponse[] = DataUtils.getData(await http.post(`/formulaire/distributions/${distributionId}/responses/multiple`, { "question_ids": questionIds }));
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

    async save(response: Response, questionType?: number) : Promise<any> {
        if (response.answer == undefined || response.answer == null) {
            response.answer = "";
        }
        else {
            if (questionType === Types.TIME && typeof response.answer != "string") {
                response.answer = moment(response.answer).format("HH:mm");
            }
            else if (questionType === Types.DATE && typeof response.answer != "string") {
                response.answer = moment(response.answer).format("DD/MM/YYYY");
            }
            else if (questionType === Types.CURSOR && typeof response.answer != "string") {
                response.answer = response.answer.toString();
            }
        }
        return response.id ? await this.update(response) : await this.create(response);
    },

    async create(response: Response) : Promise<any> {
        try {
            return DataUtils.getData(await http.post(`/formulaire/questions/${response.question_id}/responses`, response));
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

    async export(formId: number, type: string, images: any) : Promise<any> {
        try {
            if (type === Exports.CSV) {
                return await http.post(`/formulaire/responses/export/${formId}/csv`, {});
            }
            else if (type === Exports.PDF) {
                return await http.post(`/formulaire/responses/export/${formId}/pdf`, images, {responseType: "arraybuffer"});
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