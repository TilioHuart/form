package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.security.ResponseRight;
import fr.openent.formulaire.security.ShareAndOwner;
import fr.openent.formulaire.service.ResponseService;
import fr.openent.formulaire.service.impl.DefaultResponseService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserUtils;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ResponseController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(ResponseController.class);
    private ResponseService responseService;
    private Storage storage;

    public ResponseController(Storage storage) {
        super();
        this.responseService = new DefaultResponseService();
        this.storage = storage;
    }

    @Get("/questions/:questionId/responses")
    @ApiDoc("List all the responses for a question")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void list(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        responseService.list(questionId, arrayResponseHandler(request));
    }

    @Get("/questions/:questionId/responses/mine")
    @ApiDoc("List all my responses for a question")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void listMine(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                responseService.listMine(questionId, user, arrayResponseHandler(request));
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Get("/responses/:responseId")
    @ApiDoc("Get response thanks to the id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void get(HttpServerRequest request) {
        String responseId = request.getParam("responseId");
        responseService.get(responseId, defaultResponseHandler(request));
    }

    @Post("/questions/:questionId/responses")
    @ApiDoc("Create a response")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void create(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJson(request, response -> {
                    responseService.create(response, user, questionId, defaultResponseHandler(request));
                });
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Put("/responses/:responseId")
    @ApiDoc("Update given response")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void update(HttpServerRequest request) {
        String responseId = request.getParam("responseId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJson(request, response -> {
                    responseService.update(user, responseId, response, defaultResponseHandler(request));
                });
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Delete("/responses/:responseId")
    @ApiDoc("Delete given response")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void delete(HttpServerRequest request) {
        String responseId = request.getParam("responseId");
        responseService.delete(responseId, defaultResponseHandler(request));
    }

    // File repository

    @Get("/responses/:responseId/files/:fileId")
    @ApiDoc("Download specific file")
    public void getFile(HttpServerRequest request) {
        String responseId = request.getParam("responseId");
        String fileId = request.getParam("fileId");
        responseService.getFile(responseId, fileId, event -> {
            if (event.isRight()) {
                JsonObject file = event.right().getValue();
                storage.sendFile(fileId, file.getString("filename"), request, false, new JsonObject());
            } else {
                notFound(request);
            }
        });
    }

    @Post("/responses/:responseId/files")
    @ApiDoc("Upload a file for a specific response")
    public void uploadFile(HttpServerRequest request) {
        storage.writeUploadFile(request, entries -> {
            if (!"ok".equals(entries.getString("status"))) {
                renderError(request);
                return;
            }
            try {
                String responseId = request.getParam("responseId");
                String fileId = entries.getString("_id");
                String filename = entries.getJsonObject("metadata").getString("filename");
                String contentType = entries.getJsonObject("metadata").getString("content-type");

                responseService.createFile(responseId, fileId, filename, event -> {
                    if (event.isRight()) {
                        JsonObject response = new JsonObject().put("id", fileId).put("filename", filename);
                        request.response().setStatusCode(201).putHeader("Content-Type", contentType).end(response.toString());
                    } else {
                        deleteFileFromStorage(fileId);
                        renderError(request);
                    }
                });
            } catch (NumberFormatException e) {
                renderError(request);
            }
        });
    }

    @Delete("/responses/:responseId/files/:fileId")
    @ApiDoc("Delete file from basket")
    public void deleteFile(HttpServerRequest request) {
        String responseId = request.getParam("responseId");
        String fileId = request.getParam("fileId");

        responseService.deleteFile(responseId, fileId, event -> {
            if (event.isRight()) {
                request.response().setStatusCode(204).end();
                deleteFileFromStorage(fileId);
            } else {
                renderError(request);
            }
        });
    }

    private void deleteFileFromStorage(String fileId) {
        storage.removeFile(fileId, e -> {
            if (!"ok".equals(e.getString("status"))) {
                log.error("[Formulaire@deleteFile] An error occurred while removing " + fileId + " file.");
            }
        });
    }
}