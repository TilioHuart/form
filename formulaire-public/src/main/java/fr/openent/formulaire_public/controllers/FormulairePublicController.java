package fr.openent.formulaire_public.controllers;

import fr.openent.form.core.constants.ConsoleRights;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

public class FormulairePublicController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(FormulairePublicController.class);
    private EventStore eventStore;

    public FormulairePublicController() {
        super();
    }

    @SecuredAction(ConsoleRights.ACCESS_RIGHT)
    public void initSecuredActions(final HttpServerRequest request) {
    }

    @Get("")
    @ApiDoc("Render view")
    public void renderPublic(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            JsonObject context = new JsonObject().put("notLoggedIn", user == null);
            renderView(request, context, "formulaire_public.html", null);
        });
    }

    @Get("/config")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdminFilter.class)
    public void getConfig(final HttpServerRequest request) {
        renderJson(request, config);
    }
}