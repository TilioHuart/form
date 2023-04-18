package fr.openent.formulaire.controllers;

import fr.openent.form.core.enums.ChoiceTypes;
import fr.openent.form.core.models.QuestionChoice;
import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.security.CustomShareAndOwner;
import fr.openent.formulaire.service.QuestionChoiceService;
import fr.openent.formulaire.service.QuestionService;
import fr.openent.formulaire.service.impl.DefaultQuestionChoiceService;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.ShareRights.CONTRIB_RESOURCE_RIGHT;
import static fr.openent.form.helpers.RenderHelper.renderInternalError;
import static fr.openent.form.helpers.UtilsHelper.getIds;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class QuestionChoiceController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(QuestionChoiceController.class);
    private final QuestionChoiceService questionChoiceService;
    private final QuestionService questionService;

    public QuestionChoiceController() {
        super();
        this.questionChoiceService = new DefaultQuestionChoiceService();
        this.questionService = new DefaultQuestionService();
    }

    @Get("/questions/:questionId/choices")
    @ApiDoc("List all the choices of a specific question")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void list(HttpServerRequest request) {
        String questionId = request.getParam(PARAM_QUESTION_ID);
        questionChoiceService.list(questionId, arrayResponseHandler(request));
    }

    @Get("/questions/choices/all")
    @ApiDoc("List all the choices of specific questions")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listChoices(HttpServerRequest request){
        JsonArray questionIds = new JsonArray();
        for (Integer i = 0; i < request.params().size(); i++) {
            questionIds.add(request.getParam(i.toString()));
        }
        if (questionIds.size() <= 0) {
            log.error("[Formulaire@listChoices] No choiceIds to list.");
            noContent(request);
            return;
        }
        questionChoiceService.listChoices(questionIds, arrayResponseHandler(request));
    }

    @Post("/questions/:questionId/choices")
    @ApiDoc("Create a choice for a specific question")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void create(HttpServerRequest request) {
        String questionId = request.getParam(PARAM_QUESTION_ID);
        RequestUtils.bodyToJson(request, choiceJson -> {
            if (choiceJson == null || choiceJson.isEmpty()) {
                log.error("[Formulaire@QuestionChoiceController::create] No choice to create.");
                noContent(request);
                return;
            }

            // Check choice type validity
            if (!choiceJson.getString(TYPE).equals(ChoiceTypes.TXT.getValue())) {
                String message = "[Formulaire@QuestionChoiceController::create] Invalid choice type : " + choiceJson.getString(TYPE);
                log.error(message);
                badRequest(request);
                return;
            }

            QuestionChoice choice = new QuestionChoice(choiceJson);
            String locale = I18n.acceptLanguage(request);

            questionChoiceService.isTargetValid(choice)
                .compose(choiceValidity -> {
                    if (!choiceValidity) {
                        String errorMessage = "[Formulaire@QuestionChoiceController::create] Invalid choice.";
                        return Future.failedFuture(errorMessage);
                    }
                    return questionChoiceService.create(questionId, choice, locale);
                })
                .onSuccess(result -> renderJson(request, result))
                .onFailure(err -> {
                    log.error(err.getMessage());
                    renderError(request);
                });
        });
    }

    @Put("/choices/:choiceId")
    @ApiDoc("Update a specific choice")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void update(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, choiceJson -> {
            if (choiceJson == null || choiceJson.isEmpty()) {
                log.error("[Formulaire@QuestionChoiceController::update] No choice to update.");
                noContent(request);
                return;
            }

            // Check choice type validity
            if (!choiceJson.getString(TYPE).equals(ChoiceTypes.TXT.getValue())) {
                String message = "[Formulaire@QuestionChoiceController::update] Invalid choice type : " + choiceJson.getString(TYPE);
                log.error(message);
                badRequest(request);
                return;
            }

            QuestionChoice choice = new QuestionChoice(choiceJson);
            String locale = I18n.acceptLanguage(request);


            questionChoiceService.isTargetValid(choice)
                .compose(choiceValidity -> {
                    if (!choiceValidity) {
                        String errorMessage = "[Formulaire@QuestionChoiceController::update] Invalid choice.";
                        return Future.failedFuture(errorMessage);
                    }
                    return questionChoiceService.update(choice, locale);
                })
                .onSuccess(result -> renderJson(request, result))
                .onFailure(err -> {
                    log.error(err.getMessage());
                    renderError(request);
                });
        });
    }

    @Delete("/choices/:choiceId")
    @ApiDoc("Delete a specific choice")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void delete(HttpServerRequest request) {
        String choiceId = request.getParam(PARAM_CHOICE_ID);
        questionChoiceService.delete(choiceId)
            .onSuccess(result -> renderJson(request, result))
            .onFailure(err -> {
                log.error(err.getMessage());
                renderError(request);
            });
    }
}