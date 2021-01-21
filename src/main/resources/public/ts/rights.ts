const rights = {
    workflow: {
        access: 'fr.openent.formulaire.controllers.FormulaireController|view',
        creation: 'fr.openent.formulaire.controllers.FormController|update',
        response: 'fr.openent.formulaire.controllers.ResponseController|create'
        // sending: 'fr.openent.formulaire.controllers.FormController|create',
        // sharing: 'fr.openent.formulaire.controllers.FormController|create',
    },
    resource: {}
};

export default rights;