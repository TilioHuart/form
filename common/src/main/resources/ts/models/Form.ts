import {Mix, Selectable, Selection} from "entcore-toolkit";
import {idiom, notify, Rights, Shareable} from "entcore";
import {formService, utilsService} from "../services";
import {Distribution, Distributions, DistributionStatus} from "./Distribution";
import {FiltersFilters, FiltersOrders} from "../core/enums";
import {FormElement, FormElements, Question, Questions, Section} from "./FormElement";
import {QuestionChoice, QuestionChoices} from "./QuestionChoice";
import {Fields} from "@common/core/constants";

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
    reminded: boolean;
    archived: boolean;
    multiple: boolean;
    anonymous: boolean;
    is_public: boolean;
    public_key: string;
    response_notified: boolean;
    editable: boolean;
    displayed: boolean;
    rgpd: boolean;
    rgpd_goal: string;
    rgpd_lifetime: number;
    folder_id: number;
    selected: boolean;
    infoImg: {
        name: string;
        type: string;
        compatible: boolean;
    };
    nb_elements: number;
    nb_responses: number;
    distributions: Distributions;

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
        this.reminded = false;
        this.archived = false;
        this.multiple = false;
        this.anonymous = false;
        this.is_public = false;
        this.public_key = null;
        this.response_notified = false;
        this.editable = false;
        this.rgpd = false;
        this.rgpd_goal = null;
        this.rgpd_lifetime = 3;
        this.displayed = true;
        this.selected = null;

    }

    toJson() : Object {
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
            reminded: this.reminded,
            archived: this.archived,
            multiple: this.multiple,
            anonymous: this.anonymous,
            is_public: this.is_public,
            public_key: this.public_key,
            response_notified: this.response_notified,
            editable: this.editable,
            rgpd: this.rgpd,
            rgpd_goal: this.rgpd_goal,
            rgpd_lifetime: this.rgpd_lifetime,
            selected: this.selected
        }
    }

    setFromJson = (data: any) : void => {
        for (let key in data) {
            this[key] = data[key];
            if (key === 'nb_responses' && !data[key]) {
                this[key] = 0;
            }
            if ((key === 'date_creation' || key === 'date_modification' || key === 'date_opening' || key === 'date_ending') && data[key]) {
                this[key] = new Date(this[key]);
            }
        }
    };

    formatFormElements = (formElements: FormElements) : void => {
        for (let e of this['form_elements']) {
            if (!e['questions']) {
                formElements.all.push(this.formatIntoQuestion(e));
            }
            else {
                formElements.all.push(this.formatIntoSection(e));
            }
        }
        formElements.all.sort((a, b) => a.position - b.position);
        delete this['form_elements'];
    };

    formatIntoSection = (e: FormElement) : Section => {
        let questions = new Questions();
        if (e['questions']) {
            for (let q of e['questions']) {
                questions.all.push(this.formatIntoQuestion(q));
            }
        }
        questions.all.sort((a, b) => a.section_position - b.section_position);

        let section = Mix.castAs(Section, e);
        section.questions = questions;
        return section;
    };

    formatIntoQuestion = (e: FormElement) : Question => {
        let choices: QuestionChoices = new QuestionChoices();
        let children: Questions = new Questions();
        if (e[Fields.CHOICES]) {
            choices.all = Mix.castArrayAs(QuestionChoice, e[Fields.CHOICES]);
            choices.all.sort((a, b) => a.position - b.position);
        }
        if (e[Fields.CHILDREN]) {
            children.all = Mix.castArrayAs(Question, e[Fields.CHILDREN]);    // Ok because matrix children cannot not have choices or children themselves
            children.all.sort((a, b) => a.matrix_position - b.matrix_position);
        }

        let question: Question = Mix.castAs(Question, e);
        question.choices = choices;
        question.children = children;
        return question;
    };

    getDistributionKey = () : string => {
        let distributionKey = this[Fields.DISTRIBUTION_KEY].toString();
        delete this[Fields.DISTRIBUTION_KEY];
        return distributionKey;
    };

    getDistributionCaptcha = () : string => {
        let distributionCaptcha = this[Fields.DISTRIBUTION_CAPTCHA].toString();
        delete this[Fields.DISTRIBUTION_CAPTCHA];
        return distributionCaptcha;
    };

    generateShareRights = () : void => {
        this._id = this.id;
        this.owner = {userId: this.owner_id, displayName: this.owner_name};
        this.myRights = new Rights<Form>(this);
    };

    getNbFinishedDistribs = () : number => {
        if (this.distributions) {
            return this.distributions.all.filter(d => d.status === DistributionStatus.FINISHED).length;
        }
        return 0;
    };

    getDateResponse = () : Date => {
        return this.getMyLastDistrib().date_response;
    };

    getDateSending = () : Date => {
        return this.getMyFirstDistrib().date_sending;
    };

    getStatus = () : string => {
        return this.getMyLastDistrib().status;
    };

    getMyFirstDistrib = () : Distribution => {
        if (this.distributions) {
            return this.distributions.all[0];
        }
        return new Distribution();
    };

    getMyLastDistrib = () : Distribution => {
        if (this.distributions) {
            return this.distributions.all[this.distributions.all.length - 1];
        }
        return new Distribution();
    };

    setInfoImage = async () : Promise<void> => {
        const typesImgNoSend = ["image/png", "image/jpg", "image/jpeg", "image/gif"];
        try {
            let { metadata } = await utilsService.getInfoImage(this);
            this.infoImg = {
                name: metadata.filename,
                type: metadata["content-type"],
                compatible: !typesImgNoSend.some(type => type === metadata["content-type"]),
            };
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.form.image'));
            throw e;
        }
    };

    getPublicLink = () : string => {
        return `${window.location.origin}/formulaire-public#/form/${this.public_key}`;
    };
}

