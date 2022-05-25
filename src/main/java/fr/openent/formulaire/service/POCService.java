package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface POCService {
    /**
     * Get a specific form by key
     * @param formKey form key identifier
     * @param handler function handler returning JsonObject data
     */
    void getFormByKey(String formKey, Handler<Either<String, JsonObject>> handler);

    /**
     * Create a new distribution
     * @param form JsonObject data
     * @param handler function handler returning JsonObject data
     */
    void createDistribution(JsonObject form, Handler<Either<String, JsonObject>> handler);

    /**
     * Get a specific distribution by key
     * @param distributionKey distribution key identifier
     * @param handler function handler returning JsonObject data
     */
    void getDistributionByKey(String distributionKey, Handler<Either<String, JsonObject>> handler);

    /**
     * Create multiple responses
     * @param responses JsonArray data
     * @param distribution distribution to answer
     * @param handler function handler returning JsonObject data
     */
    void createResponses(JsonArray responses, JsonObject distribution, Handler<Either<String, JsonArray>> handler);

    /**
     * Finish a specific distribution
     * @param distributionKey distribution key identifier
     * @param handler function handler returning JsonObject data
     */
    void finishDistribution(String distributionKey, Handler<Either<String, JsonObject>> handler);

    /**
     * List all the managers of a form
     * @param formId form identifier
     * @param handler function handler returning JsonArray data
     */
    void listManagers(String formId, Handler<Either<String, JsonArray>> handler);

    /**
     * Send notification when a response is send
     * @param request request
     * @param form form responded
     * @param managers ids of the managers of the form
     */
    void notifyResponse(HttpServerRequest request, JsonObject form, JsonArray managers);
}