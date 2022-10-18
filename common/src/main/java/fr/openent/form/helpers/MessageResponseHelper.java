package fr.openent.form.helpers;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.openent.form.core.constants.Fields.*;

public class MessageResponseHelper {

    private MessageResponseHelper() {
    }

    public static Handler<AsyncResult<Message<JsonObject>>> messageJsonArrayHandler(Handler<Either<String, JsonArray>> handler) {
        return event -> {
            if (event.succeeded() && event.result().body().getString(STATUS).equals(OK)) {
                handler.handle(new Either.Right<>(event.result().body().getJsonArray(RESULT, event.result().body().getJsonArray(RESULTS))));
            }
            else {
                if (event.failed()) {
                    handler.handle(new Either.Left<>(event.cause().getMessage()));
                    return;
                }
                handler.handle(new Either.Left<>(event.result().body().getString(MESSAGE)));
            }
        };
    }

    public static Handler<AsyncResult<Message<JsonObject>>> messageJsonObjectHandler(Handler<Either<String, JsonObject>> handler) {
        return event -> {
            if (event.succeeded() && event.result().body().getString(STATUS).equals(OK)) {
                if (!event.result().body().containsKey(RESULT)) {
                    handler.handle(new Either.Right<>(event.result().body()));
                }
                else {
                    handler.handle(new Either.Right<>(event.result().body().getJsonObject(RESULT)));
                }
            } else {
                if (event.failed()) {
                    handler.handle(new Either.Left<>(event.cause().getMessage()));
                    return;
                }
                handler.handle(new Either.Left<>(event.result().body().getString(MESSAGE)));
            }
        };
    }
}
