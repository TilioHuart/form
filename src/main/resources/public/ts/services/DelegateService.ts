import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {DataUtils} from "../utils";

export interface DelegateService {
    list() : Promise<any>;
}

export const delegateService: DelegateService = {
    async list() : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/delegates`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.delegateService.list'));
            throw err;
        }
    }
};

export const DelegateService = ng.service('DelegateService',(): DelegateService => delegateService);