package fr.openent.formulaire.service.impl;

import fr.openent.form.core.enums.QuestionTypes;
import fr.openent.form.core.models.Form;
import fr.openent.form.core.models.FormElement;
import fr.openent.form.core.models.Question;
import fr.openent.form.core.models.TransactionElement;
import fr.openent.form.helpers.IModelHelper;
import fr.openent.form.helpers.TransactionHelper;
import fr.openent.form.helpers.UtilsHelper;
import fr.openent.form.helpers.FutureHelper;
import fr.openent.formulaire.service.QuestionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Constants.*;
import static fr.openent.form.core.constants.DistributionStatus.FINISHED;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.*;
import static fr.openent.form.core.constants.Tables.QUESTION_TYPE;
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
    public Future<JsonArray> listForForm(String formId) {
        Promise<JsonArray> promise = Promise.promise();

        String errorMessage = "[Formulaire@DefaultQuestionService::listForForm] Fail to list questions for form with id " + formId + " : ";
        listForForm(formId, FutureHelper.handlerEither(promise, errorMessage));

        return promise.future();
    }

    @Override
    public void listForSection(String sectionId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + QUESTION_TABLE + " WHERE section_id = ? AND matrix_id IS NULL " +
                "ORDER BY section_position;";
        JsonArray params = new JsonArray().add(sectionId);
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public Future<JsonArray> listForFormAndSection(String formId) {
        Promise<JsonArray> promise = Promise.promise();

        String query = "SELECT * FROM " + QUESTION_TABLE + " WHERE form_id = ? AND matrix_id IS NULL " +
                "ORDER BY position, section_id, section_position;";
        JsonArray params = new JsonArray().add(formId);

        sql.prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(promise)));

        return promise.future();
    }

    public Future<List<Question>> listChildren(JsonArray questionIds) {
        Promise<List<Question>> promise = Promise.promise();

        if (questionIds == null || questionIds.isEmpty()) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        String query = "SELECT * FROM " + QUESTION_TABLE + " WHERE matrix_id IN " + Sql.listPrepared(questionIds);
        JsonArray params = new JsonArray().addAll(questionIds);

        String errMessage = "[Formulaire@DefaultQuestionService::listChildren] Failed to list children for questions with id " + questionIds + " : ";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, Question.class, errMessage)));

        return promise.future();
    }

    @Override
    public Future<JsonArray> listOldChildren(JsonArray questionIds) {
        Promise<JsonArray> promise = Promise.promise();

        if (questionIds == null || questionIds.isEmpty()) {
            promise.complete(new JsonArray());
            return promise.future();
        }

        String query = "SELECT * FROM " + QUESTION_TABLE + " WHERE matrix_id IN " + Sql.listPrepared(questionIds);
        JsonArray params = new JsonArray().addAll(questionIds);

        String errMessage = "[Formulaire@DefaultQuestionService::listChildren] Failed to list children for questions with id " + questionIds + " : ";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(promise, errMessage)));

        return promise.future();
    }

    @Override
    public void listChildren(JsonArray questionIds, Handler<Either<String, JsonArray>> handler) {
        listOldChildren(questionIds)
                .onSuccess(result -> handler.handle(new Either.Right<>(result)))
                .onFailure(err -> handler.handle(new Either.Left<>(err.getMessage())));
    }

    public Future<List<Question>> listByIds(List<Long> questionIds) {
        Promise<List<Question>> promise = Promise.promise();

        if (questionIds == null || questionIds.isEmpty()) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        String query = "SELECT * FROM " + QUESTION_TABLE + " WHERE id IN " + Sql.listPrepared(questionIds);
        JsonArray params = new JsonArray(questionIds);

        String errMessage = "[Formulaire@DefaultQuestionService::listByIds] Failed to list questions with ids " + questionIds;
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, Question.class, errMessage)));

        return promise.future();
    }

    @Override
    @Deprecated
    public void getExportInfos(String formId, boolean isPdf, Handler<Either<String, JsonArray>> handler) {
        this.getExportInfos(formId, isPdf)
            .onSuccess(res-> handler.handle(new Either.Right<>(res)))
            .onFailure(error -> handler.handle(new Either.Left<>(error.getMessage())));
    }

    @Override
    public Future<JsonArray> getExportInfos(String formId, boolean isPdf){
        Promise<JsonArray> promise = Promise.promise();
        String getElementPosition =
                "CASE " +
                    "WHEN q.position ISNULL AND s.position IS NOT NULL THEN s.position " +
                    "WHEN s.position ISNULL AND q.position IS NOT NULL THEN q.position " +
                    "WHEN s.position ISNULL AND q.position ISNULL THEN parent.position " +
                "END AS element_position";

        String getSectionPosition =
                "CASE " +
                    "WHEN parent.section_position ISNULL AND q.section_position IS NOT NULL THEN q.section_position " +
                    "WHEN q.section_position ISNULL AND parent.section_position IS NOT NULL THEN parent.section_position " +
                "END AS section_position";

        String query = "SELECT q.id, q.title, q.position, q.question_type, q.statement, q.mandatory, q.section_id, " +
                getElementPosition + ", " + getSectionPosition + ", q.matrix_position, q.conditional " +
                "FROM " + QUESTION_TABLE + " q " +
                "LEFT JOIN " + QUESTION_TABLE + " parent ON parent.id = q.matrix_id " +
                "LEFT JOIN " + SECTION_TABLE + " s ON q.section_id = s.id OR parent.section_id = s.id " +
                "WHERE q.form_id = ? " + (isPdf ? "AND q.matrix_id IS NULL " :
                    "AND q.question_type NOT IN " + Sql.listPrepared(QUESTIONS_WITHOUT_RESPONSES)) +
                "ORDER BY element_position, section_position, q.matrix_position, q.id;";
        JsonArray params = new JsonArray().add(formId);
        if (!isPdf) params.addAll(new JsonArray(QUESTIONS_WITHOUT_RESPONSES));
        sql.prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerEither(promise)));
        return promise.future();
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
    public Future<Optional<Question>> create(Question question, String formId) {
        Promise<Optional<Question>> promise = Promise.promise();

        String query = "INSERT INTO " + QUESTION_TABLE + " (form_id, title, position, question_type, statement, " +
                "mandatory, section_id, section_position, conditional, placeholder, matrix_id, matrix_position) VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";
        int questionType = question.getMatrixId() != null &&
                !MATRIX_CHILD_QUESTIONS.contains(question.getQuestionType()) ?
                QuestionTypes.SINGLEANSWERRADIO.getCode() :
                question.getQuestionType();
        boolean isConditional = CONDITIONAL_QUESTIONS.contains(question.getQuestionType()) && question.getConditional();

        JsonArray params = new JsonArray()
                .add(formId)
                .add(question.getTitle())
                .add(question.getSectionId() != null ? null : question.getPosition())
                .add(questionType)
                .add(question.getStatement())
                .add(question.getMandatory() || isConditional)
                .add(question.getSectionId())
                .add(question.getSectionPosition())
                .add(isConditional)
                .add(question.getPlaceholder())
                .add(question.getMatrixId())
                .add(question.getMatrixPosition());

        query += getUpdateDateModifFormRequest();
        params.addAll(getParamsForUpdateDateModifFormRequest(formId));

        String errorMessage = "[Formulaire@DefaultQuestionService::create] Fail to create question " + question + " : ";
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(IModelHelper.sqlUniqueResultToIModel(promise, Question.class, errorMessage)));

        return promise.future();
    }

    @Override
    public void update(String formId, JsonArray questions, Handler<Either<String, JsonArray>> handler) {
        if (!questions.isEmpty()) {
            SqlStatementsBuilder s = new SqlStatementsBuilder();
            String nullifyerQuery = "UPDATE " + QUESTION_TABLE + " SET position = NULL, section_id = NULL, section_position = NULL, " +
                    "matrix_id = NULL, matrix_position = NULL WHERE id IN " + Sql.listPrepared(questions) + ";";
            String query = "UPDATE " + QUESTION_TABLE + " SET title = ?, position = ?, question_type = ?, " +
                    "statement = ?, mandatory = ?, section_id = ?, section_position = ?, conditional = ?, placeholder = ?, " +
                    "matrix_id = ?, matrix_position = ? WHERE id = ? RETURNING *;";

            s.raw(TRANSACTION_BEGIN_QUERY);
            s.prepared(nullifyerQuery, UtilsHelper.getIds(questions, false));
            for (int i = 0; i < questions.size(); i++) {
                JsonObject question = questions.getJsonObject(i);
                int questionType = question.getInteger(MATRIX_ID, null) != null &&
                        !MATRIX_CHILD_QUESTIONS.contains(question.getInteger(QUESTION_TYPE, 1)) ?
                        QuestionTypes.SINGLEANSWERRADIO.getCode() :
                        question.getInteger(QUESTION_TYPE, 1);
                boolean isConditional = CONDITIONAL_QUESTIONS.contains(question.getInteger(QUESTION_TYPE)) && question.getBoolean(CONDITIONAL, false);
                JsonArray params = new JsonArray()
                        .add(question.getString(TITLE, ""))
                        .add(question.getInteger(SECTION_POSITION, null) != null ? null : question.getInteger(POSITION, null))
                        .add(questionType)
                        .add(question.getString(STATEMENT, ""))
                        .add(question.getBoolean(MANDATORY, false) || isConditional)
                        .add(question.getInteger(SECTION_ID, null))
                        .add(question.getInteger(SECTION_POSITION, null))
                        .add(isConditional)
                        .add(question.getString(PLACEHOLDER, ""))
                        .add(question.getInteger(MATRIX_ID, null))
                        .add(question.getInteger(MATRIX_POSITION, null))
                        .add(question.getInteger(ID, null));
                s.prepared(query, params);
            }

            s.prepared(getUpdateDateModifFormRequest(), getParamsForUpdateDateModifFormRequest(formId));
            s.raw(TRANSACTION_COMMIT_QUERY);

            sql.transaction(s.build(), SqlResult.validResultsHandler(handler));
        }
        else {
            handler.handle(new Either.Right<>(new JsonArray()));
        }
    }

    @Override
    public Future<List<Question>> update(String formId, List<Question> questions) {
        Promise<List<Question>> promise = Promise.promise();

        if (questions == null || questions.isEmpty()) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        String nullifyerQuery = "UPDATE " + QUESTION_TABLE + " SET position = NULL, section_id = NULL, section_position = NULL, " +
                "matrix_id = NULL, matrix_position = NULL WHERE id IN " + Sql.listPrepared(questions) + ";";
        String query = "UPDATE " + QUESTION_TABLE + " SET title = ?, position = ?, question_type = ?, " +
                "statement = ?, mandatory = ?, section_id = ?, section_position = ?, conditional = ?, placeholder = ?, " +
                "matrix_id = ?, matrix_position = ? WHERE id = ? RETURNING *;";

        List<TransactionElement> transactionElements = new ArrayList<>();

        JsonArray questionIds = questions.stream().map(FormElement::getId).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
        transactionElements.add(new TransactionElement(nullifyerQuery, questionIds));

        for (Question question : questions) {
            JsonArray params = new JsonArray()
                    .add(question.getTitle())
                    .add(question.getPosition())
                    .add(question.getQuestionType())
                    .add(question.getStatement())
                    .add(question.getMandatory())
                    .add(question.getSectionId())
                    .add(question.getSectionPosition())
                    .add(question.getConditional())
                    .add(question.getPlaceholder())
                    .add(question.getMatrixId())
                    .add(question.getMatrixPosition())
                    .add(question.getId());
            transactionElements.add(new TransactionElement(query, params));
        }
        transactionElements.add(new TransactionElement(getUpdateDateModifFormRequest(), getParamsForUpdateDateModifFormRequest(formId)));

        String errorMessage = "[Formulaire@DefaultQuestionService::update] Fail to update questions " + questions + " : ";
        TransactionHelper.executeTransactionAndGetJsonObjectResults(transactionElements, errorMessage)
            .onSuccess(result -> promise.complete(IModelHelper.toList(result, Question.class)))
            .onFailure(err -> promise.fail(err.getMessage()));

        return promise.future();
    }

    @Override
    public void delete(JsonObject question, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + QUESTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(question.getInteger(ID));

        query += getUpdateDateModifFormRequest();
        params.addAll(getParamsForUpdateDateModifFormRequest(question.getInteger(FORM_ID).toString()));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public Future<List<Question>> deleteFileQuestionsForForm(Number formId) {
        Promise<List<Question>> promise = Promise.promise();
        String query = "DELETE FROM " + QUESTION_TABLE + " WHERE form_id = ? AND question_type = ? RETURNING *;";
        JsonArray params = new JsonArray().add(formId).add(QuestionTypes.FILE.getCode());

        String errorMessage = "[Formulaire@DefaultQuestionService::deleteFileQuestionsForForm] Failed to delete questions file for form with id " + formId;
        sql.prepared(query, params, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, Question.class, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<Void> reorderQuestionsAfterDeletion(Number formId, List<Question> deletedQuestions) {
        Promise<Void> promise = Promise.promise();
        List<TransactionElement> transactionElements = new ArrayList<>();

        if (deletedQuestions == null || deletedQuestions.isEmpty()) {
            promise.complete();
            return promise.future();
        }

        // Reorder questions in sections
        List<Long> sectionIdToUpdate = deletedQuestions.stream()
                .map(Question::getSectionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        String querySection =
                "WITH ranking_position AS ( " +
                    "SELECT *, RANK() OVER (PARTITION BY form_id ORDER BY section_position) AS rank " +
                    "FROM " + QUESTION_TABLE + " WHERE section_id = ? " +
                "), " +
                "nullify_questions AS ( " +
                    "UPDATE " + QUESTION_TABLE + " SET section_position = NULL, section_id = NULL WHERE section_id = ? " +
                ") " +
                "UPDATE " + QUESTION_TABLE + " q " +
                "SET section_position = (SELECT rank FROM ranking_position WHERE q.id = ranking_position.id), section_id = ? " +
                "WHERE section_id = ? " +
                "RETURNING *";

        for (Long sectionId : sectionIdToUpdate) {
            JsonArray paramsSection = new JsonArray().add(sectionId).add(sectionId).add(sectionId).add(sectionId);
            transactionElements.add(new TransactionElement(querySection, paramsSection));
        }

        // Reorder form elements in form
        String queryForm =
                "WITH ranking_position AS ( " +
                    "SELECT *, RANK() OVER (PARTITION BY form_id ORDER BY position, id_section, id_question) AS rank " +
                    "FROM ( " +
                        "SELECT form_id, id AS id_section, null AS id_question, position FROM " + SECTION_TABLE +
                        " WHERE form_id = ? " +
                        "UNION " +
                        "SELECT form_id, null AS id_section, id AS id_question, position FROM " + QUESTION_TABLE +
                        " WHERE form_id = ? AND position IS NOT NULL " +
                    ") AS elements " +
                "), " +
                "nullify_sections AS ( " +
                    "UPDATE " + SECTION_TABLE + " SET position = NULL WHERE form_id = ? " +
                "), " +
                "nullify_questions AS ( " +
                    "UPDATE " + QUESTION_TABLE + " SET position = NULL WHERE form_id = ? AND position IS NOT NULL " +
                ")," +
                "updated_sections AS ( " +
                    "UPDATE " + SECTION_TABLE + " s " +
                    "SET position = ( " +
                        "SELECT rank FROM ranking_position " +
                        "WHERE ranking_position.id_section IS NOT NULL AND s.id = ranking_position.id_section " +
                    ") " +
                    "WHERE form_id = ? " +
                    "RETURNING id, form_id, title, position, true AS is_section " +
                "), " +
                "updated_questions AS ( " +
                    "UPDATE " + QUESTION_TABLE + " q " +
                    "SET position = ( " +
                        "SELECT rank FROM ranking_position " +
                        "WHERE ranking_position.id_question IS NOT NULL AND q.id = ranking_position.id_question " +
                    ") " +
                    "WHERE form_id = ? " +
                    "RETURNING id, form_id, title, position, false AS is_section " +
                ") " +
                "SELECT * FROM updated_sections " +
                "UNION " +
                "SELECT * FROM updated_questions " +
                "ORDER BY form_id, position, id";

        JsonArray paramsForm = new JsonArray().add(formId).add(formId).add(formId).add(formId).add(formId).add(formId);
        transactionElements.add(new TransactionElement(queryForm, paramsForm));

        String errorMessage = "[Formulaire@DefaultQuestionService::reorderQuestionsAfterDeletion] Fail to reorder questions and form elements of form with id " + formId;
        TransactionHelper.executeTransaction(transactionElements, errorMessage)
                .onSuccess(result -> promise.complete())
                .onFailure(err -> promise.fail(err.getMessage()));

        return promise.future();
    }
}
