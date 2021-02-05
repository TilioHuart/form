package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface QuestionService {
    void list(String formId, Handler<Either<String, JsonArray>> handler);

    void countQuestions(String formId, Handler<Either<String, JsonObject>> handler);

    void get(String questionId, Handler<Either<String, JsonObject>> handler);

    void getByPosition(String formId, String position, Handler<Either<String, JsonObject>> handler);

    void create(JsonObject question, String formId, Handler<Either<String, JsonObject>> handler);

    void update(String questionId, JsonObject question, Handler<Either<String, JsonObject>> handler);

    void delete(String questionId, Handler<Either<String, JsonObject>> handler);
}
