import {ng} from 'entcore';
import http from 'axios';
import {DataUtils} from "@common/utils";

export interface FormService {
    getPublicFormByKey(formKey: string) : Promise<any>;
}

export const formService: FormService = {
    async getPublicFormByKey(formKey: string) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire-public/forms/key/${formKey}`));
        } catch (err) {
            throw err;
        }
    }
};

export const FormService = ng.service('FormService', (): FormService => formService);