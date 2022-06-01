import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {DataUtils} from "@common/utils";
import {Responses} from "@common/models";

export interface PublicService {
    getPublicFormByKey(formKey: string) : Promise<any>;
    sendResponses(formKey: string, distributionKey: string, responses: Responses) : Promise<any>;
}

export const publicService: PublicService = {
    async getPublicFormByKey(formKey: string) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire-public/forms/key/${formKey}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.get'));
            throw err;
        }
    },

    async sendResponses(formKey: string, distributionKey: string, responses: Responses) : Promise<any> {
        try {
            return DataUtils.getData(await http.post(`/formulaire-public/responses/${formKey}/${distributionKey}`, responses.all));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.responseService.create'));
            throw err;
        }
    }
};

export const PublicService = ng.service('PublicService', (): PublicService => publicService);