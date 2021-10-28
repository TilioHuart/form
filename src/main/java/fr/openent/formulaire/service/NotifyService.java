package fr.openent.formulaire.service;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface NotifyService {

    /**
     * Send notification when a response is send by a respondent
     * @param request   request
     * @param form      form responded
     * @param managers  ids of the managersof the form
     */
    void notifyResponse(HttpServerRequest request, JsonObject form, JsonArray managers);
}
