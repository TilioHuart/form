package fr.openent.formulaire.service.impl;

import fr.openent.form.core.constants.Fields;
import fr.openent.form.core.enums.QuestionTypes;
import fr.openent.form.helpers.FutureHelper;
import fr.openent.form.helpers.UtilsHelper;
import fr.openent.formulaire.service.QuestionSpecificFieldsService;
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

import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.*;
import static fr.openent.form.helpers.UtilsHelper.getIds;

public class DefaultQuestionSpecificFieldsService implements QuestionSpecificFieldsService {
    private final Sql sql = Sql.getInstance();

    private static final Logger log = LoggerFactory.getLogger(DefaultQuestionSpecificFieldsService.class);

    public Future<JsonArray> syncQuestionSpecs(JsonArray questions) {
        Promise<JsonArray> promise = Promise.promise();

        JsonArray questionIds = getIds(questions);
        if (!questions.isEmpty()) {
            listByIds(questionIds)
                    .onSuccess(specEvt -> {
                        promise.complete(UtilsHelper.mergeQuestionsAndSpecifics(questions, specEvt));
                    })
                    .onFailure(error -> {
                        String message = String.format("[Formulaire@%s::syncQuestionSpecs] An error has occured" +
                                " when getting specific field: %s", this.getClass().getSimpleName(), error.getMessage());
                        log.error(message, error.getMessage());
                        promise.fail(error.getMessage());
                    });
        } else promise.complete(new JsonArray());
        return promise.future();
    }

    @Override
    public Future<JsonArray> listByIds(JsonArray questionIds) {
        Promise<JsonArray> promise = Promise.promise();

        String query = "SELECT * FROM " + QUESTION_SPECIFIC_FIELDS_TABLE + " WHERE question_id IN " + Sql.listPrepared(questionIds);
        JsonArray params = new JsonArray().addAll(questionIds);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(promise)));

        return promise.future();
    }

    @Override
    public void create(JsonObject question, String questionId, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + QUESTION_SPECIFIC_FIELDS_TABLE + " (question_id, cursor_min_val, cursor_max_val, cursor_step, " +
                "cursor_min_label, cursor_max_label) VALUES (?, ?, ?, ?, ?, ?) RETURNING *;";

        boolean isCursor = question.getInteger(Fields.QUESTION_TYPE) == QuestionTypes.CURSOR.getCode();
        JsonArray params = new JsonArray()
                .add(questionId)
                .add(question.getInteger(CURSOR_MIN_VAL, isCursor ? 1 : null))
                .add(question.getInteger(CURSOR_MAX_VAL, isCursor ? 10 : null))
                .add(question.getInteger(CURSOR_STEP, isCursor ? 1 : null))
                .add(question.getString(CURSOR_MIN_LABEL, ""))
                .add(question.getString(CURSOR_MAX_LABEL, ""));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public Future<JsonObject> create(JsonObject question, String questionId) {
        Promise<JsonObject> promise = Promise.promise();

        String errorMessage = "[Formulaire@DefaultQuestionSpecificFieldsService::create] Fail to create question specifics for question " + question + " : ";
        create(question, questionId, FutureHelper.handlerEither(promise, errorMessage));

        return promise.future();
    }

    public void update(JsonArray questions, Handler<Either<String, JsonArray>> handler) {
        if (!questions.isEmpty()) {
            SqlStatementsBuilder s = new SqlStatementsBuilder();
            String query = "UPDATE " + QUESTION_SPECIFIC_FIELDS_TABLE + " SET cursor_min_val = ?, cursor_max_val = ?, cursor_step = ?, " +
                    "cursor_min_label = ?, cursor_max_label = ? WHERE question_id = ? RETURNING *;";

            s.raw("BEGIN;");
            for (int i = 0; i < questions.size(); i++) {
                JsonObject question = questions.getJsonObject(i);
                boolean isCursor = question.getInteger(Fields.QUESTION_TYPE) == QuestionTypes.CURSOR.getCode();
                JsonArray params = new JsonArray()
                        .add(question.getInteger(CURSOR_MIN_VAL, isCursor ? 1 : null))
                        .add(question.getInteger(CURSOR_MAX_VAL, isCursor ? 1 : null))
                        .add(question.getInteger(CURSOR_STEP, isCursor ? 1 : null))
                        .add(question.getString(CURSOR_MIN_LABEL, ""))
                        .add(question.getString(CURSOR_MAX_LABEL, ""))
                        .add(question.getInteger(ID, null));
                s.prepared(query, params);
            }
            s.raw("COMMIT;");

            sql.transaction(s.build(), SqlResult.validResultHandler(handler));
        }
        else {
            handler.handle(new Either.Right<>(new JsonArray()));
        }
    }

    @Override
    public Future<JsonArray> update(JsonArray questions) {
        Promise<JsonArray> promise = Promise.promise();

        String errorMessage = "[Formulaire@DefaultQuestionSpecificFieldsService::update] Fail to update questions specifics for questions " + questions + " : ";
        update(questions, FutureHelper.handlerEither(promise, errorMessage));

        return promise.future();
    }
}