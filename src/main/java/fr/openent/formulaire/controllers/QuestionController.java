package fr.openent.formulaire.controllers;

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
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void list(HttpServerRequest request) {
        String formId = request.getParam("formId");
        questionService.list(formId, arrayResponseHandler(request));
    }

    @Get("/forms/:formId/questions/count")
    @ApiDoc("Get form thanks to the id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void countQuestions(HttpServerRequest request) {
        String formId = request.getParam("formId");
        questionService.countQuestions(formId, defaultResponseHandler(request));
    }

    @Get("/questions/:questionId")
    @ApiDoc("Get form thanks to the id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void get(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        questionService.get(questionId, defaultResponseHandler(request));
    }

    @Get("/forms/:formId/questions/:position")
    @ApiDoc("Get question in a form by position")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
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

    @Put("/questions/:questionId")
    @ApiDoc("Update given question")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void update(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        RequestUtils.bodyToJson(request, question -> {
            questionService.update(questionId, question, defaultResponseHandler(request));
        });
    }

    @Delete("/questions/:questionId")
    @ApiDoc("Delete given question")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void delete(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        questionService.delete(questionId, defaultResponseHandler(request));
    }
}