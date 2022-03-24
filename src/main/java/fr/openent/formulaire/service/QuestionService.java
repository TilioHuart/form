package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface QuestionService {
    /**
     * List all the questions out of sections of a specific form
     * @param formId form identifier
     * @param handler function handler returning JsonArray data
     */
    void listForForm(String formId, Handler<Either<String, JsonArray>> handler);

    /**
     * List all the questions in a specific section
     * @param sectionId section identifier
     * @param handler function handler returning JsonArray data
     */
    void listForSection(String sectionId, Handler<Either<String, JsonArray>> handler);

    /**
     * List all the questions (in and out of sections) of a specific form
     * @param formId section identifier
     * @param handler function handler returning JsonArray data
     */
    void listForFormAndSection(String formId, Handler<Either<String, JsonArray>> handler);

    /**
     * List all the questions of a specific form without freetext questions
     * @param formId form identifier
     * @param isPdf is export for PDF
     * @param handler function handler returning JsonArray data
     */
    void export(String formId, boolean isPdf, Handler<Either<String, JsonArray>> handler);

    /**
     * Get a specific question by id
     * @param questionId question identifier
     * @param handler function handler returning JsonObject data
     */
    void get(String questionId, Handler<Either<String, JsonObject>> handler);

    /**
     * Create a question in a specific form
     * @param question JsonObject data
     * @param formId form identifier
     * @param handler function handler returning JsonObject data
     */
    void create(JsonObject question, String formId, Handler<Either<String, JsonObject>> handler);

    /**
     * Create several questions in a specific form
     * @param questions JsonArray data
     * @param formId form identifier
     * @param handler function handler returning JsonArray data
     */
    void createMultiple(JsonArray questions, String formId, Handler<Either<String, JsonArray>> handler);

    /**
     * Update a specific question
     * @param questionId question identifier
     * @param question JsonObject data
     * @param handler function handler returning JsonObject data
     */
    void update(String questionId, JsonObject question, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete a specific question
     * @param question question
     * @param handler function handler returning JsonObject data
     */
    void delete(JsonObject question, Handler<Either<String, JsonObject>> handler);
}
