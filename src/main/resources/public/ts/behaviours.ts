import {Behaviours} from 'entcore';

const rights = {
    resources: {
        read: {
            right: "fr-openent-formulaire-controllers-FormController|initReadResourceRight"
        },
        contrib: {
            right: "fr-openent-formulaire-controllers-DistributionController|create"
        },
        manager: {
            right: "fr-openent-formulaire-controllers-DistributionController|update"
        }
    },
    workflow: {
        access: 'fr.openent.formulaire.controllers.FormulaireController|render',
        creation: 'fr.openent.formulaire.controllers.FormController|update',
        response: 'fr.openent.formulaire.controllers.ResponseController|create'
        // sending: 'fr.openent.formulaire.controllers.FormController|create',
        // sharing: 'fr.openent.formulaire.controllers.FormController|create',
    }
};

Behaviours.register('formulaire', {
    rights: rights,
    dependencies: {},
    loadResources: function (callback) { },

    resourceRights: function () {
        return ['read'];
    }
});
