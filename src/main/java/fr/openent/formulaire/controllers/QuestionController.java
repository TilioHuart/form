package fr.openent.formulaire.controllers;

import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.security.CreationRight;
import fr.openent.formulaire.service.QuestionService;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
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

public class QuestionController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(QuestionController.class);
    private QuestionService questionService;

    public QuestionController() {
        super();
        this.questionService = new DefaultQuestionService();
    }

    @Get("/forms/:formId/questions")
    @ApiDoc("List questions")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void list(HttpServerRequest request) {
        String formId = request.getParam("formId");
        questionService.list(formId, arrayResponseHandler(request));
    }

    @Get("/forms/:formId/questions/count")
    @ApiDoc("Get form thanks to the id")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void countQuestions(HttpServerRequest request) {
        String id = request.getParam("formId");
        questionService.countQuestions(id, defaultResponseHandler(request));
    }

    @Get("/questions/:id")
    @ApiDoc("Get form thanks to the id")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(HttpServerRequest request) {
        String id = request.getParam("id");
        questionService.get(id, defaultResponseHandler(request));
    }

    @Get("/forms/:formId/questions/:position")
    @ApiDoc("Get question in a form by position")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getByPosition(HttpServerRequest request) {
        String formId = request.getParam("formId");
        String position = request.getParam("position");
        questionService.getByPosition(formId, position, defaultResponseHandler(request));
    }

    @Post("/forms/:formId/questions")
    @ApiDoc("Create a question")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void create(HttpServerRequest request) {
        String formId = request.getParam("formId");
        RequestUtils.bodyToJson(request, question -> {
            questionService.create(question, formId, defaultResponseHandler(request));
        });
    }

    @Put("/questions/:id")
    @ApiDoc("Update given question")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void update(HttpServerRequest request) {
        String id = request.getParam("id");
        RequestUtils.bodyToJson(request, question -> {
            questionService.update(id, question, defaultResponseHandler(request));
        });
    }

    @Delete("/questions/:id")
    @ApiDoc("Delete given question")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void delete(HttpServerRequest request) {
        String id = request.getParam("id");
        questionService.delete(id, defaultResponseHandler(request));
    }
}