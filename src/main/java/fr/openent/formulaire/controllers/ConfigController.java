package fr.openent.formulaire.controllers;

import fr.openent.formulaire.security.AccessRight;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

public class ConfigController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);

    public ConfigController() {
        super();
    }

    @Get("/config")
    @ApiDoc("Get the config of the module")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(HttpServerRequest request) { Renders.renderJson(request, config, 200); }
}