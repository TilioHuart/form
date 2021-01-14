package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.ResponseService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class DefaultResponseService implements ResponseService {

    @Override
    public void list(String question_id, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.RESPONSE_TABLE + " WHERE question_id = ? ORDER BY created;";
        JsonArray params = new JsonArray().add(question_id);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String id, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.RESPONSE_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(id);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(JsonObject response, UserInfos user, String question_id, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Formulaire.RESPONSE_TABLE + " (question_id, answer, respondent_id, created) " +
                "VALUES (?, ?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(question_id)
                .add(response.getString("answer", ""))
                .add(user.getUserId())
                .add("NOW()");

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void update(UserInfos user, String id, JsonObject response, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Formulaire.RESPONSE_TABLE + " SET answer = ? " +
                "WHERE respondent_id = ? AND id = ?;";
        JsonArray params = new JsonArray()
                .add(response.getString("answer", ""))
                .add(user.getUserId())
                .add(id);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(String id, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Formulaire.RESPONSE_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(id);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
