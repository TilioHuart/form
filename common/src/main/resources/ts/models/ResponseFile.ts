import {idiom, notify} from "entcore";
import {Mix} from "entcore-toolkit";
import {responseFileService} from "../services";

export class ResponseFile {
    id: string;
    response_id: number;
    filename: string;
    type: string;

    constructor() {
        this.id = null;
        this.response_id = null;
        this.filename = null;
        this.type =  null;
    }

    toJson() : Object {
        return {
            id: this.id,
            response_id: this.response_id,
            filename: this.filename,
            type: this.type
        }
    }

    build(data: IResponseFileResponse) : ResponseFile {
        this.id = data.id ? data.id : null;
        this.response_id = data.responseId ? data.responseId : null;
        this.filename = data.filename ? data.filename : null;
        this.type = data.type ? data.type : null;
        return this;
    }
}

export class ResponseFiles {
    all: ResponseFile[];

    constructor() {
        this.all = [];
    }

    sync = async (responseId: number) : Promise<void> => {
        try {
            let data = await responseFileService.list(responseId);
            this.all = Mix.castArrayAs(ResponseFile, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.responseFile.sync'));
            throw e;
        }
    }
}

export interface IResponseFileResponse {
    id: string;
    responseId: number;
    filename: string;
    type: string;
}