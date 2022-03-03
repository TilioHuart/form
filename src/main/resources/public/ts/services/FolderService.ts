import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {Folder} from '../models';
import {DataUtils} from "../utils";

export interface FolderService {
    list() : Promise<any>;
    get(folderId: number) : Promise<any>;
    save(folder: Folder) : Promise<any>;
    create(folder: Folder) : Promise<any>;
    update(folder: Folder) : Promise<any>;
    delete(folders: Folder[]) : Promise<any>;
    move(folders: Folder[], parentId: number) : Promise<any>;
}

export const folderService: FolderService = {

    async list() : Promise<any> {
        try {
            return DataUtils.getData(await http.get('/formulaire/folders'));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.folderService.list'));
            throw err;
        }
    },

    async get(folderId) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire/folders/${folderId}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.folderService.get'));
            throw err;
        }
    },

    async save(folder) : Promise<any> {
        return folder.id ? await this.update(folder) : await this.create(folder);
    },

    async create(folder) : Promise<any> {
        try {
            return DataUtils.getData(await http.post('/formulaire/folders', folder));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.folderService.create'));
            throw err;
        }
    },

    async update(folder) : Promise<any> {
        try {
            return DataUtils.getData(await http.put(`/formulaire/folders/${folder.id}`, folder));
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.folderService.update'));
            throw err;
        }
    },

    async delete(folders) : Promise<any> {
        try {
            return DataUtils.getData(await http.delete('/formulaire/folders', { data: folders } ));
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.folderService.delete'));
            throw e;
        }
    },

    async move(folders, parentId) : Promise<any> {
        try {
            return DataUtils.getData(await http.put(`/formulaire/folders/move/${parentId}`, folders));
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.folderService.move'));
            throw e;
        }
    }
};

export const FolderService = ng.service('FolderService', (): FolderService => folderService);