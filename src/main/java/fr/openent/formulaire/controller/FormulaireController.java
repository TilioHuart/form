package fr.openent.formulaire.controller;

import fr.openent.formulaire.Formulaire;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;

public class FormulaireController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(FormulaireController.class);

    @Get("")
    @ApiDoc("Render view")
    @SecuredAction(value = Formulaire.view, type = ActionType.WORKFLOW)
    public void render(HttpServerRequest request) {
        renderView(request);
    }
}