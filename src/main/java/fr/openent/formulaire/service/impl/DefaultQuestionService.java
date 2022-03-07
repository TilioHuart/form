package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.QuestionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

public class DefaultQuestionService implements QuestionService {

    @Override
    public void listForForm(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.QUESTION_TABLE + " WHERE form_id = ? AND section_id IS NULL ORDER BY position;";
        JsonArray params = new JsonArray().add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listForSection(String sectionId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.QUESTION_TABLE + " WHERE section_id = ? ORDER BY position;";
        JsonArray params = new JsonArray().add(sectionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void export(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.QUESTION_TABLE +
                " WHERE form_id = ? AND question_type != 1 ORDER BY position;";
        JsonArray params = new JsonArray().add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String questionId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.QUESTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(questionId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(JsonObject question, String formId, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Formulaire.QUESTION_TABLE + " (form_id, title, position, question_type, statement, " +
                "mandatory, section_id, section_position, conditional) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(formId)
                .add(question.getString("title", ""))
                .add(question.getInteger("position", null))
                .add(question.getInteger("question_type", 1))
                .add(question.getString("statement", ""))
                .add(question.getBoolean("mandatory", false))
                .add(question.getInteger("section_id", null))
                .add(question.getInteger("section_position", null))
                .add(question.getBoolean("conditional", false));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void createMultiple(JsonArray questions, String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "INSERT INTO " + Formulaire.QUESTION_TABLE + " (form_id, title, position, question_type, " +
                "statement, mandatory, section_id, section_position, conditional) VALUES ";
        JsonArray params = new JsonArray();

        List<JsonObject> allQuestions = questions.getList();
        for (JsonObject question : allQuestions) {
            query += "(?, ?, ?, ?, ?, ?, ?, ?, ?), ";
            params.add(formId)
                    .add(question.getString("title", ""))
                    .add(question.getInteger("position", null))
                    .add(question.getInteger("question_type", 1))
                    .add(question.getString("statement", ""))
                    .add(question.getBoolean("mandatory", false))
                    .add(question.getInteger("section_id", null))
                    .add(question.getInteger("section_position", null))
                    .add(question.getBoolean("conditional", false));
        }

        query = query.substring(0, query.length() - 2) + " RETURNING *;";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void update(String questionId, JsonObject question, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Formulaire.QUESTION_TABLE + " SET title = ?, position = ?, question_type = ?, " +
                "statement = ?, mandatory = ?, section_id = ?, section_position = ?, conditional = ?  WHERE id = ? RETURNING *;";
        JsonArray params = new JsonArray()
                .add(question.getString("title", ""))
                .add(question.getInteger("position", null))
                .add(question.getInteger("question_type", 1))
                .add(question.getString("statement", ""))
                .add(question.getBoolean("mandatory", false))
                .add(question.getInteger("section_id", null))
                .add(question.getInteger("section_position", null))
                .add(question.getBoolean("conditional", false))
                .add(questionId);

        query += "UPDATE " + Formulaire.FORM_TABLE + " SET date_modification = ? WHERE id = ?;";
        params.add("NOW()").add(question.getInteger("form_id"));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(String questionId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Formulaire.QUESTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(questionId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
