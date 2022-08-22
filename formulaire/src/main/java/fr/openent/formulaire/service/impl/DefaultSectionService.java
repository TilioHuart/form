package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.service.SectionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;

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
    public void update(String formId, JsonArray sections, Handler<Either<String, JsonArray>> handler) {
        if (!sections.isEmpty()) {
            SqlStatementsBuilder s = new SqlStatementsBuilder();
            String query = "UPDATE " + SECTION_TABLE + " SET title = ?, description = ?, position = ? WHERE id = ? RETURNING *;";

            s.raw("BEGIN;");
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
            s.raw("COMMIT;");

            sql.transaction(s.build(), SqlResult.validResultsHandler(handler));
        }
        else {
            handler.handle(new Either.Right<>(new JsonArray()));
        }
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
