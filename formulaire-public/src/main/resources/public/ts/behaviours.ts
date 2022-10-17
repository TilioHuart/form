import {Behaviours} from 'entcore';

const rights = {
    workflow: {
        access: 'fr.openent.formulaire_public.controllers.FormulairePublicController|initAccessRight'
    }
};

Behaviours.register('formulaire-public', {
    rights: rights
});