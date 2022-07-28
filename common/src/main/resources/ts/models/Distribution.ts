import {Mix} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {distributionService} from "../services";

export enum DistributionStatus {
    TO_DO = 'TO_DO',
    IN_PROGRESS = 'IN_PROGRESS',
    FINISHED = 'FINISHED',
    ON_CHANGE = 'ON_CHANGE'
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
    structure: string;
    original_id: number;
    public_key: string;

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
        this.structure = null;
        this.original_id = null;
        this.public_key = null;
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
            active: this.active,
            structure: this.structure,
            original_id: this.original_id,
            public_key: this.public_key
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
            let data = await distributionService.list();
            this.all = Mix.castArrayAs(Distribution, data);
            for (let i = 0; i < this.all.length; i++) {
                let distrib = this.all[i];
                distrib.date_response = new Date(distrib.date_response);
                distrib.date_sending = new Date(distrib.date_sending);
            }
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.distribution.sync'));
            throw e;
        }
    }

    syncByFormAndResponder = async (formId: number) : Promise<void> => {
        try {
            let data = await distributionService.listByFormAndResponder(formId);
            this.all = Mix.castArrayAs(Distribution, data);
            for (let i = this.all.length - 1; i >= this.all.length - data.length; i--) {
                let distrib = this.all[i];
                distrib.date_response = new Date(distrib.date_response);
                distrib.date_sending = new Date(distrib.date_sending);
            }
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.distribution.sync'));
            throw e;
        }
    }

    syncByFormAndStatus = async (formId: number, status: string, questionId: number = null, nbLines: number = null) : Promise<void> => {
        try {
            let data;
            if (questionId) {
                data = await distributionService.listByFormAndStatusAndQuestion(formId, status, questionId, nbLines);
            }
            else {
                data = await distributionService.listByFormAndStatus(formId, status, nbLines);
            }
            this.all = nbLines && nbLines > 0 ? this.all.concat(Mix.castArrayAs(Distribution, data)) : Mix.castArrayAs(Distribution, data);
            for (let i = this.all.length - 1; i >= this.all.length - data.length; i--) {
                let distrib = this.all[i];
                distrib.date_response = new Date(distrib.date_response);
                distrib.date_sending = new Date(distrib.date_sending);
            }
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.distribution.sync'));
            throw e;
        }
    }
}