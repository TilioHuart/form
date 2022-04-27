package fr.openent.formulaire.controllers;

import fr.openent.formulaire.service.QuestionTypeService;
import fr.openent.formulaire.service.impl.DefaultQuestionTypeService;
import fr.wseduc.rs.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class QuestionTypeController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(QuestionTypeController.class);
    private final QuestionTypeService questionTypeService;

    public QuestionTypeController() {
        super();
        this.questionTypeService = new DefaultQuestionTypeService();
    }

    @Get("/types")
    @ApiDoc("List all questions types")
    public void list(HttpServerRequest request) {
        questionTypeService.list(arrayResponseHandler(request));
    }
}