import {Behaviours} from 'entcore';

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
        creation: 'fr.openent.formulaire.controllers.FormController|update',
        response: 'fr.openent.formulaire.controllers.ResponseController|create'
    }
};

Behaviours.register('formulaire', {
    rights: rights,
    dependencies: {},
    loadResources: function (callback) { },

    resourceRights: function () {
        return ['contrib', 'manager', 'comment'];
    }
});
