import {UtilsUtils} from "@common/utils";
import {Form} from "@common/models/Form";
import {Constants} from "@common/core/constants";

export class Files {
    all: File[]

    constructor () {
        this.all = [];
    }
}

export class FilePayload {
    formData: FormData;
    responseId: number;

    constructor (file: File, responseId: number, form: Form) {
        this.formData = this.formatForSaving(form, file);
        this.responseId = responseId;
    }

    formatForSaving = (form: Form, file: File) : FormData => {
        let filename = file.name;
        if (file.type && !form.anonymous) {
            filename = UtilsUtils.getOwnerNameWithUnderscore() + filename;
        }
        let formData = new FormData();
        formData.append(Constants.FILE, file, filename);
        return formData;
    }
}