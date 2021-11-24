package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ResponseFileService {
    /**
     * List all files of a specific response
     * @param responseId response identifier
     * @param handler function handler returning JsonArray data
     */
    void list(String responseId, Handler<Either<String, JsonArray>> handler);

    /**
     * List all files of a specific question
     * @param questionId question identifier
     * @param handler function handler returning JsonArray data
     */
    void listByQuestion(String questionId, Handler<Either<String, JsonArray>> handler);

    /**
     * List all files of a specific form
     * @param formId  form identifier
     * @param handler function handler returning JsonArray data
     */
    void listByForm(String formId, Handler<Either<String, JsonArray>> handler);

    /**
     * Get a specific file by id
     * @param fileId file identifier
     * @param handler function handler returning JsonObject data
     */
    void get(String fileId, Handler<Either<String, JsonObject>> handler);

    /**
     * Create a response_file
     * @param responseId response identifier
     * @param fileId file identifier
     * @param filename file name
     * @param type file type
     * @param handler function handler returning JsonObject data
     */
    void create(String responseId, String fileId, String filename, String type, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete all response_files of specific responses
     * @param responseIds response identifiers
     * @param handler function handler returning JsonArray data
     */
    void deleteAllByResponse(JsonArray responseIds, Handler<Either<String, JsonArray>> handler);

    /**
     * Delete all response_files by their ids
     * @param fileIds responseFiles identifier
     * @param handler function handler returning JsonArray data
     */
    void deleteAll(JsonArray fileIds, Handler<Either<String, JsonArray>> handler);
}
