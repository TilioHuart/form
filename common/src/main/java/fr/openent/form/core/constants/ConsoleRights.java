package fr.openent.form.core.constants;

public class ConsoleRights {
    public static final String ACCESS_RIGHT = "formulaire.access";
    public static final String CREATION_RIGHT = "formulaire.creation";
    public static final String RESPONSE_RIGHT = "formulaire.response";
    public static final String RGPD_RIGHT = "formulaire.rgpd.data.collection";

    private ConsoleRights() {
        throw new IllegalStateException("Utility class");
    }
}

