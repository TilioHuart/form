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
    public void list(String questionId, String nbLines, JsonArray distribs, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.RESPONSE_TABLE + " r ";
        JsonArray params = new JsonArray().add(questionId);

        if (!nbLines.equals("null")) {
            query += "WHERE question_id = ? AND distribution_id IN " + Sql.listPrepared(distribs);
            for (int i = 0; i < distribs.size(); i++) {
                params.add(distribs.getJsonObject(i).getInteger("id"));
            }
        }
        else {
            query += "JOIN " + Formulaire.DISTRIBUTION_TABLE + " d ON d.id = r.distribution_id " +
                    "WHERE question_id = ? AND status = ? ";
            params.add(Formulaire.FINISHED);
        }

        query += ";";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listMineByDistribution(String questionId, String distributionId, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.RESPONSE_TABLE + " WHERE question_id = ? AND responder_id = ? AND distribution_id = ? " +
                "ORDER BY choice_id;";
        JsonArray params = new JsonArray().add(questionId).add(user.getUserId()).add(distributionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listByDistribution(String distributionId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.RESPONSE_TABLE + " WHERE distribution_id = ? ORDER BY question_id, choice_id;";
        JsonArray params = new JsonArray().add(distributionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listByForm(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT r.* FROM " + Formulaire.RESPONSE_TABLE + " r " +
                "JOIN " + Formulaire.DISTRIBUTION_TABLE + " d ON d.id = r.distribution_id " +
                "WHERE form_id = ? ORDER BY question_id, choice_id;";
        JsonArray params = new JsonArray().add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void countByQuestions(JsonArray questionIds, Handler<Either<String, JsonObject>> handler) {
        String query="SELECT COUNT(*) FROM " + Formulaire.RESPONSE_TABLE + " r " +
                "JOIN " + Formulaire.DISTRIBUTION_TABLE + " d ON d.id = r.distribution_id " +
                "WHERE d.status = ? AND question_id IN " + Sql.listPrepared(questionIds);
        JsonArray params=new JsonArray().add(Formulaire.FINISHED).addAll(questionIds);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void get(String responseId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.RESPONSE_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(responseId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getMissingResponses(String formId, String distributionId, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH question_ids AS (" +
                "SELECT question_id FROM " + Formulaire.RESPONSE_TABLE + " WHERE distribution_id = ?) " +
                "SELECT id FROM " + Formulaire.QUESTION_TABLE + " " +
                "WHERE form_id = ? AND id NOT IN (SELECT * FROM question_ids)";
        JsonArray params = new JsonArray().add(distributionId).add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
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
    public void fillResponses(JsonArray questionIds, String distributionId, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Formulaire.RESPONSE_TABLE + " (question_id, answer, responder_id, distribution_id) VALUES ";
        JsonArray params = new JsonArray();

        for (int i = 0; i < questionIds.size(); i++) {
            query += "(?, ?, ?, ?), ";
            params.add(questionIds.getJsonObject(i).getInteger("id")).add("").add(user.getUserId()).add(distributionId);
        }

        query = query.substring(0, query.length() - 2) + ";";
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
    public void delete(JsonArray responseIds, Handler<Either<String, JsonArray>> handler) {
        String query = "DELETE FROM " + Formulaire.RESPONSE_TABLE + " WHERE id IN " + Sql.listPrepared(responseIds) + ";";
        JsonArray params = new JsonArray().addAll(responseIds);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void deleteMultipleByDistribution(String distributionId, Handler<Either<String, JsonArray>> handler) {
        String query = "DELETE FROM " + Formulaire.RESPONSE_TABLE + " WHERE distribution_id = ?;";
        JsonArray params = new JsonArray().add(distributionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getExportCSVResponders(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, date_response, responder_id, responder_name, structure " +
                "FROM " + Formulaire.DISTRIBUTION_TABLE + " " +
                "WHERE form_id = ? AND status = ? " +
                "ORDER BY date_response DESC, responder_id, id;";

        JsonArray params = new JsonArray().add(formId).add(Formulaire.FINISHED);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void exportCSVResponses(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT d.id, d.responder_id, date_response, structure, question_id, position, answer " +
                "FROM " + Formulaire.DISTRIBUTION_TABLE + " d " +
                "JOIN " + Formulaire.RESPONSE_TABLE + " r ON r.distribution_id = d.id " +
                "JOIN " + Formulaire.QUESTION_TABLE + " q ON r.question_id = q.id " +
                "WHERE d.form_id = ? AND d.status = ? AND q.question_type != 1" +
                "ORDER BY d.date_response DESC, d.responder_id, d.id, position;";

        JsonArray params = new JsonArray().add(formId).add(Formulaire.FINISHED);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void exportPDFResponses(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT question_id, date_response, d.responder_id, responder_name, structure, answer, rf.filename " +
            "FROM " + Formulaire.DISTRIBUTION_TABLE + " d " +
            "LEFT JOIN " + Formulaire.RESPONSE_TABLE + " r ON r.distribution_id = d.id " +
            "LEFT JOIN " + Formulaire.RESPONSE_FILE_TABLE + " rf ON rf.response_id = r.id " +
            "LEFT JOIN " + Formulaire.QUESTION_TABLE + " q ON r.question_id = q.id " +
            "WHERE d.form_id = ? AND d.status = ? " +
            "ORDER BY position, date_response;";

        JsonArray params = new JsonArray().add(formId).add(Formulaire.FINISHED);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void deleteOldResponse(JsonArray distributionIds, Handler<Either<String, JsonArray>> handler) {
        String query =
                "DELETE FROM " + Formulaire.RESPONSE_TABLE + " " +
                "WHERE distribution_id IN " + Sql.listPrepared(distributionIds) + " RETURNING *;";
        JsonArray params = new JsonArray().addAll(distributionIds);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}