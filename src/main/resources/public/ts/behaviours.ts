import {Behaviours, model} from 'entcore';

const rights = {
    resources: {
        // read: {
        //     right: "fr-openent-formulaire-controllers-DistributionController|list"
        // },
        // contrib: {
        //     right: "fr-openent-formulaire-controllers-DistributionController|create"
        // },
        // manager: {
        //     right: "fr-openent-formulaire-controllers-DistributionController|update"
        // }
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
    rights: rights
    // dependencies: {},
    //
    // /**  Allows to set rights for behaviours. */
    // resource : function(resource) {
    //     let rightsContainer = resource;
    //
    //     if (resource && !resource.myRights) {
    //         resource.myRights = {};
    //     }
    //
    //     for (const behaviour in rights.resources) {
    //         if (model.me.hasRight(rightsContainer, rights.resources[behaviour]) || model.me.userId === resource.owner_id || model.me.userId === rightsContainer.owner_id) {
    //             if (resource.myRights[behaviour] !== undefined) {
    //                 resource.myRights[behaviour] = resource.myRights[behaviour] && rights.resources[behaviour];
    //             } else {
    //                 resource.myRights[behaviour] = rights.resources[behaviour];
    //             }
    //         }
    //     }
    //     return resource;
    // },
    //
    // /** Allows to define all rights to display in the share windows. Names are defined in the server part with
    //  * <code>@SecuredAction(value = "xxxx.read", type = ActionType.RESOURCE)</code> without the prefix <code>xxx</code>. */
    // resourceRights: function () {
    //     return ['contrib', 'manager'];
    // },
    //
    // loadResources: function () {
    // }
});
