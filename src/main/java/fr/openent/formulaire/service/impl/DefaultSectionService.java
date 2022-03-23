package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.helpers.SqlHelper;
import fr.openent.formulaire.service.SectionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

public class DefaultSectionService implements SectionService {

    @Override
    public void list(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.SECTION_TABLE + " WHERE form_id = ? ORDER BY position;";
        JsonArray params = new JsonArray().add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String sectionId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.SECTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(sectionId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(JsonObject section, String formId, Handler<Either<String, JsonObject>> handler) {
        String query = SqlHelper.getUpdateDateModifFormRequest();
        JsonArray params = SqlHelper.initParamsForUpdateDateModifFormRequest(formId);

        query += "INSERT INTO " + Formulaire.SECTION_TABLE + " (form_id, title, description, position) " +
                "VALUES (?, ?, ?, ?) RETURNING *;";
        params.add(formId)
                .add(section.getString("title", ""))
                .add(section.getString("description", ""))
                .add(section.getInteger("position", 0));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void update(String sectionId, JsonObject section, Handler<Either<String, JsonObject>> handler) {
        String query = SqlHelper.getUpdateDateModifFormRequest();
        JsonArray params = SqlHelper.initParamsForUpdateDateModifFormRequest(section.getInteger("form_id").toString());

        query += "UPDATE " + Formulaire.SECTION_TABLE + " SET title = ?, description = ?, position = ? WHERE id = ? RETURNING *;";
        params.add(section.getString("title", ""))
                .add(section.getString("description", ""))
                .add(section.getInteger("position", 0))
                .add(sectionId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(JsonObject section, Handler<Either<String, JsonObject>> handler) {
        String query = SqlHelper.getUpdateDateModifFormRequest();
        JsonArray params = SqlHelper.initParamsForUpdateDateModifFormRequest(section.getInteger("form_id").toString());

        query += "DELETE FROM " + Formulaire.SECTION_TABLE + " WHERE id = ?;";
        params.add(section.getInteger("id"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
