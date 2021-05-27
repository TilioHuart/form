package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface ResponseService {
    /**
     * List all the responses to a specific question
     * @param questionId question identifier
     * @param nbLines number of lines already loaded
     * @param handler function handler returning JsonArray data
     */
    void list(String questionId, String nbLines, JsonArray distribs, Handler<Either<String, JsonArray>> handler);

    /**
     * List all my responses to a specific question for a specific distribution
     * @param questionId question identifier
     * @param distributionId distribution identifier
     * @param user user connected
     * @param handler function handler returning JsonArray data
     */
    void listMineByDistribution(String questionId, String distributionId, UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * Get a specific response by id
     * @param responseId response identifier
     * @param handler function handler returning JsonObject data
     */
    void get(String responseId, Handler<Either<String, JsonObject>> handler);

    /**
     * Create a response
     * @param response JsonObject data
     * @param user user connected
     * @param questionId question identifier
     * @param handler function handler returning JsonObject data
     */
    void create(JsonObject response, UserInfos user, String questionId, Handler<Either<String, JsonObject>> handler);

    /**
     * Update a specific response
     * @param user user connected
     * @param responseId response identifier
     * @param response JsonObject data
     * @param handler function handler returning JsonObject data
     */
    void update(UserInfos user, String responseId, JsonObject response, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete a specific response
     * @param responseId response identifier
     * @param handler function handler returning JsonObject data
     */
    void delete(String responseId, Handler<Either<String, JsonObject>> handler);

    /**
     * Get all the responses of a specific form to export into a CSV file
     * @param formId form identifier
     * @param handler function handler returning JsonArray data
     */
    void exportCSVResponses(String formId, Handler<Either<String, JsonArray>> handler);

    /**
     * Get all the responses of a specific form to export into a PDF file
     * @param formId form identifier
     * @param handler function handler returning JsonArray data
     */
    void exportPDFResponses(String formId, Handler<Either<String, JsonArray>> handler);
}