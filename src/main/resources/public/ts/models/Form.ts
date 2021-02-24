import {Mix, Selectable, Selection} from "entcore-toolkit";
import {idiom, notify, Rights, Shareable} from "entcore";
import {distributionService, formService} from "../services";
import {DistributionStatus} from "./Distribution";

export class Form implements Selectable, Shareable  {
    shared: any;
    owner: { userId: string; displayName: string };
    myRights: any;
    _id: number;

    id: number;
    title: string;
    description: string;
    picture: string;
    owner_id: string;
    owner_name: string;
    date_creation: Date;
    date_modification: Date;
    date_opening: Date;
    date_ending: Date;
    sent: boolean;
    collab: boolean;
    archived: boolean;
    displayed: boolean;
    selected: boolean;
    infoImg: {
        name: string;
        type: string;
        compatible: boolean;
    };
    nbResponses: number;

    constructor() {
        this.id = null;
        this.title = null;
        this.description = null;
        this.picture = null;
        this.owner_id = null;
        this.owner_name = null;
        this.date_creation = new Date();
        this.date_modification = new Date();
        this.date_opening = new Date();
        this.date_ending = null;
        this.sent = false;
        this.collab = false;
        this.archived = false;
        this.displayed = true;
        this.selected = null;
    }

    toJson(): Object {
        return {
            id: this.id,
            title: this.title,
            description: this.description,
            picture: this.picture,
            owner_id: this.owner_id,
            owner_name: this.owner_name,
            date_creation: new Date(this.date_creation),
            date_modification: new Date(this.date_modification),
            date_opening: new Date(this.date_opening),
            date_ending: new Date(this.date_ending),
            sent: this.sent,
            collab: this.collab,
            archived: this.archived,
            selected: this.selected
        }
    }

    setFromJson(data: any) : void {
        for (let key in data) {
            this[key] = data[key];
            if (key === 'date_creation' || key === 'date_modification' || key === 'date_opening' || key === 'date_ending') {
                this[key] = new Date(this[key]);
            }
        }
    }

    generateRights() : void {
        this._id = this.id;
        this.owner = {userId: this.owner_id, displayName: this.owner_name};
        this.myRights = new Rights<Form>(this);
    }

    async setInfoImage() : Promise<void> {
        const typesImgNoSend = ["image/png", "image/jpg", "image/jpeg", "image/gif"];
        try {
            let { data: { metadata } } = await formService.getInfoImage(this);
            this.infoImg = {
                name: metadata.filename,
                type: metadata["content-type"],
                compatible: !typesImgNoSend.some(type => type === metadata["content-type"]),
            };
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.form.image'));
            throw e;
        }
    }
}

export class Forms extends Selection<Form> {
    all: Form[];

    order = {
        field: "creationDate",
        desc: false
    };

    filter = {
        shared: true,
        sent: true,
        todo: true,
        in_progress: true,
        finished: true
    }

    constructor() {
        super([]);
    }

    async sync () : Promise<void> {
        this.all = [];
        try {
            let { data } = await formService.list();
            for (let i = 0; i < data.length; i++) {
                let tempForm = new Form();
                tempForm.setFromJson(data[i]);
                tempForm.nbResponses = (await distributionService.count(tempForm.id)).data.count;
                let rightsResponse = await formService.getMyFormRights(tempForm.id);
                tempForm.myRights = rightsResponse.data;
                this.all.push(tempForm);
            }
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.form.sync'));
            throw e;
        }
    }

    async syncSent () : Promise<void> {
        try {
            let { data } = await formService.listSentForms();
            for (let i = 0; i < data.length; i++) {
                let tempForm = new Form();
                tempForm.setFromJson(data[i]);
                this.all.push(tempForm);
            }
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.form.sync'));
            throw e;
        }
    }

    orderForms() {
        this.all = this.all.sort(
            (a, b) => {
                if (this.order.field == "creationDate") {
                    if (a.date_creation > b.date_creation)
                        if (this.order.desc)
                            return 1;
                        else
                            return -1;
                    if (a.date_creation < b.date_creation)
                        if (this.order.desc)
                            return -1;
                        else
                            return 1;
                } else if (this.order.field == "modificationDate") {
                    if (a.date_modification > b.date_modification)
                        if (this.order.desc)
                            return 1;
                        else
                            return -1;
                    if (a.date_modification < b.date_modification)
                        if (this.order.desc)
                            return -1;
                        else
                            return 1;
                } else if (this.order.field == "sendingDate") {
                    if (a.date_sending > b.date_sending)
                        if (this.order.desc)
                            return 1;
                        else
                            return -1;
                    if (a.date_sending < b.date_sending)
                        if (this.order.desc)
                            return -1;
                        else
                            return 1;
                } else if (this.order.field == "creator") {
                    if (a.owner_name.toLowerCase() < b.owner_name.toLowerCase())
                        if (this.order.desc)
                            return 1;
                        else
                            return -1;
                    if (a.owner_name.toLowerCase() > b.owner_name.toLowerCase())
                        if (this.order.desc)
                            return -1;
                        else
                            return 1;
                } else if (this.order.field == "title") {
                    if (a.title.toLowerCase() < b.title.toLowerCase())
                        if (this.order.desc)
                            return 1;
                        else
                            return -1;
                    if (a.title.toLowerCase() > b.title.toLowerCase())
                        if (this.order.desc)
                            return -1;
                        else
                            return 1;
                }
            }
        );
    }

    orderByField(fieldName) {
        if (fieldName === this.order.field) {
            this.order.desc = !this.order.desc;
        } else {
            this.order.desc = false;
            this.order.field = fieldName;
        }
    };

    isOrderedAsc(field) {
        return this.order.field === field && !this.order.desc;
    }

    isOrderedDesc(field) {
        return this.order.field === field && this.order.desc;
    }

    filterForms() {
        for (let form of this.all) {
            form.displayed = true;
            if (form.collab && !this.filter.shared) {
                form.displayed = false;
            }
            if (form.sent && !this.filter.sent) {
                form.displayed = false;
            }
            if (form.status === DistributionStatus.TO_DO && !this.filter.todo) {
                form.displayed = false;
            }
            if (form.status === DistributionStatus.IN_PROGRESS && !this.filter.in_progress) {
                form.displayed = false;
            }
            if (form.status === DistributionStatus.FINISHED && !this.filter.finished) {
                form.displayed = false;
            }
        }
    }

    checkTypeFilterSelected = function (key: string) {
        this.filter[key] = !this.filter[key];
    };
}