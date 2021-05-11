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
    public void list(String questionId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.RESPONSE_TABLE + " WHERE question_id = ?;";
        JsonArray params = new JsonArray().add(questionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listMineByDistribution(String questionId, String distributionId, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.RESPONSE_TABLE + " WHERE question_id = ? AND responder_id = ?  AND distribution_id = ? ORDER BY choice_id;";
        JsonArray params = new JsonArray().add(questionId).add(user.getUserId()).add(distributionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String responseId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.RESPONSE_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(responseId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(JsonObject response, UserInfos user, String questionId, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Formulaire.RESPONSE_TABLE + " (question_id, choice_id, answer, responder_id, distribution_id) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(questionId)
                .add(response.getInteger("choice_id", null))
                .add(response.getString("answer", ""))
                .add(user.getUserId())
                .add(response.getInteger("distribution_id", null));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void update(UserInfos user, String responseId, JsonObject response, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Formulaire.RESPONSE_TABLE + " SET answer = ?, choice_id = ? " +
                "WHERE responder_id = ? AND id = ? RETURNING *;";
        JsonArray params = new JsonArray()
                .add(response.getString("answer", ""))
                .add(response.getInteger("choice_id", null))
                .add(user.getUserId())
                .add(responseId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(String responseId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Formulaire.RESPONSE_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(responseId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void exportResponses(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT d.form_id, d.responder_id, date_response, structure, question_id, position, answer " +
            "FROM " + Formulaire.DISTRIBUTION_TABLE + " d " +
            "JOIN " + Formulaire.RESPONSE_TABLE + " r ON r.distribution_id = d.id " +
            "JOIN " + Formulaire.QUESTION_TABLE + " q ON r.question_id = q.id " +
            "WHERE d.form_id = ? AND d.status = ? AND q.question_type != 1" +
            "ORDER BY d.form_id, d.responder_id, date_response, position;";

        JsonArray params = new JsonArray().add(formId).add(Formulaire.FINISHED);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
