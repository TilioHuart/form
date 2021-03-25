package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.helpers.FolderExporterZip;
import fr.openent.formulaire.security.ResponseRight;
import fr.openent.formulaire.security.ShareAndOwner;
import fr.openent.formulaire.service.ResponseFileService;
import fr.openent.formulaire.service.impl.DefaultResponseFileService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;

import java.io.*;
import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ResponseFileController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(ResponseFileController.class);
    private ResponseFileService responseFileService;
    private Storage storage;
    private static byte[] BUFFER;

    public ResponseFileController(Storage storage) {
        super();
        this.responseFileService = new DefaultResponseFileService();
        this.storage = storage;
        BUFFER = new byte[4096];
    }

    @Get("/responses/:questionId/files/all")
    @ApiDoc("List all files of a specific response")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void list(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        responseFileService.list(questionId, arrayResponseHandler(request));
    }

    @Get("/responses/:responseId/files")
    @ApiDoc("Get a specific file")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void get(HttpServerRequest request) {
        String responseId = request.getParam("responseId");
        responseFileService.get(responseId, defaultResponseHandler(request));
    }

    @Get("/responses/:responseId/files/download")
    @ApiDoc("Download specific file")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void download(HttpServerRequest request) {
        String responseId = request.getParam("responseId");
        responseFileService.get(responseId, event -> {
            if (event.isRight()) {
                JsonObject file = event.right().getValue();
                String fileId = file.getString("id");
                storage.sendFile(fileId, file.getString("filename"), request, false, new JsonObject());
            } else {
                renderError(request);
            }
        });
    }

    @Get("/responses/:questionId/files/download/zip")
    @ApiDoc("Download all response files of a specific question")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void zipAndDownload(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        responseFileService.list(questionId, event -> {
            if (event.isRight()) {
                List<JsonObject> listFiles = event.right().getValue().getList();
                JsonObject root = new JsonObject().put("type", "folder").put("name", Formulaire.ARCHIVE_ZIP_NAME);

                // Adapt properties for exportAndSendZip functions
                for (JsonObject file : listFiles) {
                    file.put("name", file.getString("filename"));
                    file.put("file", file.getString("id"));
                    file.put("type", "file");
                }

                // Export all files of the 'listFiles' in a folder defined by the 'root' object
                FolderExporterZip zipBuilder = new FolderExporterZip(storage, vertx.fileSystem(), false);
                zipBuilder.exportAndSendZip(root, listFiles, request,false).onComplete(zipEvent -> {
                    if (zipEvent.failed()) {
                        renderError(request);
                    }
                    else {
                        log.info("Zip folder downloaded !");
                    }
                });
            } else {
                renderError(request);
            }
        });
    }

    @Post("/responses/:responseId/files")
    @ApiDoc("Upload a file for a specific response")
    @ResourceFilter(ResponseRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void upload(HttpServerRequest request) {
        storage.writeUploadFile(request, entries -> {
            if (!"ok".equals(entries.getString("status"))) {
                renderError(request);
                return;
            }
            try {
                String responseId = request.getParam("responseId");
                String fileId = entries.getString("_id");
                String name = entries.getJsonObject("metadata").getString("filename");
                String type = entries.getJsonObject("metadata").getString("content-type");

                responseFileService.create(responseId, fileId, name, type, event -> {
                    if (event.isRight()) {
                        JsonObject response = new JsonObject().put("id", fileId).put("filename", name);
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
    @ResourceFilter(ResponseRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
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