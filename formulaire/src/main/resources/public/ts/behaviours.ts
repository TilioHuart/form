import {Behaviours} from 'entcore';
import {formService} from "@common/services";

const rights = {
    resources: {
        contrib: {
            right: "fr-openent-formulaire-controllers-FormController|initContribResourceRight"
        },
        manager: {
            right: "fr-openent-formulaire-controllers-FormController|initManagerResourceRight"
        },
        comment: {
            right: "fr-openent-formulaire-controllers-FormController|initResponderResourceRight"
        }
    },
    workflow: {
        access: 'fr.openent.formulaire.controllers.FormulaireController|render',
        creation: 'fr.openent.formulaire.controllers.FormController|initCreationRight',
        response: 'fr.openent.formulaire.controllers.FormController|initResponseRight',
        rgpd: 'fr.openent.formulaire.controllers.DelegateController|initRGPDRight',
        creationPublic: 'fr.openent.formulaire.controllers.FormController|initCreationPublicRight'
    }
};

Behaviours.register('formulaire', {
    rights: rights,
    dependencies: {},
    loadResources: async function(): Promise<any> {
        const data = await formService.listForLinker();
        this.resources = data.map(form => {
            if (!form.picture) form.picture = '../../../../formulaire/public/img/logo.svg';
            return {
                id: form.id,
                icon: form.picture,
                title: form.title,
                ownerName: form.owner_name,
                path: form.is_public ?
                    `${window.location.origin}/formulaire-public#/form/${form.public_key}` :
                    `${window.location.origin}/formulaire#/form/${form.id}/${form.rgpd ? 'rgpd' : 'new'}`
            }
        });
    },

    resourceRights: function () {
        return ['contrib', 'manager', 'comment'];
    }
});