package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface QuestionSpecificFieldService {
    /**
     * List all the specifics fields of question from a list of ids
     * @param questionIds questions identifiers
     * @param handler function handler returning JsonArray data
     */
    void listByIds(JsonArray questionIds, Handler<Either<String, JsonArray>> handler);

    /**
     * Add specific fields to a question
     * @param question JsonObject data
     * @param questionId question identifier
     * @param handler function handler returning JsonObject data
     */
    void create(JsonObject question, String questionId, Handler<Either<String, JsonObject>> handler);

    /**
     * Update specific fields of questions
     * @param questions JsonArray data
     * @param handler function handler returning JsonArray data
     */
    void update(JsonArray questions, Handler<Either<String, JsonArray>> handler);
}
