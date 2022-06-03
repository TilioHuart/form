package fr.openent.formulaire.security;

import fr.openent.form.core.constants.ConsoleRights;

public enum WorkflowActions {
    ACCESS_RIGHT (ConsoleRights.ACCESS_RIGHT),
    CREATION_RIGHT (ConsoleRights.CREATION_RIGHT),
    RESPONSE_RIGHT(ConsoleRights.RESPONSE_RIGHT),
    RGPD_RIGHT(ConsoleRights.RGPD_RIGHT),
    CREATION_PUBLIC_RIGHT(ConsoleRights.CREATION_PUBLIC_RIGHT);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString () {
        return this.actionName;
    }
}
