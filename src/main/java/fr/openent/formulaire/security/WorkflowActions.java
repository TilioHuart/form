package fr.openent.formulaire.security;

import fr.openent.formulaire.Formulaire;

public enum WorkflowActions {
    ACCESS_RIGHT (Formulaire.ACCESS_RIGHT),
    CREATION_RIGHT (Formulaire.CREATION_RIGHT),
    SHARING_RIGHT (Formulaire.SHARING_RIGHT),
    SENDING_RIGHT (Formulaire.SENDING_RIGHT),
    RESPONSE_RIGHT(Formulaire.RESPONSE_RIGHT);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString () {
        return this.actionName;
    }
}
