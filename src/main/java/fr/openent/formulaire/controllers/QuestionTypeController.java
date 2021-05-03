package fr.openent.formulaire.controllers;

import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.service.QuestionTypeService;
import fr.openent.formulaire.service.impl.DefaultQuestionTypeService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class QuestionTypeController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(QuestionTypeController.class);
    private QuestionTypeService questionTypeService;

    public QuestionTypeController() {
        super();
        this.questionTypeService = new DefaultQuestionTypeService();
    }

    @Get("/types")
    @ApiDoc("List all questions types")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void list(HttpServerRequest request) {
        questionTypeService.list(arrayResponseHandler(request));
    }

    @Get("/types/:code")
    @ApiDoc("Get a specific question type by code")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(HttpServerRequest request) {
        String code = request.getParam("code");
        questionTypeService.get(code, defaultResponseHandler(request));
    }
}