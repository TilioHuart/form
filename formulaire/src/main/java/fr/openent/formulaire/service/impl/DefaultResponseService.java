package fr.openent.formulaire.service.impl;

import fr.openent.form.core.constants.Tables;
import fr.openent.formulaire.service.ResponseService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import static fr.openent.form.core.constants.DistributionStatus.FINISHED;

public class DefaultResponseService implements ResponseService {

    @Override
    public void list(String questionId, String nbLines, JsonArray distribs, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT r.* FROM " + Tables.RESPONSE + " r ";
        JsonArray params = new JsonArray().add(questionId);

        if (nbLines != null && !nbLines.equals("null")) {
            query += "WHERE question_id = ? AND distribution_id IN " + Sql.listPrepared(distribs);
            for (int i = 0; i < distribs.size(); i++) {
                params.add(distribs.getJsonObject(i).getInteger("id"));
            }
        }
        else {
            query += "JOIN " + Tables.DISTRIBUTION + " d ON d.id = r.distribution_id " +
                    "WHERE question_id = ? AND status = ? ";
            params.add(FINISHED);
        }

        query += ";";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listMineByDistribution(String questionId, String distributionId, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Tables.RESPONSE + " WHERE question_id = ? AND responder_id = ? AND distribution_id = ? " +
                "ORDER BY choice_id;";
        JsonArray params = new JsonArray().add(questionId).add(user.getUserId()).add(distributionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listByDistribution(String distributionId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Tables.RESPONSE + " WHERE distribution_id = ? ORDER BY question_id, choice_id;";
        JsonArray params = new JsonArray().add(distributionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listByForm(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT r.* FROM " + Tables.RESPONSE + " r " +
                "JOIN " + Tables.DISTRIBUTION + " d ON d.id = r.distribution_id " +
                "WHERE form_id = ? ORDER BY question_id, choice_id;";
        JsonArray params = new JsonArray().add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void countByQuestions(JsonArray questionIds, Handler<Either<String, JsonObject>> handler) {
        String query="SELECT COUNT(*) FROM " + Tables.RESPONSE + " r " +
                "JOIN " + Tables.DISTRIBUTION + " d ON d.id = r.distribution_id " +
                "WHERE d.status = ? AND question_id IN " + Sql.listPrepared(questionIds);
        JsonArray params=new JsonArray().add(FINISHED).addAll(questionIds);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void get(String responseId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Tables.RESPONSE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(responseId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getMissingResponses(String formId, String distributionId, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH question_ids AS (" +
                "SELECT question_id FROM " + Tables.RESPONSE + " WHERE distribution_id = ?) " +
                "SELECT id FROM " + Tables.QUESTION + " " +
                "WHERE form_id = ? AND id NOT IN (SELECT * FROM question_ids)";
        JsonArray params = new JsonArray().add(distributionId).add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject response, UserInfos user, String questionId, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Tables.RESPONSE + " (question_id, choice_id, answer, responder_id, distribution_id) " +
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
        String query = "UPDATE " + Tables.RESPONSE + " SET answer = ?, choice_id = ? " +
                "WHERE responder_id = ? AND id = ? RETURNING *;";
        JsonArray params = new JsonArray()
                .add(response.getString("answer", ""))
                .add(response.getInteger("choice_id", null))
                .add(user.getUserId())
                .add(responseId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(JsonArray responseIds, String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "DELETE FROM " + Tables.RESPONSE + " WHERE id IN (" +
                "SELECT r.id FROM " + Tables.RESPONSE + " r " +
                "JOIN " + Tables.QUESTION + " q ON r.question_id = q.id " +
                "JOIN " + Tables.FORM + " f ON q.form_id = f.id " +
                "WHERE f.id = ? AND r.id IN " + Sql.listPrepared(responseIds) + ");";
        JsonArray params = new JsonArray().add(formId).addAll(responseIds);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void deleteMultipleByDistribution(String distributionId, Handler<Either<String, JsonArray>> handler) {
        String query = "DELETE FROM " + Tables.RESPONSE + " WHERE distribution_id = ?;";
        JsonArray params = new JsonArray().add(distributionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getExportCSVResponders(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, date_response, responder_id, responder_name, structure " +
                "FROM " + Tables.DISTRIBUTION + " " +
                "WHERE form_id = ? AND status = ? " +
                "ORDER BY date_response DESC, responder_id, id;";

        JsonArray params = new JsonArray().add(formId).add(FINISHED);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void exportCSVResponses(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT d.id, d.responder_id, date_response, structure, question_id, position, answer " +
                "FROM " + Tables.DISTRIBUTION + " d " +
                "JOIN " + Tables.RESPONSE + " r ON r.distribution_id = d.id " +
                "JOIN " + Tables.QUESTION + " q ON r.question_id = q.id " +
                "WHERE d.form_id = ? AND d.status = ? AND q.question_type != 1" +
                "ORDER BY d.date_response DESC, d.responder_id, d.id, position;";

        JsonArray params = new JsonArray().add(formId).add(FINISHED);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void exportPDFResponses(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT question_id, date_response, d.responder_id, responder_name, structure, answer, rf.filename " +
            "FROM " + Tables.DISTRIBUTION + " d " +
            "LEFT JOIN " + Tables.RESPONSE + " r ON r.distribution_id = d.id " +
            "LEFT JOIN " + Tables.RESPONSE_FILE + " rf ON rf.response_id = r.id " +
            "LEFT JOIN " + Tables.QUESTION + " q ON r.question_id = q.id " +
            "WHERE d.form_id = ? AND d.status = ? " +
            "ORDER BY position, date_response;";

        JsonArray params = new JsonArray().add(formId).add(FINISHED);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void deleteOldResponse(JsonArray distributionIds, Handler<Either<String, JsonArray>> handler) {
        String query =
                "DELETE FROM " + Tables.RESPONSE + " " +
                "WHERE distribution_id IN " + Sql.listPrepared(distributionIds) + " RETURNING *;";
        JsonArray params = new JsonArray().addAll(distributionIds);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}