package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ResponseFileService {
    void list(String responseId, Handler<Either<String, JsonArray>> handler);

    void listByQuestion(String questionId, Handler<Either<String, JsonArray>> handler);

    void get(String fileId, Handler<Either<String, JsonObject>> handler);

    void create(String responseId, String fileId, String filename, String type, Handler<Either<String, JsonObject>> handler);

    void deleteAll(String responseId, Handler<Either<String, JsonArray>> handler);
}
