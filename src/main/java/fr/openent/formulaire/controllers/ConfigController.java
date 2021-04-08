package fr.openent.formulaire.controllers;

import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;

public class ConfigController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);

    public ConfigController() {
        super();
    }

    @Get("/config")
    @ApiDoc("Get the config of the module")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void get(HttpServerRequest request) { Renders.renderJson(request, config, 200); }
}