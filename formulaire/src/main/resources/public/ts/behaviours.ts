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
        this.resources = data.map(function (f) {
            if (!f.picture) f.picture = '../../../../formulaire/public/img/logo.svg';
            let endPath = f.rgpd ? 'rgpd' : 'new';

            return {
                id: f.id,
                icon: f.picture,
                title: f.title,
                ownerName: f.owner_name,
                path: `/formulaire#/form/${f.id}/${endPath}`
            }
        });
    },

    resourceRights: function () {
        return ['contrib', 'manager', 'comment'];
    }
});