package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface QuestionService {
    void list(String form_id, Handler<Either<String, JsonArray>> handler);

    void get(String id, Handler<Either<String, JsonObject>> handler);

    void create(JsonObject question, String form_id, Handler<Either<String, JsonObject>> handler);

    void update(String id, JsonObject question, Handler<Either<String, JsonObject>> handler);

    void delete(String id, Handler<Either<String, JsonObject>> handler);
}
