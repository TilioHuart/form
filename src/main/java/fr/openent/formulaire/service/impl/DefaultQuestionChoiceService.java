package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.QuestionChoiceService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

public class DefaultQuestionChoiceService implements QuestionChoiceService {

    @Override
    public void list(String questionId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.QUESTION_CHOICE_TABLE + " WHERE question_id = ? ORDER BY id;";
        JsonArray params = new JsonArray().add(questionId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listChoices(JsonArray questionIds, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.QUESTION_CHOICE_TABLE + " WHERE question_id IN " + Sql.listPrepared(questionIds);
        JsonArray params = new JsonArray().addAll(questionIds);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String choiceId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.QUESTION_CHOICE_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(choiceId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(String questionId, JsonObject choice, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Formulaire.QUESTION_CHOICE_TABLE + " (question_id, value, position, type, next_section_id) " +
                "VALUES (?, ?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(questionId)
                .add(choice.getString("value", ""))
                .add(choice.getInteger("position", 0))
                .add(choice.getString("type", ""))
                .add(choice.getInteger("next_section_id", null));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void createMultiple(JsonArray choices, String questionId, Handler<Either<String, JsonArray>> handler) {
        String query = "";
        JsonArray params = new JsonArray();

        List<JsonObject> allChoices = choices.getList();
        for (JsonObject choice : allChoices) {
            query += "INSERT INTO " + Formulaire.QUESTION_CHOICE_TABLE + " (question_id, value, position, type, next_section_id) " +
                    "VALUES (?, ?, ?, ?); ";
            params.add(questionId)
                    .add(choice.getString("value", ""))
                    .add(choice.getInteger("position", 0))
                    .add(choice.getString("type", ""))
                    .add(choice.getInteger("next_section_id", null));
        }

        query += "RETURNING *;";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void duplicate(int formId, int questionId, int originalQuestionId, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Formulaire.QUESTION_CHOICE_TABLE + " (question_id, value, type, next_section_id) " +
                "SELECT ?, value, type, (SELECT id FROM " + Formulaire.SECTION_TABLE + " WHERE original_section_id = qc.next_section_id AND form_id = ?) " +
                "FROM " + Formulaire.QUESTION_CHOICE_TABLE + " qc " +
                "WHERE question_id = ? ORDER BY id;";
        JsonArray params = new JsonArray().add(questionId).add(formId).add(originalQuestionId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void update(String choiceId, JsonObject choice, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Formulaire.QUESTION_CHOICE_TABLE + " SET value = ?, position = ?, type = ?, next_section_id = ? " +
                "WHERE id = ? RETURNING *;";
        JsonArray params = new JsonArray()
                .add(choice.getString("value", ""))
                .add(choice.getInteger("position", 0))
                .add(choice.getString("type", ""))
                .add(choice.getInteger("next_section_id", null))
                .add(choiceId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(String choiceId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Formulaire.QUESTION_CHOICE_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(choiceId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
