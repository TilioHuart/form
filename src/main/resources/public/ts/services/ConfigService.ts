import {ng, idiom, notify} from 'entcore';
import http, {AxiosResponse} from 'axios';

export interface ConfigService {
    get(): Promise<AxiosResponse>;
}

export const configService: ConfigService = {

    async get(): Promise<AxiosResponse> {
        try {
            return await http.get('/formulaire/config');
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.configService.get'));
        }
    }
};

export const ConfigService = ng.service('ConfigService', (): ConfigService => configService);