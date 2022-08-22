package fr.openent.form.core.constants;

public class ConfigFields {
    public static final String ZIMBRA_MAX_RECIPIENTS = "zimbra-max-recipients";
    public static final String RGPD_CRON = "rgpd-cron";
    public static final String MAX_RESPONSE_EXPORT_PDF = "max-responses-export-PDF";
    public static final String MAX_USERS_SHARING = "max-users-sharing";
    public static final String NODE_PDF_GENERATOR = "node-pdf-generator";
    public static final String PDF_CONNECTOR_ID = "pdf-connector-id";
    public static final String AUTH = "auth";
    public static final String URL = "url";


    private ConfigFields() {
        throw new IllegalStateException("Utility class");
    }
}

