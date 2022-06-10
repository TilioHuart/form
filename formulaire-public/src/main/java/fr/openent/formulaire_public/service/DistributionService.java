package fr.openent.formulaire_public.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface DistributionService {
    /**
     * Get a specific distribution by key
     * @param distributionKey distribution key identifier
     * @param handler function handler returning JsonObject data
     */
    void getDistributionByKey(String distributionKey, Handler<Either<String, JsonObject>> handler);

    /**
     * Create a new distribution
     * @param form JsonObject data
     * @param handler function handler returning JsonObject data
     */
    void createDistribution(JsonObject form, Handler<Either<String, JsonObject>> handler);

    /**
     * Change captcha of a specific distribution
     * @param distributionKey distribution key identifier
     * @param handler function handler returning JsonObject data
     */
    void updateCaptchaDistribution(String distributionKey, Handler<Either<String, JsonObject>> handler);

    /**
     * Finish a specific distribution
     * @param distributionKey distribution key identifier
     * @param handler function handler returning JsonObject data
     */
    void finishDistribution(String distributionKey, Handler<Either<String, JsonObject>> handler);
}