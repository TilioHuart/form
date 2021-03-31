import {Mix} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {distributionService} from "../services";

export enum DistributionStatus {
    TO_DO = 'TO_DO',
    IN_PROGRESS = 'IN_PROGRESS',
    FINISHED = 'FINISHED'
}

export class Distribution  {

    id: number;
    form_id: number;
    sender_id: number;
    sender_name: string;
    responder_id: number;
    responder_name: string;
    status: string;
    date_sending: Date;
    date_response: Date;
    active: boolean;

    constructor() {
        this.id = null;
        this.form_id = null;
        this.sender_id = null;
        this.sender_name = null;
        this.responder_id = null;
        this.responder_name = null;
        this.status = null;
        this.date_sending = null;
        this.date_response = null;
        this.active = true;
    }

    toJson() : Object {
        return {
            id: this.id,
            form_id: this.form_id,
            sender_id: this.sender_id,
            sender_name: this.sender_name,
            responder_id: this.responder_id,
            responder_name: this.responder_name,
            status: this.status,
            date_sending: this.date_sending,
            date_response: this.date_response,
            active: this.active
        }
    }
}

export class Distributions {
    all: Distribution[];

    constructor() {
        this.all = [];
    }

    sync = async () : Promise<void> => {
        try {
            let { data } = await distributionService.list();
            this.all = Mix.castArrayAs(Distribution, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.distribution.sync'));
            throw e;
        }
    }
}