package fr.openent.formulaire.controllers;

import fr.openent.formulaire.service.ResponseFileService;
import fr.openent.formulaire.service.impl.DefaultResponseFileService;
import fr.wseduc.rs.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.storage.Storage;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ResponseFileController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(ResponseFileController.class);
    private ResponseFileService responseFileService;
    private Storage storage;

    public ResponseFileController(Storage storage) {
        super();
        this.responseFileService = new DefaultResponseFileService();
        this.storage = storage;
    }

    @Get("/responses/:responseId/files")
    @ApiDoc("Get a specific file")
    public void get(HttpServerRequest request) {
        String responseId = request.getParam("responseId");
        responseFileService.get(responseId, defaultResponseHandler(request));
    }

    @Get("/responses/:responseId/files/download")
    @ApiDoc("Download specific file")
    public void download(HttpServerRequest request) {
        String responseId = request.getParam("responseId");
        responseFileService.get(responseId, event -> {
            if (event.isRight()) {
                JsonObject file = event.right().getValue();
                String fileId = file.getString("id");
                storage.sendFile(fileId, file.getString("filename"), request, false, new JsonObject());
            } else {
                notFound(request);
            }
        });
    }

    @Post("/responses/:responseId/files")
    @ApiDoc("Upload a file for a specific response")
    public void upload(HttpServerRequest request) {
        storage.writeUploadFile(request, entries -> {
            if (!"ok".equals(entries.getString("status"))) {
                renderError(request);
                return;
            }
            try {
                String responseId = request.getParam("responseId");
                String fileId = entries.getString("_id");
                String filename = entries.getJsonObject("metadata").getString("filename");
                String type = entries.getJsonObject("metadata").getString("content-type");

                responseFileService.create(responseId, fileId, filename, type, event -> {
                    if (event.isRight()) {
                        JsonObject response = new JsonObject().put("id", fileId).put("filename", filename);
                        request.response().setStatusCode(201).putHeader("Content-Type", type).end(response.toString());
                    } else {
                        deleteFile(fileId);
                        renderError(request);
                    }
                });
            } catch (NumberFormatException e) {
                renderError(request);
            }
        });
    }

    @Delete("/responses/:responseId/files")
    @ApiDoc("Delete file from basket")
    public void delete(HttpServerRequest request) {
        String responseId = request.getParam("responseId");

        responseFileService.delete(responseId, deleteEvent -> {
            if (deleteEvent.isRight()) {
                JsonObject deletedFile = deleteEvent.right().getValue();
                request.response().setStatusCode(204).end();
                if (!deletedFile.isEmpty()) {
                    deleteFile(deletedFile.getString("id"));
                }
            } else {
                renderError(request);
            }
        });
    }

    private void deleteFile(String fileId) {
        storage.removeFile(fileId, e -> {
            if (!"ok".equals(e.getString("status"))) {
                log.error("[Formulaire@deleteFile] An error occurred while removing " + fileId + " file.");
            }
        });
    }
}