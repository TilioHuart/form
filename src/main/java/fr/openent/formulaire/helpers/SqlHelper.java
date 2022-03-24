package fr.openent.formulaire.helpers;

import fr.openent.formulaire.Formulaire;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class SqlHelper {
    private static final Logger log = LoggerFactory.getLogger(SqlHelper.class);

    private SqlHelper() {}

    public static String getUpdateDateModifFormRequest() {
        return "UPDATE " + Formulaire.FORM_TABLE + " SET date_modification = ? WHERE id = ?; ";
    }

    public static JsonArray getParamsForUpdateDateModifFormRequest(String formId) {
        return new JsonArray().add("NOW()").add(formId);
    }
}
