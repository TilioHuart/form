package fr.openent.formulaire.controllers;

import fr.openent.formulaire.service.DistributionService;
import fr.openent.formulaire.service.impl.DefaultDistributionService;
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
import org.entcore.common.user.UserUtils;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class DistributionController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(DistributionController.class);
    private DistributionService distributionService;

    public DistributionController() {
        super();
        this.distributionService = new DefaultDistributionService();
    }

    @Get("/distributions")
    @ApiDoc("List all the forms sent and created by me")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void list(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                distributionService.list(user, arrayResponseHandler(request));
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Get("/distributions/:id")
    @ApiDoc("Get the info of a distribution thanks to the id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void get(HttpServerRequest request) {
        String id = request.getParam("id");
        distributionService.get(id, defaultResponseHandler(request));
    }

    @Post("/forms/:id/distributions")
    @ApiDoc("Distribute a form to a list of respondents")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void create(HttpServerRequest request) {
        String formId = request.getParam("id");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJsonArray(request, respondents -> {
                    distributionService.create(formId, user, respondents, defaultResponseHandler(request));
                });
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Put("/distributions/:id")
    @ApiDoc("Update given distribution")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void update(HttpServerRequest request) {
        String id = request.getParam("id");
        RequestUtils.bodyToJson(request, distribution -> {
            distributionService.update(id, distribution, defaultResponseHandler(request));
        });
    }

    @Delete("/distributions/:id")
    @ApiDoc("Delete given distribution")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void delete(HttpServerRequest request) {
        String id = request.getParam("id");
        distributionService.delete(id, defaultResponseHandler(request));
    }
}