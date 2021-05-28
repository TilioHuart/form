package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.helpers.FolderExporterZip;
import fr.openent.formulaire.security.AccessRight;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ResponseFileController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(ResponseFileController.class);
    private final ResponseFileService responseFileService;
    private final Storage storage;
    private final SimpleDateFormat dateGetter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("ddMMyyyy_HH'h'mm_");

    public ResponseFileController(Storage storage) {
        super();
        this.responseFileService = new DefaultResponseFileService();
        this.storage = storage;
        dateFormatter.setTimeZone(TimeZone.getTimeZone("Europe/Paris")); // TODO to adapt for not France timezone
    }

    @Get("/responses/:responseId/files/all")
    @ApiDoc("List all files of a specific response")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void list(HttpServerRequest request) {
        String responseId = request.getParam("responseId");
        responseFileService.list(responseId, arrayResponseHandler(request));
    }

    @Get("/questions/:questionId/files/all")
    @ApiDoc("List all files of a specific question")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listByQuestion(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        responseFileService.listByQuestion(questionId, arrayResponseHandler(request));
    }

    @Get("/responses/files/:fileId")
    @ApiDoc("Get a specific file by id")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void get(HttpServerRequest request) {
        String fileId = request.getParam("fileId");
        responseFileService.get(fileId, defaultResponseHandler(request));
    }

    @Get("/responses/files/:fileId/download")
    @ApiDoc("Download a specific file")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void download(HttpServerRequest request) {
        String fileId = request.getParam("fileId");
        responseFileService.get(fileId, event -> {
            if (event.isRight()) {
                JsonObject file = event.right().getValue();
                String id = file.getString("id");
                storage.sendFile(id, file.getString("filename"), request, false, new JsonObject());
            } else {
                renderError(request);
            }
        });
    }

    @Get("/responses/:questionId/files/download/zip")
    @ApiDoc("Download all files of a specific question")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void zipAndDownload(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        responseFileService.listByQuestion(questionId, event -> {
            if (event.isRight()) {
                List<JsonObject> listFiles = event.right().getValue().getList();
                JsonObject root = new JsonObject()
                        .put("id", UUID.randomUUID().toString())
                        .put("type", "folder")
                        .put("name", Formulaire.ARCHIVE_ZIP_NAME)
                        .put("folders", new JsonArray());
                JsonObject groupFiles = new JsonObject();

                // Adapt properties for exportAndSendZip functions
                for (JsonObject file : listFiles) {
                    String displayDate = "";
                    try {
                        displayDate = dateFormatter.format(dateGetter.parse(file.getString("date_response")));
                    }
                    catch (ParseException e) { e.printStackTrace(); }

                    file.put("name", displayDate + file.getString("filename"));
                    file.put("file", file.getString("id"));
                    file.put("type", "file");

                    // Sort files by response_id in the groupFiles array (to add folder parents after)
                    String responseId = file.getInteger("response_id").toString();
                    if (!groupFiles.containsKey(responseId)) {
                        groupFiles.put(responseId, new JsonArray());
                    }
                    groupFiles.getJsonArray(responseId).add(file);
                }

                // Folder management
                for (String key : groupFiles.getMap().keySet()) {
                    JsonArray groupFile = groupFiles.getJsonArray(key);
                    int nbFilesInGroup = groupFile.size();
                    if (nbFilesInGroup > 1) {
                        JsonObject folder = new JsonObject()
                                .put("id", UUID.randomUUID().toString())
                                .put("type", "folder")
                                .put("parent", root.getString("id"))
                                .put("name", getFolderName(groupFile.getJsonObject(0)));
                        root.getJsonArray("folders").add(folder);
                        listFiles.add(folder);

                        for (int i = 0; i < nbFilesInGroup; i++) {
                            groupFile.getJsonObject(i).put("parent", folder.getString("id"));
                        }
                    }
                }

                // Export all files of the 'listFiles' in a folder defined by the 'root' object
                FolderExporterZip zipBuilder = new FolderExporterZip(storage, vertx.fileSystem(), false);
                zipBuilder.exportAndSendZip(root, listFiles, request,false).onComplete(zipEvent -> {
                    if (zipEvent.failed()) {
                        log.error("[Formulaire@zipAndDownload] Fail to zip and export files");
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

    private String getFolderName(JsonObject file) {
        String[] responderNames = file.getString("responder_name").split(" ");
        Collections.reverse(Arrays.asList(responderNames));

        String responderName = String.join("", responderNames);
        String filename = file.getString("filename");
        String completeName = file.getString("name");
        int indexOfUnderscore = filename.contains("_") ? filename.indexOf("_") : 0;
        boolean isAnonymous = !filename.substring(0, indexOfUnderscore).equals(responderName);

        return isAnonymous ? completeName.substring(0, 14) : completeName.substring(0, 15) + responderName;
    }

    @Post("/responses/:responseId/files")
    @ApiDoc("Upload files of a specific response")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void upload(HttpServerRequest request) {
        storage.writeUploadFile(request, entries -> {
            if (!"ok".equals(entries.getString("status"))) {
                log.error("[Formulaire@upload] Fail to create file in storage");
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
                        log.error("[Formulaire@upload] Fail to create in database file " + fileId);
                        deleteFiles(new JsonArray().add(fileId));
                        renderError(request);
                    }
                });
            } catch (NumberFormatException e) {
                renderError(request);
            }
        });
    }

    @Delete("/responses/:responseId/files")
    @ApiDoc("Delete all files of a specific response")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void deleteAll(HttpServerRequest request) {
        String responseId = request.getParam("responseId");

        responseFileService.deleteAll(responseId, deleteEvent -> {
            if (deleteEvent.isRight()) {
                JsonArray deletedFiles = deleteEvent.right().getValue();
                request.response().setStatusCode(204).end();
                if (!deletedFiles.isEmpty()) {
                    deleteFiles(deletedFiles);
                }
            } else {
                log.error("[Formulaire@deleteAll] An error occurred while deleting files for reponse " + responseId);
                renderError(request);
            }
        });
    }

    private void deleteFiles(JsonArray fileIds) {
        storage.removeFiles(fileIds, e -> {
            if (!"ok".equals(e.getString("status"))) {
                log.error("[Formulaire@deleteFile] An error occurred while removing files " + fileIds);
            }
        });
    }
}