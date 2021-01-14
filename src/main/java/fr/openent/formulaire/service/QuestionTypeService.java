package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface QuestionTypeService {
    void list(Handler<Either<String, JsonArray>> handler);

    void get(String code, Handler<Either<String, JsonObject>> handler);
}
