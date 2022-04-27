import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {DataUtils} from "../utils";
import {Res} from "awesome-typescript-loader/dist/checker/protocol";
import {Responses} from "../models";

export interface POCService {
    getPublicFormByKey(formKey: string) : Promise<any>;
    sendResponses(formKey: string, distributionKey: string, responses: Responses) : Promise<any>;
}

export const pocService: POCService = {
    async getPublicFormByKey(formKey: string) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/forms/key/${formKey}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.get'));
            throw err;
        }
    },

    async sendResponses(formKey: string, distributionKey: string, responses: Responses) : Promise<any> {
        try {
            return DataUtils.getData(await http.post(`/formulaire/responses/${formKey}/${distributionKey}`, responses.all));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.create'));
            throw err;
        }
    }
};

export const POCService = ng.service('POCService', (): POCService => pocService);