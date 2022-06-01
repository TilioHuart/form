package fr.openent.form.helpers;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BusResultHelper {

    private BusResultHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static Handler<Either<String, JsonObject>> busResponseHandlerEitherObject(final Message message) {
        return event -> {
            if (event.isRight()) {
                message.reply((new JsonObject()).put("status", "ok").put("result", event.right().getValue()));
            } else {
                JsonObject error = (new JsonObject()).put("status", "error").put("message", event.left().getValue());
                message.reply(error);
            }
        };
    }

    public static Handler<Either<String, JsonArray>> busResponseHandlerEitherArray(final Message message) {
        return event -> {
            if (event.isRight()) {
                message.reply((new JsonObject()).put("status", "ok").put("result", event.right().getValue()));
            } else {
                JsonObject error = (new JsonObject()).put("status", "error").put("message", event.left().getValue());
                message.reply(error);
            }
        };
    }

    public static <T>Handler<AsyncResult<T>> busResponseHandlerAsync(final Message<T> message) {
        return event -> {
            if (event.succeeded()) {
                message.reply((new JsonObject()).put("status", "ok").put("result", event.result()));
            } else {
                JsonObject error = (new JsonObject()).put("status", "error").put("message", event.cause().getMessage());
                message.reply(error);
            }
        };
    }
}
