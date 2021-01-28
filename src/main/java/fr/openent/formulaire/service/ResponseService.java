package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface ResponseService {
    void list(String question_id, Handler<Either<String, JsonArray>> handler);

    void get(String id, Handler<Either<String, JsonObject>> handler);

    void create(JsonObject response, UserInfos user, String question_id, Handler<Either<String, JsonObject>> handler);

    void update(UserInfos user, String id, JsonObject response, Handler<Either<String, JsonObject>> handler);

    void delete(String id, Handler<Either<String, JsonObject>> handler);

    void exportResponses(String formId, Handler<Either<String, JsonArray>> handler);
}
