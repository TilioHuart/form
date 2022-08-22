package fr.openent.formulaire.controllers;

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

import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.helpers.RenderHelper.renderInternalError;
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
        String formId = request.getParam(PARAM_FORM_ID);
        formElementService.countFormElements(formId, defaultResponseHandler(request));
    }

    @Get("/forms/:formId/elements/:position")
    @ApiDoc("Get a specific form element by position in a specific form")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getByPosition(HttpServerRequest request) {
        String formId = request.getParam(PARAM_FORM_ID);
        String position = request.getParam(POSITION);
        formElementService.getTypeAndIdByPosition(formId, position, formElementEvt -> {
            if (formElementEvt.isLeft()) {
                log.error("[Formulaire@getFormElement] Error in getting form element id of position " + position + " in form " + formId);
                renderInternalError(request, formElementEvt);
                return;
            }
            if (formElementEvt.right().getValue().isEmpty()) {
                String message = "[Formulaire@getFormElement] No form element found of position " + position + " in form " + formId;
                log.error(message);
                notFound(request, message);
                return;
            }

            String elementId = formElementEvt.right().getValue().getLong(ID).toString();
            String elementType = formElementEvt.right().getValue().getString(ELEMENT_TYPE);
            formElementService.getByTypeAndId(elementId, elementType, defaultResponseHandler(request));
        });
    }
}