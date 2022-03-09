import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {DataUtils} from "../../utils";
import {Section} from "../../models";
import {Mix} from "entcore-toolkit";

export interface SectionService {
    list(formId: number) : Promise<any>;
    get(sectionId: number) : Promise<any>;
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
            let section: Section = DataUtils.getData(await http.get(`/formulaire/sections/${sectionId}`));
            await section.questions.sync(section.id, true);
            return section;
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
            let sectionUpdated = Mix.castAs(Section, DataUtils.getData(await http.put(`/formulaire/sections/${section.id}`, section)));
            await sectionUpdated.questions.sync(sectionUpdated.id, true);
            return sectionUpdated;
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