export class Forms extends Selection<Form> {
    all: Form[];
    visibles: Form[];

    order = {
        field: FiltersOrders.CREATION_DATE,
        desc: true
    };

    orders = [
        { name: FiltersOrders.CREATION_DATE, display: false },
        { name: FiltersOrders.MODIFICATION_DATE, display: false },
        { name: FiltersOrders.SENDING_DATE, display: false },
        { name: FiltersOrders.CREATOR, display: false },
        { name: FiltersOrders.TITLE, display: false }
    ];

    filters = [
        { name: FiltersFilters.SHARED, value: false, display: false },
        { name: FiltersFilters.SENT, value: false, display: false },
        { name: FiltersFilters.TO_DO, value: false, display: false },
        { name: FiltersFilters.FINISHED, value: false, display: false }
    ];

    constructor() {
        super([]);
    }

    sync = async () : Promise<void> => {
        this.all = [];
        try {
            let data = await formService.list();
            for (let i = 0; i < data.length; i++) {
                let tempForm = new Form();
                tempForm.setFromJson(data[i]);
                this.all.push(tempForm);
            }
            await this.setResourceRights();
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.form.sync'));
            throw e;
        }
    };

    syncSent = async () : Promise<void> => {
        try {
            let data = await formService.listSentForms();
            for (let i = 0; i < data.length; i++) {
                let tempForm = new Form();
                tempForm.setFromJson(data[i]);
                this.all.push(tempForm);
            }
            await this.setResourceRights();
        } catch (e) {
            notify.error(idiom.translate('formulaire.error.form.sync'));
            throw e;
        }
    };

    setResourceRights = async () : Promise<void> => {
        let data = await formService.getAllMyFormRights();
        let ids = this.all.map(form => form.id);
        for (let i = 0; i < ids.length; i++) {
            let formId = ids[i];
            let rights = data.filter(right => right.resource_id === formId).map(right => right.action);
            this.all.filter(form => form.id === formId)[0].myRights = rights;
        }
    };

