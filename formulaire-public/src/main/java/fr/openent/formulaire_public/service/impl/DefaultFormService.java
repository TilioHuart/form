package fr.openent.formulaire_public.service.impl;

import fr.openent.form.core.constants.ShareRights;
import fr.openent.form.core.constants.Tables;
import fr.openent.formulaire_public.service.FormService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import static fr.openent.form.core.constants.Tables.FORM_SHARES_TABLE;
import static fr.openent.form.core.constants.Tables.FORM_TABLE;

public class DefaultFormService implements FormService {

    @Override
    public void getFormByKey(String formKey, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + FORM_TABLE + " WHERE public_key = ?;";
        JsonArray params = new JsonArray().add(formKey);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void listManagers(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT fs.member_id AS id FROM " + FORM_TABLE + " f " +
                "JOIN " + FORM_SHARES_TABLE + " fs ON fs.resource_id = f.id " +
                "WHERE f.id = ? AND fs.action = ? " +
                "UNION " +
                "SELECT owner_id FROM "  + FORM_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(formId).add(ShareRights.MANAGER_RESOURCE_BEHAVIOUR).add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
