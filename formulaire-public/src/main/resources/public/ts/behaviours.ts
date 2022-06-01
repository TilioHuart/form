import {Behaviours} from 'entcore';

const rights = {
    workflow: {
        access: 'fr.openent.formulaire_public.controllers.FormulairePublicController|initSecuredActions'
    }
};

Behaviours.register('formulaire-public', {
    rights: rights
});