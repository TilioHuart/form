package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface DistributionService {
    void list(UserInfos user, Handler<Either<String, JsonArray>> handler);

    void get(String formId, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void create(String formId, UserInfos user, JsonArray responders, Handler<Either<String, JsonObject>> handler);

    void update(String id, JsonObject distribution, Handler<Either<String, JsonObject>> handler);

    void delete(String id, Handler<Either<String, JsonObject>> handler);

    void getDuplicates(String formId, JsonArray responders, Handler<Either<String, JsonArray>> handler);

    void getRemoved(String formId, JsonArray responders, Handler<Either<String, JsonArray>> handler);

    void createMultiple(String formId, UserInfos user, JsonArray responders, JsonArray duplicates, Handler<Either<String, JsonObject>> handler);

    void removeMultiple(String formId, JsonArray removed, Handler<Either<String, JsonObject>> handler);
}