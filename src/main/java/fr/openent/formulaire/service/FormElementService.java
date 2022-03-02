package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface FormElementService {

    /**
     * Count the number of form elements in a specific form
     * @param formId form identifier
     * @param handler function handler returning JsonObject data
     */
    void countFormElements(String formId, Handler<Either<String, JsonObject>> handler);

    /**
     * Get a specific element id by position in a specific form
     * @param formId form identifier
     * @param position position of the specific form element
     * @param handler function handler returning JsonObject data
     */
    void getIdByPosition(String formId, String position, Handler<Either<String, JsonObject>> handler);

    /**
     * Get a specific element by position in a specific form
     * @param elementId id of the specific form element
     * @param elementType type of the specific form element
     * @param handler function handler returning JsonObject data
     */
    void getByTypeAndId(String elementId, String elementType, Handler<Either<String, JsonObject>> handler);
}
