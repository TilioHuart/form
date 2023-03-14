package fr.openent.formulaire.service.impl;

import fr.openent.form.core.enums.QuestionTypes;
import fr.openent.form.core.models.FormElement;
import fr.openent.form.core.models.Question;
import fr.openent.form.core.models.Section;
import fr.openent.form.helpers.FutureHelper;
import fr.openent.form.helpers.UtilsHelper;
import fr.openent.formulaire.service.FormElementService;
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
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Constants.*;
import static fr.openent.form.core.constants.Constants.CONDITIONAL_QUESTIONS;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Fields.ID;
import static fr.openent.form.core.constants.Tables.QUESTION_TABLE;
import static fr.openent.form.core.constants.Tables.SECTION_TABLE;
import static fr.openent.form.helpers.SqlHelper.getParamsForUpdateDateModifFormRequest;
import static fr.openent.form.helpers.SqlHelper.getUpdateDateModifFormRequest;

public class DefaultFormElementService implements FormElementService {

    @Override
    public void countFormElements(String formId, Handler<Either<String, JsonObject>> handler) {
        String countQuestions = "SELECT COUNT(*) FROM " + QUESTION_TABLE + " WHERE form_id = ? AND section_id IS NULL AND matrix_id IS NULL";
        String countSections = "SELECT COUNT(*) FROM " + SECTION_TABLE + " WHERE form_id = ?";
        String query = "SELECT ((" + countQuestions + ") + (" + countSections + ")) AS count;";
        JsonArray params = new JsonArray().add(formId).add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getTypeAndIdByPosition(String formId, String position, Handler<Either<String, JsonObject>> handler) {
        String getQuestions = "SELECT id, 'question' AS element_type FROM " + QUESTION_TABLE + " WHERE form_id = ? AND position = ? AND matrix_id IS NULL";
        String getSections = "SELECT id, 'section' AS element_type FROM " + SECTION_TABLE + " WHERE form_id = ? AND position = ?";
        String query = getQuestions + " UNION " + getSections + ";";
        JsonArray params = new JsonArray().add(formId).add(position).add(formId).add(position);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getByTypeAndId(String elementId, String elementType, Handler<Either<String, JsonObject>> handler) {
        String table = elementType.equals(QUESTION) ? QUESTION_TABLE : SECTION_TABLE;
        String query = "SELECT * FROM " + table + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(elementId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public Future<JsonArray> update(List<FormElement> formElements, String formId) {
        Promise<JsonArray> promise = Promise.promise();

        // Type formElements
        List<Section> sections = FormElement.getSections(formElements);
        List<Question> questions = FormElement.getQuestions(formElements);

        if (sections.isEmpty()) {
            promise.fail("[Formulaire@DefaultFormElementService::update] formElements must contain at least one section. " +
                    "You should use questionService.update instead.");
            return promise.future();
        }
        else if (questions.isEmpty()) {
            promise.fail("[Formulaire@DefaultFormElementService::update] formElements must contain at least one question. " +
                    "You should use sectionService.update instead.");
            return promise.future();
        }

        SqlStatementsBuilder s = new SqlStatementsBuilder();

        // Get ids
        List<Number> sectionIds = sections.stream().map(FormElement::getId).collect(Collectors.toList());
        List<Number> questionIds = questions.stream().map(FormElement::getId).collect(Collectors.toList());

        // Sections and questions nullifying queries
        String sectionNullifyerQuery = "UPDATE " + SECTION_TABLE + " SET position = NULL WHERE id IN " + Sql.listPrepared(sectionIds) + ";";
        String questionNullifyerQuery =
                "UPDATE " + QUESTION_TABLE + " " +
                "SET position = NULL, section_id = NULL, section_position = NULL, matrix_id = NULL, matrix_position = NULL " +
                "WHERE id IN " + Sql.listPrepared(questionIds) + ";";

        // Sections and questions queries
        String sectionQueryUpdate = "UPDATE " + SECTION_TABLE + " SET title = ?, description = ?, position = ? WHERE id = ? RETURNING *;";
        String questionQueryUpdate =
                "UPDATE " + QUESTION_TABLE + " SET title = ?, position = ?, question_type = ?, statement = ?, mandatory = ?, " +
                "section_id = ?, section_position = ?, conditional = ?, placeholder = ?, matrix_id = ?, matrix_position = ? " +
                "WHERE id = ? RETURNING *;";

        s.raw(TRANSACTION_BEGIN_QUERY);

        // Nullify positions in sections and questions
        s.prepared(sectionNullifyerQuery, new JsonArray(sectionIds));
        s.prepared(questionNullifyerQuery, new JsonArray(questionIds));

        // Update sections
        for (Section section : sections) {
            JsonArray params = new JsonArray()
                    .add(section.getTitle())
                    .add(section.getDescription())
                    .add(section.getPosition())
                    .add(section.getId());
            s.prepared(sectionQueryUpdate, params);
        }
        // Update questions
        for (Question question : questions) {
            int questionType = question.getMatrixId() != null &&
                    !MATRIX_CHILD_QUESTIONS.contains(question.getQuestionType()) ?
                    QuestionTypes.SINGLEANSWERRADIO.getCode() :
                    question.getQuestionType();
            boolean isConditional = CONDITIONAL_QUESTIONS.contains(question.getQuestionType()) && question.getConditional();
            JsonArray params = new JsonArray()
                    .add(question.getTitle())
                    .add(question.getSectionPosition() != null ? null : question.getPosition())
                    .add(questionType)
                    .add(question.getStatement())
                    .add(question.getMandatory() || isConditional)
                    .add(question.getSectionId())
                    .add(question.getSectionPosition())
                    .add(isConditional)
                    .add(question.getPlaceholder())
                    .add(question.getMatrixId())
                    .add(question.getMatrixPosition())
                    .add(question.getId());
            s.prepared(questionQueryUpdate, params);
        }

        s.prepared(getUpdateDateModifFormRequest(), getParamsForUpdateDateModifFormRequest(formId));
        s.raw(TRANSACTION_COMMIT_QUERY);

        String errorMessage = "[Formulaire@DefaultFormElementService::update] Fail to update form elements for form with id " + formId + " : ";
        Sql.getInstance().transaction(s.build(), SqlResult.validResultsHandler(FutureHelper.handlerEither(promise, errorMessage)));

        return promise.future();
    }
}
