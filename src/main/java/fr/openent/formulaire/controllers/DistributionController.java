package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.security.CreationRight;
import fr.openent.formulaire.security.ShareAndOwner;
import fr.openent.formulaire.service.DistributionService;
import fr.openent.formulaire.service.impl.DefaultDistributionService;
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

public class DistributionController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(DistributionController.class);
    private final DistributionService distributionService;

    public DistributionController() {
        super();
        this.distributionService = new DefaultDistributionService();
    }

    @Get("/distributions")
    @ApiDoc("List all the distributions of the forms sent by me")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listBySender(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                distributionService.listBySender(user, arrayResponseHandler(request));
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Get("/distributions/listMine")
    @ApiDoc("List all the distributions of the forms sent to me")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listByResponder(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                distributionService.listByResponder(user, arrayResponseHandler(request));
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Get("/distributions/forms/:formId/list")
    @ApiDoc("List all the distributions of a specific form")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listByForm(HttpServerRequest request) {
        String formId = request.getParam("formId");
        distributionService.listByForm(formId, arrayResponseHandler(request));
    }

    @Get("/distributions/forms/:formId/listMine")
    @ApiDoc("List all the distributions for a specific form sent to me")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listByFormAndResponder(HttpServerRequest request) {
        String formId = request.getParam("formId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                distributionService.listByFormAndResponder(formId, user, arrayResponseHandler(request));
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Get("/distributions/forms/:formId/count")
    @ApiDoc("Get the number of distributions for a specific form")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void count(HttpServerRequest request) {
        String formId = request.getParam("formId");
        distributionService.count(formId, defaultResponseHandler(request));
    }

    @Get("/distributions/forms/:formId")
    @ApiDoc("Get a specific distribution by form, responder and status")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(HttpServerRequest request) {
        String formId = request.getParam("formId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                distributionService.get(formId, user, defaultResponseHandler(request));
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Post("/distributions/forms/:formId")
    @ApiDoc("Distribute a form to a list of responders")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void create(HttpServerRequest request) {
        String formId = request.getParam("formId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJsonArray(request, responders -> {
                    distributionService.create(formId, user, responders, defaultResponseHandler(request));
                });
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Post("/distributions")
    @ApiDoc("Create a new distribution based on an already existing one")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void add(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            RequestUtils.bodyToJson(request, distribution -> {
                distributionService.add(distribution, defaultResponseHandler(request));
            });
        });
    }

    @Put("/distributions/:distributionId")
    @ApiDoc("Update a specific distribution")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void update(HttpServerRequest request) {
        String distributionId = request.getParam("distributionId");
        RequestUtils.bodyToJson(request, distribution -> {
            distributionService.update(distributionId, distribution, defaultResponseHandler(request));
        });
    }

    @Delete("/distributions/:distributionId")
    @ApiDoc("Delete a specific distribution")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void delete(HttpServerRequest request) {
        String distributionId = request.getParam("distributionId");
        distributionService.delete(distributionId, defaultResponseHandler(request));
    }
}