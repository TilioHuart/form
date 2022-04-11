package fr.openent.formulaire.controllers;

import fr.openent.formulaire.helpers.upload_file.Attachment;
import fr.openent.formulaire.helpers.upload_file.FileHelper;
import fr.openent.formulaire.security.CreationRight;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;

public class UtilsController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(UtilsController.class);
    private final Storage storage;

    public UtilsController(Storage storage) {
        super();
        this.storage = storage;
    }

    @Post("/file/img")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void postImage(final HttpServerRequest request){
        this.storage.writeUploadFile(request, uploaded -> {
            if (!"ok".equals(uploaded.getString("status"))) {
                log.error(uploaded.encode());
                badRequest(request, uploaded.getString("message"));
                return;
            }

            // Format verification (should be an image)
            JsonObject metadata = uploaded.getJsonObject("metadata");
            String contentType = metadata.getString("content-type");

            if (contentType.contains("image")) {
                Renders.renderJson(request, uploaded);
            }
            else {
                badRequest(request, "[Formulaire@postImage] Wrong format file");
            }
        });
    }

    @Post("/file/img/multiple")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void postMultipleImages(final HttpServerRequest request) {
        String nbFiles = request.getHeader("Number-Files");
        int nbFilesToUpload = nbFiles != null ? Integer.parseInt(nbFiles) : 0;
        FileHelper.uploadMultipleFiles(nbFilesToUpload, request, storage, vertx)
            .onSuccess(files -> {
                JsonArray jsonFiles = new JsonArray();
                for (Attachment file : files) {
                    jsonFiles.add(file.toJson());
                }
                renderJson(request, jsonFiles);
            })
            .onFailure(err -> {
                log.error("[Formulaire@postMultipleImages] An error has occurred during upload files: " + err.getMessage());
                renderError(request);
            });
    }
}