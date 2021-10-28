package fr.openent.formulaire.helpers;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class RenderHelper {
    private static final Logger log = LoggerFactory.getLogger(FutureHelper.class);

    private RenderHelper() {}

    public static void ok(HttpServerRequest request) {
        Renders.renderJson(request, new JsonObject(), 200);
    }

    public static void badRequest(HttpServerRequest request, Either event) {
        JsonObject error = (new JsonObject()).put("error", event.left().getValue());
        Renders.renderJson(request, error, 400);
    }
}
