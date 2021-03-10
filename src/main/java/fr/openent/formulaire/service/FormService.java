package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface FormService {
    void list(List<String> groupsAndUserIds, UserInfos user, Handler<Either<String, JsonArray>> handler);

    void listSentForms(UserInfos user, Handler<Either<String, JsonArray>> handler);

    void get(String id, Handler<Either<String, JsonObject>> handler);

    void create(JsonObject form, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void createMultiple(JsonArray forms, UserInfos user, Handler<Either<String, JsonArray>> handler);

    void update(String formId, JsonObject form, Handler<Either<String, JsonObject>> handler);

    void delete(String formId, Handler<Either<String, JsonObject>> handler);

    void getMyFormRights(String formId, UserInfos user, Handler<Either<String, JsonArray>> handler);

    void getAllMyFormRights(UserInfos user, Handler<Either<String, JsonArray>> handler);

    void getImage (EventBus eb, String idImage, Handler<Either<String,JsonObject>> handler);
}
