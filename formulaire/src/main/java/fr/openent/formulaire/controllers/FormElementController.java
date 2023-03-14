package fr.openent.formulaire.controllers;

import fr.openent.form.core.models.FormElement;
import fr.openent.form.helpers.UtilsHelper;
import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.security.CustomShareAndOwner;
import fr.openent.formulaire.service.FormElementService;
import fr.openent.formulaire.service.impl.DefaultFormElementService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.ShareRights.CONTRIB_RESOURCE_RIGHT;
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

    @Put("/forms/:formId/elements")
    @ApiDoc("Update a list of formElements")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void update(HttpServerRequest request) {
        String formId = request.getParam(PARAM_FORM_ID);

        RequestUtils.bodyToJsonArray(request, formElementsJson -> {
            if (formElementsJson == null || formElementsJson.isEmpty()) {
                log.error("[Formulaire@FormElementController::update] No formElements to update.");
                noContent(request);
                return;
            }

            // Check doubles inside
            JsonArray positions = UtilsHelper.getByProp(formElementsJson, POSITION);
            Set<Object> doubles = positions.stream()
                    .filter(i -> Collections.frequency(positions.getList(), i) > 1)
                    .collect(Collectors.toSet());
            if (doubles.size() > 0) {
                String message = "[Formulaire@FormElementController::update] Position(s) " + doubles + " are/is used by several questions.";
                log.error(message);
                renderError(request);
                return;
            }

            List<FormElement> formElements = FormElement.toListFormElements(formElementsJson);
            formElementService.update(formElements, formId)
                .onSuccess(result -> renderJson(request, result))
                .onFailure(err -> {
                    log.error("[Formulaire@FormElementController::update] Failed to update form elements " + formElements + " : " + err.getMessage());
                    renderError(request);
                });
        });
    }
}