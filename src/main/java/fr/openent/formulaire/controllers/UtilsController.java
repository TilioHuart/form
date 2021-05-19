package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.security.CreationRight;
import fr.openent.formulaire.security.ShareAndOwner;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserUtils;

public class UtilsController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(UtilsController.class);
    private final Storage storage;

    public UtilsController(Storage storage) {
        super();
        this.storage = storage;
    }

    @Post("/file/img")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value ="", type = ActionType.AUTHENTICATED)
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
}