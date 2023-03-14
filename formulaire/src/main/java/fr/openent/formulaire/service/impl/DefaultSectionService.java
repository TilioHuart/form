package fr.openent.formulaire.service.impl;

import fr.openent.form.helpers.FutureHelper;
import fr.openent.form.helpers.UtilsHelper;
import fr.openent.formulaire.service.SectionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;

import static fr.openent.form.core.constants.Constants.TRANSACTION_BEGIN_QUERY;
import static fr.openent.form.core.constants.Constants.TRANSACTION_COMMIT_QUERY;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.SECTION_TABLE;
import static fr.openent.form.helpers.SqlHelper.getParamsForUpdateDateModifFormRequest;
import static fr.openent.form.helpers.SqlHelper.getUpdateDateModifFormRequest;

public class DefaultSectionService implements SectionService {
    private final Sql sql = Sql.getInstance();

    @Override
    public void list(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + SECTION_TABLE + " WHERE form_id = ? ORDER BY position;";
        JsonArray params = new JsonArray().add(formId);
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public Future<JsonArray> list(String formId) {
        Promise<JsonArray> promise = Promise.promise();

        String errorMessage = "[Formulaire@DefaultSectionService::list] Fail to list sections for form with id " + formId + " : ";
        list(formId, FutureHelper.handlerEither(promise, errorMessage));

        return promise.future();
    }

    @Override
    public void get(String sectionId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + SECTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(sectionId);
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(JsonObject section, String formId, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + SECTION_TABLE + " (form_id, title, description, position) " +
                "VALUES (?, ?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(formId)
                .add(section.getString(TITLE, ""))
                .add(section.getString(DESCRIPTION, ""))
                .add(section.getInteger(POSITION, null));

        query += getUpdateDateModifFormRequest();
        params.addAll(getParamsForUpdateDateModifFormRequest(formId));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public Future<JsonArray> update(String formId, JsonArray sections) {
        Promise<JsonArray> promise = Promise.promise();

        if (!sections.isEmpty()) {
            SqlStatementsBuilder s = new SqlStatementsBuilder();
            String nullifyerQuery = "UPDATE " + SECTION_TABLE + " SET position = NULL WHERE id IN " + Sql.listPrepared(sections) + ";";
            String query = "UPDATE " + SECTION_TABLE + " SET title = ?, description = ?, position = ? WHERE id = ? RETURNING *;";

            s.raw(TRANSACTION_BEGIN_QUERY);
            s.prepared(nullifyerQuery, UtilsHelper.getIds(sections, false));
            for (int i = 0; i < sections.size(); i++) {
                JsonObject section = sections.getJsonObject(i);
                JsonArray params = new JsonArray()
                        .add(section.getString(TITLE, ""))
                        .add(section.getString(DESCRIPTION, ""))
                        .add(section.getInteger(POSITION, null))
                        .add(section.getInteger(ID, null));
                s.prepared(query, params);
            }

            s.prepared(getUpdateDateModifFormRequest(), getParamsForUpdateDateModifFormRequest(formId));
            s.raw(TRANSACTION_COMMIT_QUERY);

            String errorMessage = "[Formulaire@DefaultSectionService::update] Fail to update sections " + sections + " : ";
            sql.transaction(s.build(), SqlResult.validResultsHandler(FutureHelper.handlerEither(promise, errorMessage)));
        }
        else {
            promise.complete(new JsonArray());
        }

        return promise.future();
    }

    @Override
    public void delete(JsonObject section, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + SECTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(section.getInteger(ID).toString());

        query += getUpdateDateModifFormRequest();
        params.addAll(getParamsForUpdateDateModifFormRequest(section.getInteger(FORM_ID).toString()));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
