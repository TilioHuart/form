package fr.openent.formulaire.service;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface NotifyService {

    /**
     * Send notification when a nwe form is distributed to a responder
     * @param request request
     * @param form form sent
     * @param responders ids of the responders to the form
     */
    void notifyNewForm(HttpServerRequest request, JsonObject form, JsonArray responders);

    /**
     * Send notification when a response is send by a responder
     * @param request request
     * @param form form responded
     * @param managers ids of the managers of the form
     */
    void notifyResponse(HttpServerRequest request, JsonObject form, JsonArray managers);
}
