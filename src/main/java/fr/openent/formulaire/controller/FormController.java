package fr.openent.formulaire.controller;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.FormService;
import fr.openent.formulaire.service.impl.DefaultFormService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserUtils;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class FormController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(FormController.class);
    private FormService formService;

    public FormController() {
        super();
        this.formService = new DefaultFormService();
    }

    @Get("/forms")
    @ApiDoc("List forms")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void list(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                formService.list(user, arrayResponseHandler(request));
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Post("/forms")
    @ApiDoc("Create a form")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void create(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJson(request, form -> {
                    formService.create(form, user, defaultResponseHandler(request));
                });
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Get("/forms/:id")
    @ApiDoc("Get form thanks to the id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void get(HttpServerRequest request) {
        String id = request.getParam("id");
        RequestUtils.bodyToJson(request, form -> {
            formService.get(id, defaultResponseHandler(request));
        });
    }

    @Put("/forms/:id")
    @ApiDoc("Upate given form")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void update(HttpServerRequest request) {
        String id = request.getParam("id");
        RequestUtils.bodyToJson(request, form -> {
            formService.update(id, form, defaultResponseHandler(request));
        });
    }

    @Delete("/forms/:id")
    @ApiDoc("Delete given form")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void delete(HttpServerRequest request) {
        String id = request.getParam("id");
        formService.delete(id, defaultResponseHandler(request));
    }
}