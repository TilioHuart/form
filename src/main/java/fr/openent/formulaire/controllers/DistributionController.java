package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.helpers.RenderHelper;
import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.security.CreationRight;
import fr.openent.formulaire.security.ResponseRight;
import fr.openent.formulaire.security.ShareAndOwner;
import fr.openent.formulaire.service.DistributionService;
import fr.openent.formulaire.service.FormService;
import fr.openent.formulaire.service.NotifyService;
import fr.openent.formulaire.service.impl.DefaultDistributionService;
import fr.openent.formulaire.service.impl.DefaultFormService;
import fr.openent.formulaire.service.impl.DefaultNotifyService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.UserUtils;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class DistributionController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(DistributionController.class);
    private final NotifyService notifyService;
    private final DistributionService distributionService;
    private final FormService formService;

    public DistributionController(TimelineHelper timelineHelper) {
        super();
        this.notifyService = new DefaultNotifyService(timelineHelper, eb);
        this.distributionService = new DefaultDistributionService();
        this.formService = new DefaultFormService();
    }

    @Get("/distributions")
    @ApiDoc("List all the distributions of the forms sent by me")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listBySender(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
            distributionService.listBySender(user, arrayResponseHandler(request));
        });
    }

    @Get("/distributions/listMine")
    @ApiDoc("List all the distributions of the forms sent to me")
    @ResourceFilter(ResponseRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listByResponder(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
            distributionService.listByResponder(user, arrayResponseHandler(request));
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

    @Get("/distributions/forms/:formId/list/:status")
    @ApiDoc("List all the distributions of a specific form with a specific status")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listByFormAndStatus(HttpServerRequest request) {
        String formId = request.getParam("formId");
        String status = request.getParam("status");
        String nbLines = request.params().get("nbLines");
        distributionService.listByFormAndStatus(formId, status, nbLines, arrayResponseHandler(request));
    }

    @Get("/distributions/forms/:formId/listMine")
    @ApiDoc("List all the distributions for a specific form sent to me")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listByFormAndResponder(HttpServerRequest request) {
        String formId = request.getParam("formId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
            distributionService.listByFormAndResponder(formId, user, arrayResponseHandler(request));
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
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void get(HttpServerRequest request) {
        String formId = request.getParam("formId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
            distributionService.get(formId, user, defaultResponseHandler(request));
        });
    }

    @Post("/distributions/forms/:formId")
    @ApiDoc("Distribute a form to a list of responders")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void create(HttpServerRequest request) {
        String formId = request.getParam("formId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
            RequestUtils.bodyToJsonArray(request, responders -> {
                distributionService.create(formId, user, responders, defaultResponseHandler(request));
            });
        });
    }

    @Post("/distributions/forms/:formId/add")
    @ApiDoc("Create a new distribution based on an already existing one")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void add(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
            RequestUtils.bodyToJson(request, distribution -> {
                distributionService.add(distribution, defaultResponseHandler(request));
            });
        });
    }

    @Put("/distributions/:distributionId")
    @ApiDoc("Update a specific distribution")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void update(HttpServerRequest request) {
        String distributionId = request.getParam("distributionId");
        RequestUtils.bodyToJson(request, distribution -> {
            distributionService.update(distributionId, distribution, updateEvent -> {
                if (updateEvent.isLeft()) {
                    log.error("[Formulaire@updateDistribution] Error in updating distribution " + distributionId);
                    RenderHelper.badRequest(request, updateEvent);
                }
                if (distribution.getString("status").equals(Formulaire.FINISHED)) {
                    String formId = distribution.getInteger("form_id").toString();
                    formService.get(formId, getEvent -> {
                        if (getEvent.isLeft()) {
                            log.error("[Formulaire@updateDistribution] Error in getting form with id " + formId);
                            RenderHelper.badRequest(request, getEvent);
                        }

                        JsonObject form = getEvent.right().getValue();
                        if (form.getBoolean("response_notified")) {
                            formService.listManagers(form.getInteger("id").toString(), listManagersEvent -> {
                                if (listManagersEvent.isLeft()) {
                                    log.error("[Formulaire@updateDistribution] Error in listing managers for form with id " + formId);
                                    RenderHelper.badRequest(request, listManagersEvent);
                                }

                                JsonArray managers = listManagersEvent.right().getValue();
                                JsonArray managerIds = new JsonArray();
                                for (int i = 0; i < managers.size(); i++) {
                                    managerIds.add(managers.getJsonObject(i).getString("id"));
                                }

                                notifyService.notifyResponse(request, form, managerIds);
                                renderJson(request, new JsonObject(), 200);
                            });
                        }
                    });
                }
                renderJson(request, new JsonObject(), 200);
            });
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