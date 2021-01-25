package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.security.CreationRight;
import fr.openent.formulaire.service.FormService;
import fr.openent.formulaire.service.impl.DefaultFormService;
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
    @ApiDoc("List all the forms created by me")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
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

    @Get("/forms/:id")
    @ApiDoc("Get form thanks to the id")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(HttpServerRequest request) {
        String id = request.getParam("id");
        formService.get(id, defaultResponseHandler(request));
    }

    @Put("/forms/:id")
    @ApiDoc("Update given form")
    @SecuredAction(Formulaire.CREATION_RIGHT)
    public void update(HttpServerRequest request) {
        String id = request.getParam("id");
        RequestUtils.bodyToJson(request, form -> {
            formService.update(id, form, defaultResponseHandler(request));
        });
    }

    @Post("/forms")
    @ApiDoc("Create a form")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
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

    @Delete("/forms/:id")
    @ApiDoc("Delete given form")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void delete(HttpServerRequest request) {
        String id = request.getParam("id");
        formService.delete(id, defaultResponseHandler(request));
    }
}