package fr.openent.formulaire.controller;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.ResponseService;
import fr.openent.formulaire.service.impl.DefaultResponseService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserUtils;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ResponseController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(ResponseController.class);
    private ResponseService responseService;

    public ResponseController() {
        super();
        this.responseService = new DefaultResponseService();
    }

    @Get("/questions/:id/responses")
    @ApiDoc("List responses")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void list(HttpServerRequest request) {
        String question_id = request.getParam("id");
        responseService.list(question_id, arrayResponseHandler(request));
    }

    @Get("/responses/:id")
    @ApiDoc("Get form thanks to the id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void get(HttpServerRequest request) {
        String id = request.getParam("id");
        responseService.get(id, defaultResponseHandler(request));
    }

    @Post("/questions/:id/responses")
    @ApiDoc("Create a response")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void create(HttpServerRequest request) {
        String question_id = request.getParam("id");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJson(request, response -> {
                    responseService.create(response, user, question_id, defaultResponseHandler(request));
                });
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Put("/responses/:id")
    @ApiDoc("Update given response")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void update(HttpServerRequest request) {
        String id = request.getParam("id");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJson(request, response -> {
                    responseService.update(user, id, response, defaultResponseHandler(request));
                });
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Delete("/responses/:id")
    @ApiDoc("Delete given response")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void delete(HttpServerRequest request) {
        String id = request.getParam("id");
        responseService.delete(id, defaultResponseHandler(request));
    }
}