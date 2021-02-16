package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.security.ResponseRight;
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
import org.entcore.common.http.filter.ResourceFilter;
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

    @Get("/questions/:questionId/responses")
    @ApiDoc("List responses")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void list(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        responseService.list(questionId, arrayResponseHandler(request));
    }

    @Get("/questions/:questionId/responses/mine")
    @ApiDoc("List responses")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
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

    @Get("/questions/:questionId/response")
    @ApiDoc("Get form thanks to the id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void get(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                responseService.get(questionId, user, defaultResponseHandler(request));
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Post("/questions/:questionId/responses")
    @ApiDoc("Create a response")
    @SecuredAction(Formulaire.RESPONSE_RIGHT)
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
    @ResourceFilter(ResponseRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
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
    @ResourceFilter(ResponseRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void delete(HttpServerRequest request) {
        String responseId = request.getParam("responseId");
        responseService.delete(responseId, defaultResponseHandler(request));
    }
}