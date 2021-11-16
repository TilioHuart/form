import {idiom, ng, notify} from 'entcore';
import http, {AxiosResponse} from 'axios';

export interface DelegateService {
    list() : Promise<AxiosResponse>;
}

export const delegateService: DelegateService = {

    async list() : Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/delegates`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.delegateService.list'));
            throw err;
        }
    }
};

export const DelegateService = ng.service('DelegateService',(): DelegateService => delegateService);