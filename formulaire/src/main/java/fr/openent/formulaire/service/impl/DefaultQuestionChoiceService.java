package fr.openent.formulaire.service.impl;

import fr.openent.form.core.enums.FormElementTypes;
import fr.openent.form.core.enums.I18nKeys;
import fr.openent.form.core.models.FormElement;
import fr.openent.form.core.models.Question;
import fr.openent.form.core.models.QuestionChoice;
import fr.openent.form.core.models.TransactionElement;
import fr.openent.form.helpers.FutureHelper;
import fr.openent.form.helpers.I18nHelper;
import fr.openent.form.helpers.IModelHelper;
import fr.openent.form.helpers.TransactionHelper;
import fr.openent.formulaire.service.QuestionChoiceService;
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
import org.entcore.common.sql.SqlStatementsBuilder;

import java.util.ArrayList;
import java.util.List;

import static fr.openent.form.core.constants.Constants.TRANSACTION_BEGIN_QUERY;
import static fr.openent.form.core.constants.Constants.TRANSACTION_COMMIT_QUERY;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.*;
import static fr.openent.form.helpers.SqlHelper.getParamsForUpdateDateModifFormRequest;
import static fr.openent.form.helpers.SqlHelper.getUpdateDateModifFormRequest;

public class DefaultQuestionChoiceService implements QuestionChoiceService {
    private final Sql sql = Sql.getInstance();
    private static final Logger log = LoggerFactory.getLogger(DefaultQuestionChoiceService.class);

    @Override
    public void list(String questionId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + QUESTION_CHOICE_TABLE + " WHERE question_id = ? ORDER BY id;";
        JsonArray params = new JsonArray().add(questionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public Future<List<QuestionChoice>> listChoices(JsonArray questionIds) {
        Promise<List<QuestionChoice>> promise = Promise.promise();

        if (questionIds == null || questionIds.isEmpty()) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        String query = "SELECT * FROM " + QUESTION_CHOICE_TABLE + " WHERE question_id IN " + Sql.listPrepared(questionIds);
        JsonArray params = new JsonArray().addAll(questionIds);

        String errMessage = "[Formulaire@DefaultQuestionChoiceService::listChoices] Failed to list choices for questions with id " + questionIds + " : ";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, QuestionChoice.class, errMessage)));

