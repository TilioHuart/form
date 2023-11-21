package fr.openent.formulaire.service.impl;

import fr.openent.form.core.models.Response;
import fr.openent.form.core.models.TransactionElement;
import fr.openent.form.helpers.IModelHelper;
import fr.openent.form.helpers.TransactionHelper;
import fr.openent.formulaire.service.ResponseService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.List;

import java.util.Collections;

import static fr.openent.form.core.constants.Constants.QUESTIONS_WITHOUT_RESPONSES;
import static fr.openent.form.core.constants.DistributionStatus.FINISHED;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.*;

public class DefaultResponseService implements ResponseService {

    private static final Logger log = LoggerFactory.getLogger(DefaultResponseService.class);

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
    public Future<List<Response>> listMineByQuestionsIds(JsonArray questionsIds, String distributionId, String userId){
        Promise<List<Response>> promise = Promise.promise();

        if (questionsIds.isEmpty()) {
            promise.complete(Collections.emptyList());
            return promise.future();
        }

        String query = "SELECT * FROM " + RESPONSE_TABLE + " WHERE question_id IN " + Sql.listPrepared(questionsIds) + " " +
                "AND responder_id = ? AND distribution_id = ? " +
                "ORDER BY choice_id;";
        JsonArray params = new JsonArray().addAll(questionsIds).add(userId).add(distributionId);

        String errorMessage = "[Formulaire@DefaultResponseService::ListMineByQuestionsIds] Failed to get responses";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, Response.class, errorMessage)));

        return promise.future();
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

    public Future<List<Response>> listByIds(List<String> responseIds) {
        Promise<List<Response>> promise = Promise.promise();

        if (responseIds == null || responseIds.isEmpty()) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        String query = "SELECT * FROM " + RESPONSE_TABLE + " WHERE id IN " + Sql.listPrepared(responseIds);
        JsonArray params = new JsonArray(responseIds);

        String errorMessage = "[Formulaire@DefaultResponseService::listByIds] An error occurred while listing responses for ids " + responseIds;
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, Response.class, errorMessage)));

        return promise.future();
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
    public Future<List<Response>> createMultiple(List<Response> responses, String userId) {
        Promise<List<Response>> promise = Promise.promise();

        if (responses == null || responses.isEmpty()) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        List<TransactionElement> transactionElements = new ArrayList<>();
        responses.forEach(response -> {
            String query = "INSERT INTO " + RESPONSE_TABLE + " (question_id, choice_id, answer, responder_id, distribution_id, choice_position, custom_answer, image) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";
            JsonArray params = new JsonArray()
                    .add(response.getQuestionId())
                    .add(response.getChoiceId())
                    .add(response.getAnswer())
                    .add(userId)
                    .add(response.getDistributionId())
                    .add(response.getChoicePosition())
                    .add(response.getCustomAnswer())
                    .add(response.getImage());

            transactionElements.add(new TransactionElement(query, params));
        });

        String errorMessage = "[Formulaire@DefaultResponseService::createMultiple] Failed to create multiple responses";
        TransactionHelper.executeTransactionAndGetJsonObjectResults(transactionElements, errorMessage)
                .onSuccess(result -> promise.complete(IModelHelper.toList(result, Response.class)))
                .onFailure(err -> promise.fail(err.getMessage()));

        return promise.future();
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
    public Future<List<Response>> updateMultiple(List<Response> responses, String userId) {
        Promise<List<Response>> promise = Promise.promise();

        if (responses == null || responses.isEmpty()) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        List<TransactionElement> transactionElements = new ArrayList<>();
        responses.forEach(response -> {
            String query = "UPDATE " + RESPONSE_TABLE + " SET answer = ?, choice_id = ?, custom_answer = ?, image = ? " +
                    "WHERE responder_id = ? AND id = ? RETURNING *;";
            JsonArray params = new JsonArray()
                    .add(response.getAnswer())
                    .add(response.getChoiceId())
                    .add(response.getCustomAnswer())
                    .add(response.getImage())
                    .add(userId)
                    .add(response.getId());

            transactionElements.add(new TransactionElement(query, params));
        });

        String errorMessage = "[Formulaire@DefaultResponseService::createMultiple] Failed to create multiple responses";
        TransactionHelper.executeTransactionAndGetJsonObjectResults(transactionElements, errorMessage)
                .onSuccess(result -> promise.complete(IModelHelper.toList(result, Response.class)))
                .onFailure(err -> promise.fail(err.getMessage()));

        return promise.future();
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

    public Future<Void> deleteByQuestionsAndDistribution(List<Long> questionIds, String distributionId) {
        Promise<Void> promise = Promise.promise();

        if (questionIds == null || questionIds.isEmpty()) {
            promise.complete();
            return promise.future();
        }

        String query = "DELETE FROM " + RESPONSE_TABLE + " " +
                "WHERE question_id IN " + Sql.listPrepared(questionIds) + " " +
                "AND distribution_id = ?;";
        JsonArray params = new JsonArray(questionIds).add(distributionId);

        String errorMessage = "[Formulaire@DefaultResponseService::deleteByQuestionAndDistribution] Failed to delete multiple responses for question ids " + questionIds;
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(either -> {
            if (either.isLeft()){
                log.error(errorMessage + " : " + either.left().getValue());
                promise.fail(either.left().getValue());
            }
            else {
                promise.complete();
            }
        }));

        return promise.future();
    }

    @Override
    public void deleteAllByDistribution(String distributionId, Handler<Either<String, JsonArray>> handler) {
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