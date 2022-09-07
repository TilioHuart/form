package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.service.QuestionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;

import static fr.openent.form.core.constants.Constants.CONDITIONAL_QUESTIONS;
import static fr.openent.form.core.constants.Constants.QUESTIONS_WITHOUT_RESPONSES;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.QUESTION_TABLE;
import static fr.openent.form.core.constants.Tables.SECTION_TABLE;
import static fr.openent.form.helpers.SqlHelper.getParamsForUpdateDateModifFormRequest;
import static fr.openent.form.helpers.SqlHelper.getUpdateDateModifFormRequest;

public class DefaultQuestionService implements QuestionService {
    private final Sql sql = Sql.getInstance();

    @Override
    public void listForForm(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + QUESTION_TABLE + " WHERE form_id = ? AND section_id IS NULL AND matrix_id IS NULL " +
                "ORDER BY position;";
        JsonArray params = new JsonArray().add(formId);
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listForSection(String sectionId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + QUESTION_TABLE + " WHERE section_id = ? AND matrix_id IS NULL " +
                "ORDER BY section_position;";
        JsonArray params = new JsonArray().add(sectionId);
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listForFormAndSection(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + QUESTION_TABLE + " WHERE form_id = ? AND matrix_id IS NULL " +
                "ORDER BY position, section_id, section_position;";
        JsonArray params = new JsonArray().add(formId);
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listChildren(JsonArray questionIds, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + QUESTION_TABLE + " WHERE matrix_id IN " + Sql.listPrepared(questionIds);
        JsonArray params = new JsonArray().addAll(questionIds);
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void export(String formId, boolean isPdf, Handler<Either<String, JsonArray>> handler) {
        String getElementPosition = "CASE " +
                "WHEN q.position ISNULL AND s.position IS NOT NULL THEN s.position " +
                "WHEN s.position ISNULL AND q.position IS NOT NULL THEN q.position " +
                "WHEN q.position ISNULL AND s.position ISNULL THEN parent.position " +
                "END";
        String getMatrixPosition = "CASE WHEN q.matrix_id IS NOT NULL THEN RANK() OVER (PARTITION BY q.matrix_id ORDER BY q.id) END";

        String query = "SELECT q.*, " + getElementPosition + " AS element_position, " + getMatrixPosition + " AS matrix_position " +
                "FROM " + QUESTION_TABLE + " q " +
                "LEFT JOIN " + QUESTION_TABLE + " parent ON parent.id = q.matrix_id " +
                "LEFT JOIN " + SECTION_TABLE + " s ON q.section_id = s.id " +
                "WHERE q.form_id = ? " + (isPdf ? "AND q.matrix_id IS NULL " :
                    "AND q.question_type NOT IN " + Sql.listPrepared(QUESTIONS_WITHOUT_RESPONSES)) +
                "ORDER BY element_position, q.section_position, q.id;";
        JsonArray params = new JsonArray().add(formId);
        if (!isPdf) params.addAll(new JsonArray(QUESTIONS_WITHOUT_RESPONSES));
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String questionId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + QUESTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(questionId);
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getSectionIdsWithConditionalQuestions(String formId, JsonArray questionIds, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT section_id FROM " + QUESTION_TABLE + " WHERE form_id = ? AND conditional = ? " +
                "AND section_id IS NOT NULL ";
        JsonArray params = new JsonArray().add(formId).add(true);

        if (questionIds.size() > 0) {
            query += "AND id NOT IN " + Sql.listPrepared(questionIds);
            params.addAll(questionIds);
        }

        query += ";";

        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getSectionIdsByForm(String questionId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + SECTION_TABLE +
                " WHERE form_id = (SELECT form_id FROM " + QUESTION_TABLE + " WHERE id = ?);";
        JsonArray params = new JsonArray().add(questionId);
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getFormPosition(String questionId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT DISTINCT (SELECT MAX(pos) as position FROM (VALUES (q.position), (s.position)) AS value(pos)) " +
                "FROM " + QUESTION_TABLE + " q " +
                "LEFT JOIN " + SECTION_TABLE + " s ON s.id = q.section_id " +
                "WHERE q.id = ?;";
        JsonArray params = new JsonArray().add(questionId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(JsonObject question, String formId, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + QUESTION_TABLE + " (form_id, title, position, question_type, statement, " +
                "mandatory, section_id, section_position, conditional, placeholder, matrix_id) VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";

        boolean isConditional = CONDITIONAL_QUESTIONS.contains(question.getInteger(QUESTION_TYPE)) && question.getBoolean(CONDITIONAL, false);
        JsonArray params = new JsonArray()
                .add(formId)
                .add(question.getString(TITLE, ""))
                .add(question.getInteger(SECTION_POSITION, null) != null ? null : question.getInteger(POSITION, null))
                .add(question.getInteger(QUESTION_TYPE, 1))
                .add(question.getString(STATEMENT, ""))
                .add(question.getBoolean(MANDATORY, false) || isConditional)
                .add(question.getInteger(SECTION_ID, null))
                .add(question.getInteger(SECTION_POSITION, null))
                .add(isConditional)
                .add(question.getString(PLACEHOLDER, ""))
                .add(question.getInteger(MATRIX_ID, null));

        query += getUpdateDateModifFormRequest();
        params.addAll(getParamsForUpdateDateModifFormRequest(formId));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void update(String formId, JsonArray questions, Handler<Either<String, JsonArray>> handler) {
        if (!questions.isEmpty()) {
            SqlStatementsBuilder s = new SqlStatementsBuilder();
            String query = "UPDATE " + QUESTION_TABLE + " SET title = ?, position = ?, question_type = ?, " +
                    "statement = ?, mandatory = ?, section_id = ?, section_position = ?, conditional = ?, placeholder = ?, " +
                    "matrix_id = ? WHERE id = ? RETURNING *;";

            s.raw("BEGIN;");
            for (int i = 0; i < questions.size(); i++) {
                JsonObject question = questions.getJsonObject(i);
                JsonArray params = new JsonArray()
                        .add(question.getString(TITLE, ""))
                        .add(question.getInteger(SECTION_POSITION, null) != null ? null : question.getInteger(POSITION, null))
                        .add(question.getInteger(QUESTION_TYPE, 1))
                        .add(question.getString(STATEMENT, ""))
                        .add(question.getBoolean(CONDITIONAL, false) || question.getBoolean(MANDATORY, false))
                        .add(question.getInteger(SECTION_ID, null))
                        .add(question.getInteger(SECTION_POSITION, null))
                        .add(question.getBoolean(CONDITIONAL, false))
                        .add(question.getInteger(MATRIX_ID, null))
                        .add(question.getString(PLACEHOLDER, ""))
                        .add(question.getInteger(ID, null));
                s.prepared(query, params);
            }

            s.prepared(getUpdateDateModifFormRequest(), getParamsForUpdateDateModifFormRequest(formId));
            s.raw("COMMIT;");

            sql.transaction(s.build(), SqlResult.validResultsHandler(handler));
        }
        else {
            handler.handle(new Either.Right<>(new JsonArray()));
        }
    }

    @Override
    public void delete(JsonObject question, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + QUESTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(question.getInteger(ID));

        query += getUpdateDateModifFormRequest();
        params.addAll(getParamsForUpdateDateModifFormRequest(question.getInteger(FORM_ID).toString()));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
