package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.QuestionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultQuestionService implements QuestionService {

    @Override
    public void list(String form_id, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.QUESTION_TABLE + " WHERE form_id = ? ORDER BY position;";
        JsonArray params = new JsonArray().add(form_id);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String id, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.QUESTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(id);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(JsonObject question, String form_id, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Formulaire.QUESTION_TABLE + " (form_id, position, question_type, statement, mandatory) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(form_id)
                .add(question.getInteger("position", 0))
                .add(question.getInteger("question_type", 1))
                .add(question.getString("statement", ""))
                .add(question.getBoolean("mandatory", false));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void update(String id, JsonObject question, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Formulaire.QUESTION_TABLE + " SET title=  ?, position = ?, question_type = ?, " +
                "statement = ?, mandatory = ? WHERE id = ?;";
        JsonArray params = new JsonArray()
                .add(question.getString("title", ""))
                .add(question.getInteger("position", 0))
                .add(question.getInteger("question_type", 1))
                .add(question.getString("statement", ""))
                .add(question.getBoolean("mandatory", false))
                .add(id);

        query += "UPDATE " + Formulaire.FORM_TABLE + " SET date_modification = ? WHERE id = ?;";
        params.add("NOW()").add(question.getInteger("form_id"));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(String id, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Formulaire.QUESTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(id);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
