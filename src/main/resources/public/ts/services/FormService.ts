import {idiom, ng, toasts} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {Form} from '../models/Form';

export interface FormService {
    create(Form): Promise<AxiosResponse>;
    get(number): Promise<AxiosResponse>;
}

export const formService: FormService = {

    async create(form : Form): Promise<AxiosResponse> {
        try {
            return await http.post('/forms', form);
        } catch (err) {
            toasts.warning(idiom.translate('student.error.sendForm'));
            throw err;
        }
    },

    async get(id : number): Promise<AxiosResponse> {
        try {
            return await http.get(`/forms/${id}`);
        } catch (err) {
            toasts.warning(idiom.translate('student.error.getServices'));
            throw err;
        }
    }
};

export const FormService = ng.service('CallbackService', (): FormService => formService);