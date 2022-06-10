import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {DataUtils} from "@common/utils";
import {Response, Responses} from "@common/models";

export interface ResponseService {
    sendResponses(formKey: string, distributionKey: string, responseCaptcha: Response, responses: Responses) : Promise<any>;
}

export const responseService: ResponseService = {
    async sendResponses(formKey: string, distributionKey: string, responseCaptcha: Response, responses: Responses) : Promise<any> {
        try {
            let data = {
                captcha: responseCaptcha,
                responses: responses.all
            };
            return DataUtils.getData(await http.post(`/formulaire-public/responses/${formKey}/${distributionKey}`, data));
        } catch (err) {
            notify.error(idiom.translate('formulaire.public.error.responseService.create'));
            throw err;
        }
    }
};

export const ResponseService = ng.service('ResponseService', (): ResponseService => responseService);