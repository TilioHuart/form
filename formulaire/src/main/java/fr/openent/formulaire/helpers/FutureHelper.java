package fr.openent.formulaire.helpers;

import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.impl.CompositeFutureImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

public class FutureHelper {

    private static final Logger log = LoggerFactory.getLogger(FutureHelper.class);

    private FutureHelper() {
    }

    // Promise Either

    public static <R> Handler<Either<String, R>> handlerEither(Promise<R> promise, String errorMessage) {
        return event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue());
                return;
            }
            log.error((errorMessage != null ? errorMessage : "") + event.left().getValue());
            promise.fail(event.left().getValue());
        };
    }

    public static <R> Handler<Either<String, R>> handlerEither(Promise<R> promise) {
        return handlerEither(promise, null);
    }


    // Promise AsyncResult

    public static <R> Handler<AsyncResult<R>> handlerAsyncResult(Promise<R> promise, String errorMessage) {
        return event -> {
            if (event.succeeded()) {
                promise.complete(event.result());
                return;
            }
            log.error((errorMessage != null ? errorMessage : "") + event.cause().getMessage());
            promise.fail(event.cause().getMessage());
        };
    }

    public static <R> Handler<AsyncResult<R>> handlerAsyncResult(Promise<R> promise) {
        return handlerAsyncResult(promise, null);
    }




    // AsyncResult

    public static Handler<Either<String, JsonArray>> handlerJsonArray(Handler<AsyncResult<JsonArray>> handler, String errorMessage) {
        return event -> {
            if (event.isRight()) {
                handler.handle(Future.succeededFuture(event.right().getValue()));
                return;
            }
            log.error((errorMessage != null ? errorMessage : "") + event.left().getValue());
            handler.handle(Future.failedFuture(event.left().getValue()));
        };
    }

    public static Handler<Either<String, JsonArray>> handlerJsonArray(Handler<AsyncResult<JsonArray>> handler) {
        return handlerJsonArray(handler, null);
    }

    public static Handler<Either<String, JsonObject>> handlerJsonObject(Handler<AsyncResult<JsonObject>> handler, String errorMessage) {
        return event -> {
            if (event.isRight()) {
                handler.handle(Future.succeededFuture(event.right().getValue()));
                return;
            }
            log.error((errorMessage != null ? errorMessage : "") + event.left().getValue());
            handler.handle(Future.failedFuture(event.left().getValue()));
        };
    }

    public static Handler<Either<String, JsonObject>> handlerJsonObject(Handler<AsyncResult<JsonObject>> handler) {
        return handlerJsonObject(handler, null);
    }

    // Future

    public static Handler<Either<String, JsonArray>> handlerJsonArray(Future<JsonArray> future) {
        return event -> {
            if (event.isRight()) {
                future.complete(event.right().getValue());
            } else {
                log.error(event.left().getValue());
                future.fail(event.left().getValue());
            }
        };
    }

    public static Handler<Either<String, JsonObject>> handlerJsonObject(Future<JsonObject> future) {
        return event -> {
            if (event.isRight()) {
                future.complete(event.right().getValue());
            } else {
                log.error(event.left().getValue());
                future.fail(event.left().getValue());
            }
        };
    }

    public static <T> CompositeFuture all(List<Future<T>> futures) {
        return CompositeFutureImpl.all(futures.toArray(new Future[futures.size()]));
    }

    public static <T> CompositeFuture join(List<Future<T>> futures) {
        return CompositeFutureImpl.join(futures.toArray(new Future[futures.size()]));
    }

    public static <T> CompositeFuture any(List<Future<T>> futures) {
        return CompositeFutureImpl.any(futures.toArray(new Future[0]));
    }
}

