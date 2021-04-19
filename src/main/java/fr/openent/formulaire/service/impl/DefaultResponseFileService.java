package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.ResponseFileService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultResponseFileService implements ResponseFileService {

    @Override
    public void list(String responseId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.RESPONSE_FILE_TABLE + " WHERE response_id = ?;";
        JsonArray params = new JsonArray().add(responseId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String responseId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.RESPONSE_FILE_TABLE + " WHERE response_id = ?;";
        JsonArray params = new JsonArray().add(responseId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(String responseId, String fileId, String filename, String type, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Formulaire.RESPONSE_FILE_TABLE + " (id, response_id, filename, type) VALUES (?, ?, ?, ?);";
        JsonArray params = new JsonArray().add(fileId).add(responseId).add(filename).add(type);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(String responseId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Formulaire.RESPONSE_FILE_TABLE + " WHERE response_id = ? RETURNING *;";
        JsonArray params = new JsonArray().add(responseId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
