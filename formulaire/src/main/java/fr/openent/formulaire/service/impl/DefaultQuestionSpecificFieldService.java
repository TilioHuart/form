package fr.openent.formulaire.service.impl;

import fr.openent.form.core.constants.Fields;
import fr.openent.form.core.enums.QuestionTypes;
import fr.openent.formulaire.service.QuestionSpecificFieldService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;

import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.*;

public class DefaultQuestionSpecificFieldService implements QuestionSpecificFieldService {
    private final Sql sql = Sql.getInstance();


    @Override
    public void listByIds(JsonArray questionIds, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + QUESTION_SPECIFIC_FIELDS + " WHERE question_id IN " + Sql.listPrepared(questionIds);
        JsonArray params = new JsonArray().addAll(questionIds);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject question, String questionId, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + QUESTION_SPECIFIC_FIELDS + " (question_id, cursor_min_val, cursor_max_val, cursor_step, " +
                "cursor_label_min_val, cursor_label_max_val) VALUES (?, ?, ?, ?, ?, ?) RETURNING *;";

        boolean isCursor = question.getInteger(Fields.QUESTION_TYPE) == QuestionTypes.CURSOR.getCode();
        JsonArray params = new JsonArray()
                .add(questionId)
                .add(question.getInteger(CURSOR_MIN_VAL, isCursor ? 1 : null))
                .add(question.getInteger(CURSOR_MAX_VAL, isCursor ? 10 : null))
                .add(question.getInteger(CURSOR_STEP, isCursor ? 1 : null))
                .add(question.getString(CURSOR_LABEL_MIN_VAL, ""))
                .add(question.getString(CURSOR_LABEL_MAX_VAL, ""));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void update(JsonArray questions, Handler<Either<String, JsonArray>> handler) {
        if (!questions.isEmpty()) {
            SqlStatementsBuilder s = new SqlStatementsBuilder();
            String query = "UPDATE " + QUESTION_SPECIFIC_FIELDS + " SET cursor_min_val = ?, cursor_max_val = ?, cursor_step = ?, " +
                    "cursor_label_min_val = ?, cursor_label_max_val = ? WHERE question_id = ? RETURNING *;";

            s.raw("BEGIN;");
            for (int i = 0; i < questions.size(); i++) {
                JsonObject question = questions.getJsonObject(i);
                boolean isCursor = question.getInteger(Fields.QUESTION_TYPE) == QuestionTypes.CURSOR.getCode();
                JsonArray params = new JsonArray()
                        .add(question.getInteger(CURSOR_MIN_VAL, isCursor ? 1 : null))
                        .add(question.getInteger(CURSOR_MAX_VAL, isCursor ? 1 : null))
                        .add(question.getInteger(CURSOR_STEP, isCursor ? 1 : null))
                        .add(question.getString(CURSOR_LABEL_MIN_VAL, ""))
                        .add(question.getString(CURSOR_LABEL_MAX_VAL, ""))
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
}