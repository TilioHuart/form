import {idiom, ng, notify} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {Folder} from '../models/Folder';

export interface FolderService {
    list() : Promise<AxiosResponse>;
    get(folderId: number) : Promise<AxiosResponse>;
    save(folder: Folder) : Promise<AxiosResponse>;
    create(folder: Folder) : Promise<AxiosResponse>;
    update(folder: Folder) : Promise<AxiosResponse>;
    delete(folders: Folder[]) : Promise<AxiosResponse>;
    move(folders: Folder[], parentId: number) : Promise<AxiosResponse>;
}

export const folderService: FolderService = {

    async list() : Promise<AxiosResponse> {
        try {
            return http.get('/formulaire/folders');
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.folderService.list'));
            throw err;
        }
    },

    async get(folderId) : Promise<AxiosResponse> {
        try {
            return http.get(`/formulaire/folders/${folderId}`);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.folderService.get'));
            throw err;
        }
    },

    async save(folder) : Promise<AxiosResponse> {
        return folder.id ? await this.update(folder) : await this.create(folder);
    },

    async create(folder) : Promise<AxiosResponse> {
        try {
            return http.post('/formulaire/folders', folder);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.folderService.create'));
            throw err;
        }
    },

    async update(folder) : Promise<AxiosResponse> {
        try {
            return http.put(`/formulaire/folders/${folder.id}`, folder);
        } catch (err) {
            notify.error(idiom.translate('formulaire.error.folderService.update'));
            throw err;
        }
    },

    async delete(folders) : Promise<AxiosResponse> {
        try {
            return http.delete('/formulaire/folders', { data: folders } );
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.folderService.delete'));
            throw e;
        }
    },

    async move(folders, parentId) : Promise<AxiosResponse> {
        try {
            return http.put(`/formulaire/folders/move/${parentId}`, folders);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.folderService.move'));
            throw e;
        }
    }
};

export const FolderService = ng.service('FolderService', (): FolderService => folderService);