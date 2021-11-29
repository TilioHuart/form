import {Mix} from "entcore-toolkit";
import {idiom, notify} from "entcore";
import {delegateService} from "../services";

export class Delegate {
    id: number;
    entity: string;
    mail: string;
    address: string;
    zipcode: string;
    city: string;

    constructor () {
        this.id = null;
        this.entity = null;
        this.mail = null;
        this.address = null;
        this.zipcode = null;
        this.city = null;
    }

    toJson() : Object {
        return {
            id: this.id,
            entity: this.entity,
            mail: this.mail,
            address: this.address,
            zipcode: this.zipcode,
            city: this.city
        }
    }
}

export class Delegates {
    all: Delegate[];

    constructor() {
        this.all = [];
    }

    sync = async () : Promise<void> => {
        try {
            let { data } = await delegateService.list();
            this.all = Mix.castArrayAs(Delegate, data);
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.delegateService.sync'));
            throw e;
        }
    }
}