        return promise.future();
    }

    @Override
    public void listChoices(JsonArray questionIds, Handler<Either<String, JsonArray>> handler) {
        listChoices(questionIds)
            .onSuccess(choices -> handler.handle(new Either.Right<>(IModelHelper.toJsonArray(choices))))
            .onFailure(err -> handler.handle(new Either.Left<>(err.getMessage())));
    }

    @Override
    public void get(String choiceId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + QUESTION_CHOICE_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(choiceId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public Future<JsonObject> create(String questionId, QuestionChoice choice, String locale) {
        Promise<JsonObject> promise = Promise.promise();

        String query = "INSERT INTO " + QUESTION_CHOICE_TABLE + " (question_id, value, position, type, " +
                "next_form_element_id, next_form_element_type, is_next_form_element_default, is_custom, image) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";
        boolean isCustom = choice.getIsCustom();
        JsonArray params = new JsonArray()
                .add(questionId)
                .add(isCustom ? I18nHelper.getI18nValue(I18nKeys.OTHER, locale) : choice.getValue())
                .add(choice.getPosition())
                .add(choice.getType())
                .add(choice.getNextFormElementId())
                .add(choice.getNextFormElementType())
                .add(choice.getIsNextFormElementDefault())
                .add(isCustom)
                .add(choice.getImage());

        String errorMessage = "[Formulaire@DefaultQuestionChoiceService::create] Fail to create question choice : ";
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerEither(promise, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<List<QuestionChoice>> create(List<QuestionChoice> choices, String locale) {
        Promise<List<QuestionChoice>> promise = Promise.promise();

        List<TransactionElement> transactionElements = new ArrayList<>();

        String query = "INSERT INTO " + QUESTION_CHOICE_TABLE + " (question_id, value, position, type, " +
                "next_form_element_id, next_form_element_type, is_next_form_element_default, is_custom, image) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";

        for (QuestionChoice choice : choices) {
            boolean isCustom = choice.getIsCustom();
            JsonArray params = new JsonArray()
                    .add(choice.getQuestionId())
                    .add(isCustom ? I18nHelper.getI18nValue(I18nKeys.OTHER, locale) : choice.getValue())
                    .add(choice.getPosition())
                    .add(choice.getType())
                    .add(choice.getNextFormElementId())
                    .add(choice.getNextFormElementType())
                    .add(choice.getIsNextFormElementDefault())
                    .add(isCustom)
                    .add(choice.getImage());
            transactionElements.add(new TransactionElement(query, params));
        }

        String errorMessage = "[Formulaire@DefaultQuestionChoiceService::create] Fail to create question choices : ";
        TransactionHelper.executeTransactionAndGetJsonObjectResults(transactionElements, errorMessage)
            .onSuccess(result -> promise.complete(IModelHelper.toList(result, QuestionChoice.class)))
            .onFailure(err -> promise.fail(err.getMessage()));

        return promise.future();
    }

    @Override
    public void duplicate(int formId, int questionId, int originalQuestionId, Handler<Either<String, JsonObject>> handler) {
        String query = "" +
                "WITH form_elements_infos AS (" +
                    "SELECT * FROM (" +
                        "SELECT id, form_id, 'QUESTION' AS type, original_question_id AS original_id FROM " + QUESTION_TABLE +
                        " UNION " +
                        "SELECT id, form_id, 'SECTION' AS type, original_section_id AS original_id FROM " + SECTION_TABLE +
                    ") AS qs_infos " +
                    "WHERE form_id = ? " +
                ")" +
                "INSERT INTO " + QUESTION_CHOICE_TABLE + " (question_id, value, position, type, is_custom, next_form_element_id, " +
                    "next_form_element_type, is_next_form_element_default, image) " +
                "SELECT ?, value, position, type, is_custom, " +
                    "(SELECT id FROM form_elements_infos WHERE original_id = qc.next_form_element_id AND type = qc.next_form_element_type), " +
                    "next_form_element_type, is_next_form_element_default, image " +
                "FROM " + QUESTION_CHOICE_TABLE + " qc " +
                "WHERE question_id = ? ORDER BY qc.id;";
        JsonArray params = new JsonArray().add(formId).add(questionId).add(originalQuestionId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public Future<JsonObject> update(QuestionChoice choice, String locale) {
        Promise<JsonObject> promise = Promise.promise();

        String query = "UPDATE " + QUESTION_CHOICE_TABLE + " SET value = ?, position = ?, type = ?, " +
                "next_form_element_id = ?, next_form_element_type = ?, is_next_form_element_default = ?, is_custom = ?, " +
                "image = ? WHERE id = ? RETURNING *;";
        boolean isCustom = choice.getIsCustom();
        JsonArray params = new JsonArray()
                .add(isCustom ? I18nHelper.getI18nValue(I18nKeys.OTHER, locale) : choice.getValue())
                .add(choice.getPosition())
                .add(choice.getType())
                .add(choice.getNextFormElementId())
                .add(choice.getNextFormElementType())
                .add(choice.getIsNextFormElementDefault())
                .add(isCustom)
                .add(choice.getImage())
                .add(choice.getId());

        String errorMessage = "[Formulaire@DefaultQuestionChoiceService::update] Fail to update question choice : ";
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerEither(promise, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<JsonArray> updateOld(List<QuestionChoice> choices, String locale) {
        Promise<JsonArray> promise = Promise.promise();

        if (!choices.isEmpty()) {
            SqlStatementsBuilder s = new SqlStatementsBuilder();
            String query = "UPDATE " + QUESTION_CHOICE_TABLE + " SET value = ?, position = ?, type = ?, " +
                    "next_form_element_id = ?, next_form_element_type = ?, is_next_form_element_default = ?, is_custom = ?, " +
                    "image = ? WHERE id = ? RETURNING *;";

            s.raw(TRANSACTION_BEGIN_QUERY);
            for (QuestionChoice choice : choices) {
                boolean isCustom = choice.getIsCustom();
                JsonArray params = new JsonArray()
                        .add(isCustom ? I18nHelper.getI18nValue(I18nKeys.OTHER, locale) : choice.getValue())
                        .add(choice.getPosition())
                        .add(choice.getType())
                        .add(choice.getNextFormElementId())
                        .add(choice.getNextFormElementType())
                        .add(choice.getIsNextFormElementDefault())
                        .add(isCustom)
                        .add(choice.getImage())
                        .add(choice.getId());
                s.prepared(query, params);
            }
            s.raw(TRANSACTION_COMMIT_QUERY);

            String errorMessage = "[Formulaire@DefaultQuestionChoiceService::update] Fail to update question choice : ";
            sql.transaction(s.build(), SqlResult.validResultsHandler(FutureHelper.handlerEither(promise, errorMessage)));
        }
        else {
            promise.complete(new JsonArray());
        }

        return promise.future();
    }

    @Override
    public Future<List<QuestionChoice>> update(List<QuestionChoice> choices, String locale) {
        Promise<List<QuestionChoice>> promise = Promise.promise();

        if (!choices.isEmpty()) {
            SqlStatementsBuilder s = new SqlStatementsBuilder();
            String query = "UPDATE " + QUESTION_CHOICE_TABLE + " SET value = ?, position = ?, type = ?, " +
                    "next_form_element_id = ?, next_form_element_type = ?, is_next_form_element_default = ?, is_custom = ?, " +
                    "image = ? WHERE id = ? RETURNING *;";

            s.raw(TRANSACTION_BEGIN_QUERY);
            for (QuestionChoice choice : choices) {
                boolean isCustom = choice.getIsCustom();
                JsonArray params = new JsonArray()
                        .add(isCustom ? I18nHelper.getI18nValue(I18nKeys.OTHER, locale) : choice.getValue())
                        .add(choice.getPosition())
                        .add(choice.getType())
                        .add(choice.getNextFormElementId())
                        .add(choice.getNextFormElementType())
                        .add(choice.getIsNextFormElementDefault())
                        .add(isCustom)
                        .add(choice.getImage())
                        .add(choice.getId());
                s.prepared(query, params);
            }
            s.raw(TRANSACTION_COMMIT_QUERY);

            String errorMessage = "[Formulaire@DefaultQuestionChoiceService::update] Fail to update question choice : ";
            sql.transaction(s.build(), SqlResult.validResultsHandler(IModelHelper.sqlResultToIModel(promise, QuestionChoice.class, errorMessage)));
        }
        else {
            promise.complete(new ArrayList<>());
        }

        return promise.future();
    }

    @Override
    public Future<JsonObject> delete(String choiceId) {
        Promise<JsonObject> promise = Promise.promise();

        String query = "DELETE FROM " + QUESTION_CHOICE_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(choiceId);

        String errorMessage = "[Formulaire@DefaultQuestionChoiceService::delete] Fail to delete question choice : ";
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerEither(promise, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<Boolean> isTargetValid(QuestionChoice choice) {
        Promise<Boolean> promise = Promise.promise();

        if (choice.getNextFormElementId() == null && choice.getNextFormElementType() == null) {
            promise.complete(Boolean.TRUE);
            return promise.future();
        }
        else if (choice.getNextFormElementId() == null ^ choice.getNextFormElementType() == null) {
            String errorMessage = "[Formulaire@DefaultQuestionChoiceService::isTargetValid] Choice next_form_element_id " +
                    "and next_form_element_type must be both null or both not null.";
            log.error(errorMessage);
            promise.fail(errorMessage);
            return promise.future();
        }

        Long questionId = choice.getQuestionId();
        Long targetedId = choice.getNextFormElementId();
        String targetedTable = choice.getNextFormElementType() == FormElementTypes.QUESTION ? QUESTION_TABLE : SECTION_TABLE;

        String query =
                "WITH question AS (SELECT * FROM " + QUESTION_TABLE + " WHERE id = ?), " +
                "element_position AS (SELECT CASE " +
                    "WHEN (SELECT section_id FROM question) IS NULL THEN (SELECT position FROM question) " +
                    "ELSE (SELECT position FROM " + SECTION_TABLE + " WHERE id = (SELECT section_id FROM question))" +
                "END) " +
                "SELECT COUNT(*) = 1 AS count FROM (SELECT id, form_id, position FROM " + targetedTable + ") AS targets_infos " +
                "WHERE form_id = (SELECT form_id FROM " + QUESTION_TABLE + " WHERE id = ?) " +
                "AND position IS NOT NULL " +
                "AND position > (SELECT position FROM element_position) " +
                "AND id = ?;";
        JsonArray params = new JsonArray().add(questionId).add(questionId).add(targetedId);

        String errorMessage = "[Formulaire@DefaultQuestionChoiceService::isTargetValid] Fail to check if choice target is valid : ";
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue().getBoolean(COUNT));
                return;
            }
            log.error(errorMessage + event.left().getValue());
            promise.fail(event.left().getValue());
        }));

        return promise.future();
    }
}
