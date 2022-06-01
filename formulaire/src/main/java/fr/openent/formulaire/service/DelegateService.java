package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface DelegateService {
    /**
     * List all the delegate of the platform
     * @param handler function handler returning JsonArray data
     */
    void list(Handler<Either<String, JsonArray>> handler);
}