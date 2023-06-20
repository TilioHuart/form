import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {DataUtils} from "../../utils";
import {Section, SectionPayload} from "../../models";
import {Mix} from "entcore-toolkit";

export interface SectionService {
    list(formId: number) : Promise<any>;
    get(sectionId: number) : Promise<any>;
    save(section: Section) : Promise<any>;
    create(section: Section) : Promise<any>;
    update(sections: Section[]) : Promise<Section[]>;
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
        return section.id ? await this.update([section]) : await this.create(section);
    },

    async create(section: Section) : Promise<any> {
        try {
            let sectionPayload: SectionPayload = new SectionPayload(section);
            return DataUtils.getData(await http.post(`/formulaire/forms/${section.form_id}/sections`, sectionPayload));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.sectionService.create'));
            throw err;
        }
    },

    async update(sections: Section[]) : Promise<Section[]> {
        try {
            if (sections.length <= 0) {
                return [];
            }
            let sectionsPayload: SectionPayload[] = sections.map((s: Section) => new SectionPayload(s));
            let data: any = DataUtils.getData(await http.put(`/formulaire/forms/${sections[0].form_id}/sections`, sectionsPayload));
            let updatedSections: Section[] = Mix.castArrayAs(Section, data);
            for (let section of updatedSections) {
                await section.questions.sync(section.id, true);
            }
            return updatedSections;
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