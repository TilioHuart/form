import {Behaviours, model} from 'entcore';

const rights = {
    resources: {},
    workflow: {
        access: 'fr.openent.formulaire.controllers.FormulaireController|render',
        creation: 'fr.openent.formulaire.controllers.FormController|update',
        response: 'fr.openent.formulaire.controllers.ResponseController|create'
        // sending: 'fr.openent.formulaire.controllers.FormController|create',
        // sharing: 'fr.openent.formulaire.controllers.FormController|create',
    }
};

Behaviours.register('formulaire', {
    rights: rights
});
