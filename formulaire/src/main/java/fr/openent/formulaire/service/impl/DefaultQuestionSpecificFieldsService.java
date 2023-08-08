package fr.openent.formulaire.service.impl;

import fr.openent.form.core.enums.QuestionTypes;
import fr.openent.form.core.models.FormElement;
import fr.openent.form.core.models.Question;
import fr.openent.form.core.models.QuestionSpecificFields;
import fr.openent.form.core.models.TransactionElement;
import fr.openent.form.helpers.IModelHelper;
import fr.openent.form.helpers.TransactionHelper;
import fr.openent.form.helpers.UtilsHelper;
import fr.openent.formulaire.service.QuestionSpecificFieldsService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Tables.*;

public class DefaultQuestionSpecificFieldsService implements QuestionSpecificFieldsService {
    private final Sql sql = Sql.getInstance();

    private static final Logger log = LoggerFactory.getLogger(DefaultQuestionSpecificFieldsService.class);

    public Future<JsonArray> syncQuestionSpecs(JsonArray jsonQuestions) {
        Promise<JsonArray> promise = Promise.promise();

        if (jsonQuestions == null || jsonQuestions.isEmpty()) {
            promise.complete(new JsonArray());
            return promise.future();
        }

        List<Question> questions = IModelHelper.toList(jsonQuestions, Question.class);
        List<Long> questionIds = questions.stream().map(FormElement::getId).collect(Collectors.toList());

        listByIds(questionIds)
            .onSuccess(specifics -> {
                List<Question> updatedQuestionsAndSpecifics = UtilsHelper.mergeQuestionsAndSpecifics(questions, specifics);
                promise.complete(new JsonArray(updatedQuestionsAndSpecifics));
            })
            .onFailure(error -> {
                String errorMessage = String.format("[Formulaire@%s::syncQuestionSpecs] An error has occurred when getting specific field : %s", this.getClass().getSimpleName(), error.getMessage());
                log.error(errorMessage);
                promise.fail(error.getMessage());
            });

        return promise.future();
    }

    @Override
    public Future<List<QuestionSpecificFields>> listByIds(List<Long> questionIds) {
        Promise<List<QuestionSpecificFields>> promise = Promise.promise();

        if (questionIds == null || questionIds.isEmpty()) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        String query = "SELECT * FROM " + QUESTION_SPECIFIC_FIELDS_TABLE + " WHERE question_id IN " + Sql.listPrepared(questionIds);
        JsonArray params = new JsonArray(questionIds);

        String errorMessage = "[Formulaire@DefaultQuestionSpecificFieldsService::listByIds] Fail to list questions specifics for questions with ids " + questionIds + " : ";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, QuestionSpecificFields.class, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<Optional<QuestionSpecificFields>> create(QuestionSpecificFields questionSpecificFields, Long questionId) {
        Promise<Optional<QuestionSpecificFields>> promise = Promise.promise();

        String query = "INSERT INTO " + QUESTION_SPECIFIC_FIELDS_TABLE + " (question_id, cursor_min_val, cursor_max_val, cursor_step, " +
                "cursor_min_label, cursor_max_label) VALUES (?, ?, ?, ?, ?, ?) RETURNING *;";

        JsonArray params = new JsonArray()
                .add(questionId)
                .add(questionSpecificFields.getCursorMinVal())
                .add(questionSpecificFields.getCursorMaxVal())
                .add(questionSpecificFields.getCursorStep())
                .add(questionSpecificFields.getCursorMinLabel())
                .add(questionSpecificFields.getCursorMaxLabel());

        String errorMessage = "[Formulaire@DefaultQuestionSpecificFieldsService::create] Fail to create question specifics for question with id " + questionId + " : ";
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(IModelHelper.sqlUniqueResultToIModel(promise, QuestionSpecificFields.class, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<List<QuestionSpecificFields>> update(List<Question> questions) {
        Promise<List<QuestionSpecificFields>> promise = Promise.promise();

        if (questions == null || questions.isEmpty()) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        String query = "UPDATE " + QUESTION_SPECIFIC_FIELDS_TABLE + " SET cursor_min_val = ?, cursor_max_val = ?, cursor_step = ?, " +
                "cursor_min_label = ?, cursor_max_label = ? WHERE question_id = ? RETURNING *;";

        List<TransactionElement> transactionElements = new ArrayList<>();
        List<Question> questionsWithSpecifics = questions.stream()
                .filter(question -> question.getSpecificFields() != null)
                .collect(Collectors.toList());

        for (Question question : questionsWithSpecifics) {
            JsonArray params = new JsonArray()
                    .add(question.getSpecificFields().getCursorMinVal())
                    .add(question.getSpecificFields().getCursorMaxVal())
                    .add(question.getSpecificFields().getCursorStep())
                    .add(question.getSpecificFields().getCursorMinLabel())
                    .add(question.getSpecificFields().getCursorMaxLabel())
                    .add(question.getId());
            transactionElements.add(new TransactionElement(query, params));
        }

        String errorMessage = "[Formulaire@DefaultQuestionSpecificFieldsService::update] Fail to update questions specifics for questions " + questions + " : ";
        TransactionHelper.executeTransactionAndGetJsonObjectResults(transactionElements, errorMessage)
            .onSuccess(result -> promise.complete(IModelHelper.toList(result, QuestionSpecificFields.class)))
            .onFailure(err -> promise.fail(err.getMessage()));

        return promise.future();
    }
}