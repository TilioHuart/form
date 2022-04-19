package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface QuestionChoiceService {
    /**
     * List all the choices of a specific question
     * @param questionId question identifier
     * @param handler function handler returning JsonArray data
     */
    void list(String questionId, Handler<Either<String, JsonArray>> handler);

    /**
     * ListChoices all the choices of a specific question
     * @param questionIds JsonArray identifier
     * @param handler function handler returning JsonArray data
     */
    void listChoices(JsonArray questionIds, Handler<Either<String, JsonArray>> handler);

    /**
     * Get a specific by id
     * @param choiceId choice identifier
     * @param handler function handler returning JsonObject data
     */
    void get(String choiceId, Handler<Either<String, JsonObject>> handler);

    /**
     * Create a choice for a specific question
     * @param questionId question identifier
     * @param choice JsonObject data
     * @param handler function handler returning JsonObject data
     */
    void create(String questionId, JsonObject choice, Handler<Either<String, JsonObject>> handler);

    /**
     * Create several choices for a specific question
     * @param choices JsonArray data
     * @param questionId question identifier
     * @param handler function handler returning JsonArray data
     */
    void createMultiple(JsonArray choices, String questionId, Handler<Either<String, JsonArray>> handler);

    /**
     * Duplicate choices of a specific question
     * @param formId new form identifier
     * @param questionId question identifier
     * @param originalQuestionId original question identifier
     * @param handler function handler returning JsonObject data
     */
    void duplicate(int formId, int questionId, int originalQuestionId, Handler<Either<String, JsonObject>> handler);

    /**
     * Update a specific choice
     * @param choiceId choice identifier
     * @param choice JsonObject data
     * @param handler function handler returning JsonObject data
     */
    void update(String choiceId, JsonObject choice, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete a specific choice
     * @param choiceId choice identifier
     * @param handler function handler returning JsonObject data
     */
    void delete(String choiceId, Handler<Either<String, JsonObject>> handler);
}