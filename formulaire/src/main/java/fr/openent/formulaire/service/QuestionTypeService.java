package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface QuestionTypeService {
    /**
     * List all questions types
     * @param handler function handler returning JsonArray data
     */
    void list(Handler<Either<String, JsonArray>> handler);
}
