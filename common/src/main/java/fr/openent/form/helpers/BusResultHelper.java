package fr.openent.form.helpers;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.openent.form.core.constants.Fields.*;

public class BusResultHelper {

    private BusResultHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static Handler<Either<String, JsonObject>> busResponseHandlerEitherObject(final Message message) {
        return event -> {
            if (event.isRight()) {
                message.reply((new JsonObject()).put(STATUS, OK).put(RESULT, event.right().getValue()));
            } else {
                JsonObject error = (new JsonObject()).put(STATUS, ERROR).put(MESSAGE, event.left().getValue());
                message.reply(error);
            }
        };
    }

    public static Handler<Either<String, JsonArray>> busResponseHandlerEitherArray(final Message message) {
        return event -> {
            if (event.isRight()) {
                message.reply((new JsonObject()).put(STATUS, OK).put(RESULT, event.right().getValue()));
            } else {
                JsonObject error = (new JsonObject()).put(STATUS, ERROR).put(MESSAGE, event.left().getValue());
                message.reply(error);
            }
        };
    }

    public static <T>Handler<AsyncResult<T>> busResponseHandlerAsync(final Message<T> message) {
        return event -> {
            if (event.succeeded()) {
                message.reply((new JsonObject()).put(STATUS, OK).put(RESULT, event.result()));
            } else {
                JsonObject error = (new JsonObject()).put(STATUS, ERROR).put(MESSAGE, event.cause().getMessage());
                message.reply(error);
            }
        };
    }

    public static void busArrayHandler(Future<JsonArray> future, Message<JsonObject> message) {
        future
                .onSuccess(result -> message.reply((new JsonObject()).put(STATUS, OK).put(RESULT, result)))
                .onFailure(error -> message.reply((new JsonObject()).put(STATUS, ERROR).put(MESSAGE, error.getMessage())));
    }

    public static void busObjectHandler(Future<JsonObject> future, Message<JsonObject> message) {
        future
                .onSuccess(result -> message.reply((new JsonObject()).put(STATUS, OK).put(RESULT, result)))
                .onFailure(error -> message.reply((new JsonObject()).put(STATUS, ERROR).put(MESSAGE, error.getMessage())));
    }
}
