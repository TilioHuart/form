package fr.openent.formulaire.service.impl;

import fr.openent.form.core.models.ResponseFile;
import fr.openent.form.helpers.IModelHelper;
import fr.openent.formulaire.service.ResponseFileService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Tables.*;

public class DefaultResponseFileService implements ResponseFileService {

    @Override
    public void list(String responseId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + RESPONSE_FILE_TABLE + " WHERE response_id = ?;";
        JsonArray params = new JsonArray().add(responseId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listByQuestion(String questionId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT rf.*, d.date_response, d.responder_name FROM " + RESPONSE_FILE_TABLE + " rf " +
                "INNER JOIN " + RESPONSE_TABLE + " r ON r.id = rf.response_id " +
                "INNER JOIN " + DISTRIBUTION_TABLE + " d ON d.id = r.distribution_id " +
                "WHERE r.question_id = ?" +
                "ORDER BY rf.response_id;";
        JsonArray params = new JsonArray().add(questionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listByForm(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT rf.id FROM " + RESPONSE_FILE_TABLE + " rf " +
                "INNER JOIN " + RESPONSE_TABLE + " r ON r.id = rf.response_id " +
                "INNER JOIN " + QUESTION_TABLE + " q ON q.id = r.question_id " +
                "WHERE q.form_id = ?;";
        JsonArray params = new JsonArray().add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String fileId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + RESPONSE_FILE_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(fileId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(String responseId, String fileId, String filename, String type, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + RESPONSE_FILE_TABLE + " (id, response_id, filename, type) VALUES (?, ?, ?, ?);";
        JsonArray params = new JsonArray().add(fileId).add(responseId).add(filename).add(type);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Deprecated
    @Override
    public void deleteAllByResponse(JsonArray responseIds, Handler<Either<String, JsonArray>> handler) {
        List<String> responseIdsList = responseIds.stream().map(String.class::cast).collect(Collectors.toList());
        deleteAllByResponse(responseIdsList)
            .onSuccess(result -> handler.handle(new Either.Right<>(new JsonArray(result))))
            .onFailure(err -> handler.handle(new Either.Left<>(err.getMessage())));
    }

    @Override
    public Future<List<ResponseFile>> deleteAllByResponse(List<String> responseIds) {
        Promise<List<ResponseFile>> promise = Promise.promise();

        if (responseIds == null || responseIds.isEmpty()) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        String query = "DELETE FROM " + RESPONSE_FILE_TABLE + " " +
                "WHERE response_id IN " + Sql.listPrepared(responseIds) + " " +
                "RETURNING *;";
        JsonArray params = new JsonArray(responseIds);

        String errorMessage = "[Formulaire@DefaultResponseFileService::deleteAllByResponse] Fail to delete response files " +
                "for responses " + responseIds + " : ";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler((IModelHelper.sqlResultToIModel(promise, ResponseFile.class, errorMessage))));

        return promise.future();
    }

    @Override
    public void deleteAll(JsonArray fileIds, Handler<Either<String, JsonArray>> handler) {
        String query = "DELETE FROM " + RESPONSE_FILE_TABLE +
                " WHERE id IN " + Sql.listPrepared(fileIds) + " RETURNING id;";
        JsonArray params = new JsonArray().addAll(fileIds);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
