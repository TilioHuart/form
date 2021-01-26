import {idiom, ng, notify} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {Distribution, Form} from '../models';

export interface DistributionService {
    list(): Promise<AxiosResponse>;
    get(number): Promise<AxiosResponse>;
    create(Form, Distribution): Promise<AxiosResponse>;
    update(Distribution): Promise<AxiosResponse>;
    delete(number): Promise<AxiosResponse>;
}

export const distributionService: DistributionService = {

    async list (): Promise<AxiosResponse> {
        try {
            return await http.get('/formulaire/distributions');
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.list'));
            throw err;
        }
    },

    async get(id : number): Promise<AxiosResponse> {
        try {
            return await http.get(`/formulaire/distributions/${id}`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.get'));
            throw err;
        }
    },

    async create(form: Form, dist : Distribution): Promise<AxiosResponse> {
        try {
            return await http.post(`/formulaire/forms/${form.id}/distributions`, dist);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.create'));
            throw err;
        }
    },

    async update(dist : Distribution): Promise<AxiosResponse> {
        try {
            return await http.put(`/formulaire/distributions/${dist.id}`, dist);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.update'));
            throw err;
        }
    },

    async delete(id : number): Promise<AxiosResponse> {
        try {
            return await http.delete(`/formulaire/distributions/${id}`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.delete'));
            throw err;
        }
    }
};

export const DistributionService = ng.service('DistributionService',
    (): DistributionService => distributionService);