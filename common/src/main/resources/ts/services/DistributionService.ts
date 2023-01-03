import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {Distribution} from '../models';
import {DataUtils} from "../utils";
import {Mix} from "entcore-toolkit";

export interface DistributionService {
    list() : Promise<any>;
    listByResponder() : Promise<any>;
    listByForm(formId: number) : Promise<any>;
    listByFormAndStatus(formId: number, status: string, nbLines: number) : Promise<any>;
    listByFormAndStatusAndQuestion(formId: number, status: string, questionId: number, nbLines: number) : Promise<any>
    listByFormAndResponder(formId: number) : Promise<Distribution[]>;
    count(formId: number) : Promise<any>;
    get(distributionId: number) : Promise<Distribution>;
    getByFormResponderAndStatus(formId: number) : Promise<any>;
    add(distributionId: number) : Promise<any>;
    duplicateWithResponses(distributionId: number) : Promise<any>;
    update(distribution: Distribution) : Promise<any>;
    replace(distribution: Distribution) : Promise<any>;
    delete(distributionId: number) : Promise<any>;
}

export const distributionService: DistributionService = {
    async list() : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/distributions`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.list'));
            throw err;
        }
    },

    async listByResponder() : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/distributions/listMine`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.list'));
            throw err;
        }
    },

    async listByForm(formId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/distributions/forms/${formId}/list`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.list'));
            throw err;
        }
    },

    async listByFormAndStatus(formId: number, status: string, nbLines: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/distributions/forms/${formId}/list/${status}?nbLines=${nbLines}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.list'));
            throw err;
        }
    },

    async listByFormAndStatusAndQuestion(formId: number, status: string, questionId: number, nbLines: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/distributions/forms/${formId}/questions/${questionId}/list/${status}?nbLines=${nbLines}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.list'));
            throw err;
        }
    },

    async listByFormAndResponder(formId: number) : Promise<Distribution[]> {
        try {
            let data: any = DataUtils.getData(await http.get(`/formulaire/distributions/forms/${formId}/listMine`));
            return Mix.castArrayAs(Distribution, data);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.list'));
            throw err;
        }
    },

    async count(formId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/distributions/forms/${formId}/count`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.count'));
            throw err;
        }
    },

    async get(distributionId: number) : Promise<Distribution> {
        try {
            let data: any = DataUtils.getData(await http.get(`/formulaire/distributions/${distributionId}`));
            return Mix.castAs(Distribution, data);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.get'));
            throw err;
        }
    },

    async getByFormResponderAndStatus(formId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/distributions/forms/${formId}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.get'));
            throw err;
        }
    },

    async add(distributionId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.post(`/formulaire/distributions/${distributionId}/add`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.create'));
            throw err;
        }
    },

    async duplicateWithResponses(distributionId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.post(`/formulaire/distributions/${distributionId}/duplicate`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.create'));
            throw err;
        }
    },

    async update(distribution: Distribution) : Promise<any> {
        try {
            return DataUtils.getData(await http.put(`/formulaire/distributions/${distribution.id}`, distribution));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.update'));
            throw err;
        }
    },

    async replace(distribution: Distribution) : Promise<any> {
        try {
            return DataUtils.getData(await http.delete(`/formulaire/distributions/${distribution.id}/replace/${distribution.original_id}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.update'));
            throw err;
        }
    },

    async delete(distributionId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.delete(`/formulaire/distributions/${distributionId}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.distributionService.delete'));
            throw err;
        }
    }
};

export const DistributionService = ng.service('DistributionService',(): DistributionService => distributionService);