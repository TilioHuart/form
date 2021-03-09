package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface ResponseService {
    void list(String questionId, Handler<Either<String, JsonArray>> handler);

    void listMine(String questionId, UserInfos user, Handler<Either<String, JsonArray>> handler);

    void get(String responseId, Handler<Either<String, JsonObject>> handler);

    void create(JsonObject response, UserInfos user, String questionId, Handler<Either<String, JsonObject>> handler);

    void update(UserInfos user, String responseId, JsonObject response, Handler<Either<String, JsonObject>> handler);

    void delete(String responseId, Handler<Either<String, JsonObject>> handler);

    void exportResponses(String formId, Handler<Either<String, JsonArray>> handler);

    void getFile(String responseId, String fileId, Handler<Either<String, JsonObject>> handler);

    void createFile(String responseId, String fileId, String filename, Handler<Either<String, JsonObject>> handler);

    void deleteFile(String responseId, String formId, Handler<Either<String, JsonObject>> handler);
}
