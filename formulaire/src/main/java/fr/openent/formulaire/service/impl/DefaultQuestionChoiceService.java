package fr.openent.formulaire.service.impl;

import fr.openent.form.core.enums.FormElementTypes;
import fr.openent.form.core.enums.I18nKeys;
import fr.openent.form.core.models.QuestionChoice;
import fr.openent.form.helpers.FutureHelper;
import fr.openent.form.helpers.I18nHelper;
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

import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.*;

public class DefaultQuestionChoiceService implements QuestionChoiceService {
    private static final Logger log = LoggerFactory.getLogger(DefaultQuestionChoiceService.class);

    @Override
    public void list(String questionId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + QUESTION_CHOICE_TABLE + " WHERE question_id = ? ORDER BY id;";
        JsonArray params = new JsonArray().add(questionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listChoices(JsonArray questionIds, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + QUESTION_CHOICE_TABLE + " WHERE question_id IN " + Sql.listPrepared(questionIds);
        JsonArray params = new JsonArray().addAll(questionIds);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
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
                "next_form_element_id, next_form_element_type, is_custom) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING *;";
        boolean isCustom = choice.getIsCustom();
        JsonArray params = new JsonArray()
                .add(questionId)
                .add(isCustom ? I18nHelper.getI18nValue(I18nKeys.OTHER, locale) : choice.getValue())
                .add(choice.getPosition())
                .add(choice.getType())
                .add(choice.getNextFormElementId())
                .add(choice.getNextFormElementType())
                .add(isCustom);

        String errorMessage = "[Formulaire@DefaultQuestionChoiceService::create] Fail to create question choice : ";
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerEither(promise, errorMessage)));

        return promise.future();
    }

    @Override
    public void duplicate(int formId, int questionId, int originalQuestionId, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + QUESTION_CHOICE_TABLE + " " +
                "(question_id, value, position, type, is_custom, next_form_element_id, next_form_element_type) " +
                "SELECT ?, value, position, type, is_custom, " +
                "(SELECT id FROM " + SECTION_TABLE + " WHERE original_section_id = qc.next_form_element_id AND form_id = ?) " +
                "FROM " + QUESTION_CHOICE_TABLE + " qc " +
                "WHERE question_id = ? ORDER BY qc.id;";
        JsonArray params = new JsonArray().add(questionId).add(formId).add(originalQuestionId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public Future<JsonObject> update(QuestionChoice choice, String locale) {
        Promise<JsonObject> promise = Promise.promise();

        String query = "UPDATE " + QUESTION_CHOICE_TABLE + " SET value = ?, position = ?, type = ?, " +
                "next_form_element_id = ?, next_form_element_type = ?, is_custom = ? " +
                "WHERE id = ? RETURNING *;";
        boolean isCustom = choice.getIsCustom();
        JsonArray params = new JsonArray()
                .add(isCustom ? I18nHelper.getI18nValue(I18nKeys.OTHER, locale) : choice.getValue())
                .add(choice.getPosition())
                .add(choice.getType())
                .add(choice.getNextFormElementId())
                .add(choice.getNextFormElementType())
                .add(isCustom)
                .add(choice.getId());

        String errorMessage = "[Formulaire@DefaultQuestionChoiceService::update] Fail to update question choice : ";
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerEither(promise, errorMessage)));

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
    public Future<Boolean> isChoiceTargetValid(QuestionChoice choice) {
        Promise<Boolean> promise = Promise.promise();

        if (choice.getNextFormElementId() == null && choice.getNextFormElementType() == null) {
            promise.complete(Boolean.TRUE);
            return promise.future();
        }
        else if (choice.getNextFormElementId() == null ^ choice.getNextFormElementType() == null) {
            String errorMessage = "[Formulaire@DefaultQuestionChoiceService::isChoiceValid] Choice next_form_element_id " +
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
                "AND  id = ?;";
        JsonArray params = new JsonArray().add(questionId).add(questionId).add(targetedId);

        String errorMessage = "[Formulaire@DefaultQuestionChoiceService::isChoiceValid] Fail to check if choice target is valid : ";
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
