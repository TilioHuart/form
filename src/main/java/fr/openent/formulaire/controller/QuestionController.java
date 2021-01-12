package fr.openent.formulaire.controller;

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
        String form_id = request.getParam("formId");
        questionService.list(form_id, arrayResponseHandler(request));
    }

    @Post("/forms/:formId/questions")
    @ApiDoc("Create a question")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void create(HttpServerRequest request) {
        String form_id = request.getParam("formId");
        RequestUtils.bodyToJson(request, question -> {
            questionService.create(question, form_id, defaultResponseHandler(request));
        });
    }

    @Get("/questions/:id")
    @ApiDoc("Get form thanks to the id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void get(HttpServerRequest request) {
        String id = request.getParam("id");
        questionService.get(id, defaultResponseHandler(request));
    }

    @Put("/questions/:id")
    @ApiDoc("Upate given question")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void update(HttpServerRequest request) {
        String id = request.getParam("id");
        RequestUtils.bodyToJson(request, question -> {
            questionService.update(id, question, defaultResponseHandler(request));
        });
    }

    @Delete("/questions/:id")
    @ApiDoc("Delete given question")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void delete(HttpServerRequest request) {
        String id = request.getParam("id");
        questionService.delete(id, defaultResponseHandler(request));
    }
}