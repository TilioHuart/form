import {Mix} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {distributionService} from "../services";

export class Distribution  {

    id: number;
    form_id: number;
    sender_id: number;
    sender_name: string;
    respondent_id: number;
    respondent_name: string;
    status: string;
    date_sending: Date;
    date_response: Date;

    constructor() {
        this.id = null;
        this.form_id = null;
        this.sender_id = null;
        this.sender_name = null;
        this.respondent_id = null;
        this.respondent_name = null;
        this.status = null;
        this.date_sending = null;
        this.date_response = null;
    }

    toJson(): Object {
        return {
            id: this.id,
            form_id: this.form_id,
            sender_id: this.sender_id,
            sender_name: this.sender_name,
            respondent_id: this.respondent_id,
            respondent_name: this.respondent_name,
            status: this.status,
            date_sending: this.date_sending,
            date_response: this.date_response,
        }
    }
}

export class Distributions {
    all: Distribution[];

    constructor() {
        this.all = [];
    }

    async sync () : Promise<void> {
        try {
            let { data } = await distributionService.list();
            this.all = Mix.castArrayAs(Distribution, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.distribution.sync'));
            throw e;
        }
    }
}