    // Sorting / Filtering

    orderForms = () : void => {
        this.all = this.all.sort(
            (a, b) => {
                if (this.order.field == FiltersOrders.CREATION_DATE) {
                    if (a.date_creation > b.date_creation) return this.order.desc ? 1 : -1;
                    if (a.date_creation < b.date_creation) return this.order.desc ? -1 : 1;
                }
                else if (this.order.field == FiltersOrders.MODIFICATION_DATE) {
                    if (a.date_modification > b.date_modification) return this.order.desc ? 1 : -1;
                    if (a.date_modification < b.date_modification) return this.order.desc ? -1 : 1;
                }
                else if (this.order.field == FiltersOrders.SENDING_DATE) {
                    if (a.getDateSending() > b.getDateSending()) return this.order.desc ? 1 : -1;
                    if (a.getDateSending() < b.getDateSending()) return this.order.desc ? -1 : 1;
                }
                else if (this.order.field == FiltersOrders.CREATOR) {
                    if (a.owner_name.toLowerCase() < b.owner_name.toLowerCase()) return this.order.desc ? 1 : -1;
                    if (a.owner_name.toLowerCase() > b.owner_name.toLowerCase()) return this.order.desc ? -1 : 1;
                }
                else if (this.order.field == FiltersOrders.TITLE) {
                    if (a.title.toLowerCase() < b.title.toLowerCase()) return this.order.desc ? 1 : -1;
                    if (a.title.toLowerCase() > b.title.toLowerCase()) return this.order.desc ? -1 : 1;
                }
            }
        );
    };

    orderByField = (fieldName: FiltersOrders) : void => {
        if (fieldName === this.order.field) {
            this.order.desc = !this.order.desc;
        }
        else {
            this.order.desc = false;
            this.order.field = fieldName;
        }
    };

    isOrderedAsc = (field: FiltersOrders) : boolean => {
        return this.order.field === field && !this.order.desc;
    };

    isOrderedDesc = (field: FiltersOrders) : boolean => {
        return this.order.field === field && this.order.desc;
    };

    filterForms = () : void => {
        let objectFilters = {};
        for (let filter of this.filters) {
            objectFilters[filter.name] = filter;
        }

        for (let form of this.all) {
            form.displayed = true;

            if (objectFilters[FiltersFilters.SENT].display && objectFilters[FiltersFilters.SHARED].display) {
                // If both unchecked, display form
                if (!objectFilters[FiltersFilters.SENT].value && !objectFilters[FiltersFilters.SHARED].value) {
                    form.displayed = true;
                }
                else {
                    if (objectFilters[FiltersFilters.SENT].value && !form.sent) {
                        form.displayed = false;
                    }
                    if (objectFilters[FiltersFilters.SHARED].value && !form.collab) {
                        form.displayed = false;
                    }
                    if ((objectFilters[FiltersFilters.SENT].value && objectFilters[FiltersFilters.SHARED].value) && (!form.sent || !form.collab)) {
                        form.displayed = false;
                    }
                }
            }

            if (objectFilters[FiltersFilters.TO_DO].display &&  objectFilters[FiltersFilters.FINISHED].display) {
                if (!objectFilters[FiltersFilters.TO_DO].value && !objectFilters[FiltersFilters.FINISHED].value) {
                    form.displayed = true;
                }
                else {
                    if (form.getStatus() === DistributionStatus.TO_DO && !objectFilters[FiltersFilters.TO_DO].value) {
                        form.displayed = false;
                    }
                    if (form.getStatus() === DistributionStatus.FINISHED && !objectFilters[FiltersFilters.FINISHED].value) {
                        form.displayed = false;
                    }
                }
            }
        }
    };

    switchFilter = (key: string) : void => {
        let filter = this.filters.find(f => f.name === key);
        filter.value = !filter.value;
    };
}