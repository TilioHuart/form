package fr.openent.formulaire.controllers;

import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.security.CreationRight;
import fr.openent.formulaire.service.QuestionChoiceService;
import fr.openent.formulaire.service.impl.DefaultQuestionChoiceService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class QuestionChoiceController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(QuestionChoiceController.class);
    private QuestionChoiceService questionChoiceService;

    public QuestionChoiceController() {
        super();
        this.questionChoiceService = new DefaultQuestionChoiceService();
    }

    @Get("/questions/:questionId/choices")
    @ApiDoc("List all the choices of a specific question")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void list(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        questionChoiceService.list(questionId, arrayResponseHandler(request));
    }

    @Get("/choices/:choiceId")
    @ApiDoc("Get a specific by id")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(HttpServerRequest request) {
        String choiceId = request.getParam("choiceId");
        questionChoiceService.get(choiceId, defaultResponseHandler(request));
    }

    @Post("/questions/:questionId/choices")
    @ApiDoc("Create a choice for a specific question")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void create(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        RequestUtils.bodyToJson(request, choice -> {
            questionChoiceService.create(questionId, choice, defaultResponseHandler(request));
        });
    }

    @Post("/questions/:questionId/choices/multiple")
    @ApiDoc("Create several choices for a specific question")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void createMultiple(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        RequestUtils.bodyToJsonArray(request, choices -> {
            questionChoiceService.createMultiple(choices, questionId, arrayResponseHandler(request));
        });
    }

    @Put("/choices/:choiceId")
    @ApiDoc("Update a specific choice")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void update(HttpServerRequest request) {
        String choiceId = request.getParam("choiceId");
        RequestUtils.bodyToJson(request, choice -> {
            questionChoiceService.update(choiceId, choice, defaultResponseHandler(request));
        });
    }

    @Delete("/choices/:choiceId")
    @ApiDoc("Delete a specific choice")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void delete(HttpServerRequest request) {
        String choiceId = request.getParam("choiceId");
        questionChoiceService.delete(choiceId, defaultResponseHandler(request));
    }
}