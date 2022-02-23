import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {DataUtils} from "../utils/data";
import {Section} from "../models/Section";

export interface SectionService {
    list(formId: number) : Promise<any>;
    get(choiceId: number) : Promise<any>;
    save(section: Section) : Promise<any>;
    create(section: Section) : Promise<any>;
    update(section: Section) : Promise<any>;
    delete(sectionId: number) : Promise<any>;
}

export const sectionService: SectionService = {

    async list(formId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/forms/${formId}/sections`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.sectionService.list'));
            throw err;
        }
    },

    async get(sectionId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/sections/${sectionId}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.sectionService.get'));
            throw err;
        }
    },

    async save(section: Section) : Promise<any> {
        return section.id ? await this.update(section) : await this.create(section);
    },

    async create(section: Section) : Promise<any> {
        try {
            return DataUtils.getData(await http.post(`/formulaire/forms/${section.form_id}/sections`, section));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.sectionService.create'));
            throw err;
        }
    },

    async update(section: Section) : Promise<any> {
        try {
            return DataUtils.getData(await http.put(`/formulaire/forms/${section.form_id}/sections`, section));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.sectionService.update'));
            throw err;
        }
    },

    async delete(sectionId: number) : Promise<any> {
        try {
            return DataUtils.getData(await http.delete(`/formulaire/sections/${sectionId}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.sectionService.delete'));
            throw err;
        }
    }
};

export const SectionService = ng.service('SectionService',(): SectionService => sectionService);