package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface QuestionChoiceService {
    void list(String questionId, Handler<Either<String, JsonArray>> handler);

    void get(String choiceId, Handler<Either<String, JsonObject>> handler);

    void create(String questionId, JsonObject choice, Handler<Either<String, JsonObject>> handler);

    void update(String choiceId, JsonObject choice, Handler<Either<String, JsonObject>> handler);

    void delete(String choiceId, Handler<Either<String, JsonObject>> handler);
}