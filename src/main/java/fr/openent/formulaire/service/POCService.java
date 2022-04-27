package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

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


}