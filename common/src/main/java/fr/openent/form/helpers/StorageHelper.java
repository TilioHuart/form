package fr.openent.form.helpers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.Storage;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

import static fr.openent.form.core.constants.Fields.*;

public class StorageHelper {
    private static final Logger log = LoggerFactory.getLogger(StorageHelper.class);

    private StorageHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Send file to a specific path
     * @param storage           Storage instance
     * @param exportId          File identifier of the file to send
     * @param fileName          Name of the file downloaded
     * @param request           Current request instance
     * @param inline
     * @param metadata          Additional metadata for the file
     * @return                  {@link Future<Void>}
     */
    public static Future<Void> sendFile(Storage storage, String exportId, String fileName, HttpServerRequest request, boolean inline, JsonObject metadata) {
        Promise<Void> promise = Promise.promise();
        if (exportId == null || exportId.isEmpty()) {
            promise.complete();
        }
        else {
            String message = "[Formulaire@sendFile] Failed to send file : ";
            storage.sendFile(exportId, fileName, request, inline, metadata, FutureHelper.handlerAsyncResult(promise, message));
        }
        return promise.future();
    }

    /**
     * Remove files by ids
     * @param storage           Storage instance
     * @param fileIds           List of file identifiers to send
     * @return                  {@link Future<JsonObject>}
     */
    public static Future<JsonObject> removeFiles(Storage storage, List<String> fileIds) {
        Promise<JsonObject> promise = Promise.promise();
        if (fileIds.isEmpty()) {
            promise.complete(new JsonObject().put("remove file status", OK));
        } else {
            storage.removeFiles(new JsonArray(fileIds), result -> {
                if (!result.getString(STATUS).equals(OK)) {
                    String message = "[Formulaire@removeFiles] Failed to remove files : " + result.getString(MESSAGE);
                    log.error(message);
                    promise.fail(message);
                    return;
                }
                promise.complete(result);
            });
        }
        return promise.future();
    }

    /**
     * Remove file by id
     * @param storage           Storage instance
     * @param fileId            file identifier to send
     * @return                  {@link Future<JsonObject>}
     */
    public static Future<JsonObject> removeFile(Storage storage, String fileId) {
        Promise<JsonObject> promise = Promise.promise();
        storage.removeFile(fileId, result -> {
            if (!result.getString(STATUS).equals(OK)) {
                String message = "[Formulaire@removeFile] Failed to remove file : " + result.getString(MESSAGE);
                log.error(message);
                promise.fail(message);
            } else {
                promise.complete(result);
            }
        });
        return promise.future();
    }

    /**
     * Remove a directory based on its path
     * @param fileSystem            file system
     * @param directoryPath         directory path
     * @return                      {@link Future<Void>}
     */
    public static Future<Void> removeDirectory(FileSystem fileSystem, String directoryPath) {
        Promise<Void> promise = Promise.promise();
        fileSystem.deleteRecursive(directoryPath, true, res -> {
            if (res.failed()) {
                String message = "[Formulaire@emoveDirectory] Failed to remove directory.";
                promise.fail(message);
            } else {
                promise.complete(res.result());
            }
        });
        return promise.future();
    }

    /**
     * Check if a file exists based on its id
     * @param storage           list of file identifiers
     * @param fileId            list of file names for each id
     * @return                  {@link Future<Boolean>} true if exist, false if none
     */
    public static Future<Boolean> exist(Storage storage, String fileId) {
        Promise<Boolean> promise = Promise.promise();
        storage.readFile(fileId, result -> {
            if (result == null) {
                promise.complete(false);
                return;
            }
            promise.complete(true);
        });
        return promise.future();
    }
}