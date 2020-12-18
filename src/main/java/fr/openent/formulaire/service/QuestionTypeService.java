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

    /**
     * Get a specific question type by code
     * @param code code of the wanted question_type
     * @param handler function handler returning JsonObject data
     */
    void get(String code, Handler<Either<String, JsonObject>> handler);
}
