package fr.openent.formulaire.service;

import fr.openent.form.core.models.QuestionChoice;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

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
     * @param choice QuestionChoice data
     * @param locale locale language
     */
    Future<JsonObject> create(String questionId, QuestionChoice choice, String locale);

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
     * @param choice QuestionChoice data
     * @param locale locale language
     */
    Future<JsonObject> update(QuestionChoice choice, String locale);

    /**
     * Update a list of specific choices
     * @param choices List<QuestionChoice> data
     * @param locale locale language
     */
    Future<JsonArray> update(List<QuestionChoice> choices, String locale);

    /**
     * Delete a specific choice
     * @param choiceId choice identifier
     */
    Future<JsonObject> delete(String choiceId);

    /**
     * Check validity of a specific choice
     * @param choice choice to check
     */
    Future<Boolean> isTargetValid(QuestionChoice choice);
}