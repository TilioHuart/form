package fr.openent.formulaire_public.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface FormService {
    /**
     * Get a specific form by key
     * @param formKey form key identifier
     * @param handler function handler returning JsonObject data
     */
    void getFormByKey(String formKey, Handler<Either<String, JsonObject>> handler);

    /**
     * List all the managers of a form
     * @param formId form identifier
     * @param handler function handler returning JsonArray data
     */
    void listManagers(String formId, Handler<Either<String, JsonArray>> handler);
}