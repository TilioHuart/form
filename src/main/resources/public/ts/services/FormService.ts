import {idiom, ng, notify} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {Form} from '../models/Form';

export interface FormService {
    list(): Promise<AxiosResponse>;
    create(Form): Promise<AxiosResponse>;
    get(number): Promise<AxiosResponse>;
}

export const formService: FormService = {

    async list (): Promise<AxiosResponse> {
        try {
            return await http.get('/formulaire/forms');
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.list'));
            throw err;
        }
    },

    async create(form : Form): Promise<AxiosResponse> {
        try {
            return await http.post('/formulaire/forms', form);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.create'));
            throw err;
        }
    },

    async get(id : number): Promise<AxiosResponse> {
        try {
            return await http.get(`/formulaire/forms/${id}`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.formService.get'));
            throw err;
        }
    }
};

export const FormService = ng.service('FormService', (): FormService => formService);