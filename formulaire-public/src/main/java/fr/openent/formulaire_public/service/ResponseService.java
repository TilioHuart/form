package fr.openent.formulaire_public.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ResponseService {
    /**
     * Create multiple responses
     * @param responses JsonArray data
     * @param distribution distribution to answer
     * @param handler function handler returning JsonArray data
     */
    void createResponses(JsonArray responses, JsonObject distribution, Handler<Either<String, JsonArray>> handler);
}