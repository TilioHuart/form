package fr.openent.formulaire.controllers;

import fr.openent.formulaire.helpers.RenderHelper;
import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.service.FormElementService;
import fr.openent.formulaire.service.impl.DefaultFormElementService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class FormElementController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(FormElementController.class);
    private final FormElementService formElementService;

    public FormElementController() {
        super();
        this.formElementService = new DefaultFormElementService();
    }

    @Get("/forms/:formId/elements/count")
    @ApiDoc("Count the number of form elements in a specific form")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void countFormElements(HttpServerRequest request) {
        String formId = request.getParam("formId");
        formElementService.countFormElements(formId, defaultResponseHandler(request));
    }

    @Get("/forms/:formId/elements/:position")
    @ApiDoc("Get a specific form element by position in a specific form")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getByPosition(HttpServerRequest request) {
        String formId = request.getParam("formId");
        String position = request.getParam("position");
        formElementService.getIdByPosition(formId, position, event -> {
            if (event.isLeft()) {
                log.error("[Formulaire@getByPosition] Error in getting form element id of position " + position + " for form " + formId);
                RenderHelper.badRequest(request, event);
            }

            String elementId = event.right().getValue().getLong("id").toString();
            String elementType = event.right().getValue().getString("element_type");
            formElementService.getByTypeAndId(elementId, elementType, defaultResponseHandler(request));
        });
    }
}