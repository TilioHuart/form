package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface ResponseFileService {
    void getFile(String responseId, Handler<Either<String, JsonObject>> handler);

    void createFile(String responseId, String fileId, String filename, String type, Handler<Either<String, JsonObject>> handler);

    void deleteFile(String responseId, Handler<Either<String, JsonObject>> handler);
}
