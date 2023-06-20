package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.service.ResponseService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import static fr.openent.form.core.constants.Constants.QUESTIONS_WITHOUT_RESPONSES;
import static fr.openent.form.core.constants.DistributionStatus.FINISHED;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.*;

public class DefaultResponseService implements ResponseService {

    @Override
    public void list(String questionId, String nbLines, JsonArray distribs, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT r.* FROM " + RESPONSE_TABLE + " r ";
        JsonArray params = new JsonArray().add(questionId);

        if (nbLines != null && !nbLines.equals(NULL)) {
            query += "WHERE question_id = ? AND distribution_id IN " + Sql.listPrepared(distribs);
            for (int i = 0; i < distribs.size(); i++) {
                params.add(distribs.getJsonObject(i).getInteger(ID));
            }
        }
        else {
            query += "JOIN " + DISTRIBUTION_TABLE + " d ON d.id = r.distribution_id " +
                    "WHERE (question_id = ? OR question_id IN (" +
                        "SELECT id FROM " + QUESTION_TABLE + " WHERE matrix_id = ?" +
                    ")) AND status = ?";
            params.add(questionId).add(FINISHED);
        }

        query += ";";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listMineByDistribution(String questionId, String distributionId, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + RESPONSE_TABLE + " WHERE question_id = ? AND responder_id = ? AND distribution_id = ? " +
                "ORDER BY choice_id;";
        JsonArray params = new JsonArray().add(questionId).add(user.getUserId()).add(distributionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listByDistribution(String distributionId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + RESPONSE_TABLE + " WHERE distribution_id = ? ORDER BY question_id, choice_id;";
        JsonArray params = new JsonArray().add(distributionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listByForm(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT r.* FROM " + RESPONSE_TABLE + " r " +
                "JOIN " + DISTRIBUTION_TABLE + " d ON d.id = r.distribution_id " +
                "WHERE form_id = ? ORDER BY question_id, choice_id;";
        JsonArray params = new JsonArray().add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void countByQuestions(JsonArray questionIds, Handler<Either<String, JsonObject>> handler) {
        String query="SELECT COUNT(*) FROM " + RESPONSE_TABLE + " r " +
                "JOIN " + DISTRIBUTION_TABLE + " d ON d.id = r.distribution_id " +
                "WHERE d.status = ? AND question_id IN " + Sql.listPrepared(questionIds);
        JsonArray params=new JsonArray().add(FINISHED).addAll(questionIds);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void get(String responseId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + RESPONSE_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(responseId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getMissingResponses(String formId, String distributionId, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH question_ids AS (" +
                "SELECT question_id FROM " + RESPONSE_TABLE + " WHERE distribution_id = ?) " +
                "SELECT id FROM " + QUESTION_TABLE + " " +
                "WHERE form_id = ? AND id NOT IN (SELECT * FROM question_ids)";
        JsonArray params = new JsonArray().add(distributionId).add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject response, UserInfos user, String questionId, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + RESPONSE_TABLE + " (question_id, choice_id, answer, responder_id, distribution_id, choice_position, custom_answer, image) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(questionId)
                .add(response.getInteger(CHOICE_ID, null))
                .add(response.getString(ANSWER, ""))
                .add(user.getUserId())
                .add(response.getInteger(DISTRIBUTION_ID, null))
                .add(response.getInteger(CHOICE_POSITION, null))
                .add(response.getString(CUSTOM_ANSWER, null))
                .add(response.getString(IMAGE, null));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void update(UserInfos user, String responseId, JsonObject response, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + RESPONSE_TABLE + " SET answer = ?, choice_id = ?, custom_answer = ?, image = ? " +
                "WHERE responder_id = ? AND id = ? RETURNING *;";
        JsonArray params = new JsonArray()
                .add(response.getString(ANSWER, ""))
                .add(response.getInteger(CHOICE_ID, null))
                .add(response.getString(CUSTOM_ANSWER, null))
                .add(response.getString(IMAGE, null))
                .add(user.getUserId())
                .add(responseId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(JsonArray responseIds, String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "DELETE FROM " + RESPONSE_TABLE + " WHERE id IN (" +
                "SELECT r.id FROM " + RESPONSE_TABLE + " r " +
                "JOIN " + QUESTION_TABLE + " q ON r.question_id = q.id " +
                "JOIN " + FORM_TABLE + " f ON q.form_id = f.id " +
                "WHERE f.id = ? AND r.id IN " + Sql.listPrepared(responseIds) + ");";
        JsonArray params = new JsonArray().add(formId).addAll(responseIds);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void deleteByQuestionAndDistribution(String questionId, String distributionId, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "DELETE FROM " + RESPONSE_TABLE + " WHERE id IN (" +
                    "SELECT r.id FROM " + RESPONSE_TABLE + " r " +
                    "JOIN " + DISTRIBUTION_TABLE + " d ON r.distribution_id = d.id " +
                    "WHERE r.distribution_id = ? AND r.responder_id = ? " +
                    "AND r.question_id IN (SELECT id FROM " + QUESTION_TABLE + " WHERE matrix_id = ? OR id = ?)" +
                ")";
        JsonArray params = new JsonArray().add(distributionId).add(user.getUserId()).add(questionId).add(questionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void deleteMultipleByDistribution(String distributionId, Handler<Either<String, JsonArray>> handler) {
        String query = "DELETE FROM " + RESPONSE_TABLE + " WHERE distribution_id = ?;";
        JsonArray params = new JsonArray().add(distributionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getExportCSVResponders(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, date_response, responder_id, responder_name, structure " +
                "FROM " + DISTRIBUTION_TABLE + " " +
                "WHERE form_id = ? AND status = ? " +
                "ORDER BY date_response DESC, responder_id, id;";

        JsonArray params = new JsonArray().add(formId).add(FINISHED);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void exportCSVResponses(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT d.id, d.responder_id, date_response, structure, question_id, position, answer, custom_answer " +
                "FROM " + DISTRIBUTION_TABLE + " d " +
                "JOIN " + RESPONSE_TABLE + " r ON r.distribution_id = d.id " +
                "JOIN " + QUESTION_TABLE + " q ON r.question_id = q.id " +
                "WHERE d.form_id = ? AND d.status = ? AND q.question_type NOT IN " + Sql.listPrepared(QUESTIONS_WITHOUT_RESPONSES) +
                "ORDER BY d.date_response DESC, d.responder_id, d.id, position, choice_position, q.id;";

        JsonArray params = new JsonArray().add(formId).add(FINISHED).addAll(new JsonArray(QUESTIONS_WITHOUT_RESPONSES));
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void exportPDFResponses(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT question_id, date_response, d.responder_id, responder_name, structure, answer, " +
            "custom_answer, rf.filename " +
            "FROM " + DISTRIBUTION_TABLE + " d " +
            "LEFT JOIN " + RESPONSE_TABLE + " r ON r.distribution_id = d.id " +
            "LEFT JOIN " + RESPONSE_FILE_TABLE + " rf ON rf.response_id = r.id " +
            "LEFT JOIN " + QUESTION_TABLE + " q ON r.question_id = q.id " +
            "WHERE d.form_id = ? AND d.status = ? " +
            "ORDER BY position ASC, date_response DESC;";

        JsonArray params = new JsonArray().add(formId).add(FINISHED);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void deleteOldResponse(JsonArray distributionIds, Handler<Either<String, JsonArray>> handler) {
        String query =
                "DELETE FROM " + RESPONSE_TABLE + " " +
                "WHERE distribution_id IN " + Sql.listPrepared(distributionIds) + " RETURNING *;";
        JsonArray params = new JsonArray().addAll(distributionIds);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}