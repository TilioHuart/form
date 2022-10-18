package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface QuestionSpecificFieldService {
    /**
     * Add question to specific field
     * @param question JsonObject data
     * @param questionId question identifier
     * @param handler function handler returning JsonObject data
     */
    void create(JsonObject question, String questionId, Handler<Either<String, JsonObject>> handler);

    /**
     * Update question to specific field
     * @param questions question identifier
     * @param questionId JsonArray data
     * @param handler function handler returning JsonArray data
     */
    void update(JsonArray questions, String questionId, Handler<Either<String, JsonArray>> handler);

    /**
     * Get specific field by questionId
     * @param questionId question identifier
     * @param handler function handler returning JsonObject data
     */
    void get(String questionId, Handler<Either<String, JsonObject>> handler);
}
