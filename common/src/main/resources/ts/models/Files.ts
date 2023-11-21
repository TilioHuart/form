import {UtilsUtils} from "@common/utils";
import {Form} from "@common/models/Form";
import {Constants} from "@common/core/constants";

export class Files {
    all: File[]

    constructor () {
        this.all = [];
    }

    formatForSaving = (form: Form) : void => {
        for (let i = 0; i < this.all.length; i++) {
            let filename = this.all[i].name;
            if (this.all[i].type && !form.anonymous) {
                filename = UtilsUtils.getOwnerNameWithUnderscore() + filename;
            }
            let file = new FormData();
            file.append(Constants.FILE, this.all[i], filename);
        }
    }
}

export class FilePayload {
    file: File;
    responseId: number;

    constructor (file: File, responseId: number) {
        this.file = file;
        this.responseId = responseId;
    }
}