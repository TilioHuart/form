package fr.openent.form.core.constants;

public class ShareRights {
    public static final String READ_RESOURCE_RIGHT = "formulaire.read";
    public static final String CONTRIB_RESOURCE_RIGHT = "formulaire.contrib";
    public static final String MANAGER_RESOURCE_RIGHT = "formulaire.manager";
    public static final String RESPONDER_RESOURCE_RIGHT = "formulaire.comment";

    public static final String READ_RESOURCE_BEHAVIOUR = "fr-openent-formulaire-controllers-FormController|initReadResourceRight";
    public static final String CONTRIB_RESOURCE_BEHAVIOUR = "fr-openent-formulaire-controllers-FormController|initContribResourceRight";
    public static final String MANAGER_RESOURCE_BEHAVIOUR = "fr-openent-formulaire-controllers-FormController|initManagerResourceRight";
    public static final String RESPONDER_RESOURCE_BEHAVIOUR = "fr-openent-formulaire-controllers-FormController|initResponderResourceRight";

    private ShareRights() {
        throw new IllegalStateException("Utility class");
    }
}

