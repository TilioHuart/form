package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface ResponseFileService {
    void get(String responseId, Handler<Either<String, JsonObject>> handler);

    void create(String responseId, String fileId, String filename, String type, Handler<Either<String, JsonObject>> handler);

    void delete(String responseId, Handler<Either<String, JsonObject>> handler);
}
