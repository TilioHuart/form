package fr.openent.formulaire.controller;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

public class FormulaireController extends ControllerHelper {

    @Get("")
    @ApiDoc("Render view")
    @SecuredAction(value = "formulaire.view", type = ActionType.WORKFLOW)
    public void render(HttpServerRequest request) {
        renderView(request);
    }
}