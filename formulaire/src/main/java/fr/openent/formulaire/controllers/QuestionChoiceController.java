package fr.openent.formulaire.controllers;

import fr.openent.form.core.enums.ChoiceTypes;
import fr.openent.form.core.models.QuestionChoice;
import fr.openent.form.helpers.FutureHelper;
import fr.openent.form.helpers.IModelHelper;
import fr.openent.formulaire.helpers.ApiVersionHelper;
import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.security.CustomShareAndOwner;
import fr.openent.formulaire.service.QuestionChoiceService;
import fr.openent.formulaire.service.impl.DefaultQuestionChoiceService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.ShareRights.CONTRIB_RESOURCE_RIGHT;
import static fr.openent.form.core.constants.ShareRights.READ_RESOURCE_RIGHT;
import static fr.openent.form.core.enums.ApiVersions.*;
import static fr.openent.form.helpers.RenderHelper.renderInternalError;

public class QuestionChoiceController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(QuestionChoiceController.class);
    private final QuestionChoiceService questionChoiceService;

    public QuestionChoiceController() {
        super();
        this.questionChoiceService = new DefaultQuestionChoiceService();
    }

    @Get("/questions/:questionId/choices")
    @ApiDoc("List all the choices of a specific question")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = READ_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void list(HttpServerRequest request) {
        String questionId = request.getParam(PARAM_QUESTION_ID);
        String apiVersion = RequestUtils.acceptVersion(request);
        boolean shouldAdaptData = !ONE_NINE.isUnderOrEqual(apiVersion);
        questionChoiceService.list(questionId, questionChoicesEvt -> {
            if (questionChoicesEvt.isLeft()) {
                String message = "[Formulaire@QuestionChociceController::list] Failed to list question choices for question with id " + questionId;
                renderInternalError(request, questionChoicesEvt, message);
            }

            JsonArray questionChoices = questionChoicesEvt.right().getValue();
            if (shouldAdaptData) ApiVersionHelper.convertToNextSectionId(questionChoices);
            renderJson(request, questionChoices);
        });
    }

    @Get("/questions/choices/all")
    @ApiDoc("List all the choices of specific questions")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listChoices(HttpServerRequest request){
        JsonArray questionIds = new JsonArray();
        String apiVersion = RequestUtils.acceptVersion(request);
        boolean shouldAdaptData = !ONE_NINE.isUnderOrEqual(apiVersion);
        for (Integer i = 0; i < request.params().size(); i++) {
            questionIds.add(request.getParam(i.toString()));
        }
        if (questionIds.size() <= 0) {
            log.error("[Formulaire@QuestionChociceController::listChoices] No choiceIds to list.");
            noContent(request);
            return;
        }
        questionChoiceService.listChoices(questionIds, questionChoicesEvt -> {
            if (questionChoicesEvt.isLeft()) {
                String message = "[Formulaire@QuestionChociceController::listChoices] Failed to list question choices for questions with ids " + questionIds;
                renderInternalError(request, questionChoicesEvt, message);
            }

            JsonArray questionChoices = questionChoicesEvt.right().getValue();
            if (shouldAdaptData) ApiVersionHelper.convertToNextSectionId(questionChoices);
            renderJson(request, questionChoices);
        });
    }

    @Post("/questions/:questionId/choices")
    @ApiDoc("Create a choice for a specific question")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void create(HttpServerRequest request) {
        String questionId = request.getParam(PARAM_QUESTION_ID);
        String apiVersion = RequestUtils.acceptVersion(request);
        boolean shouldAdaptData = !ONE_NINE.isUnderOrEqual(apiVersion);
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

            if (shouldAdaptData) ApiVersionHelper.convertToNextFormElementId(choiceJson);

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
                .onSuccess(result -> {
                    if (shouldAdaptData) ApiVersionHelper.convertToNextSectionId(result);
                    renderJson(request, result);
                })
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
        String apiVersion = RequestUtils.acceptVersion(request);
        String choiceId = request.getParam(PARAM_CHOICE_ID);
        boolean shouldAdaptData = !ONE_NINE.isUnderOrEqual(apiVersion);
        RequestUtils.bodyToJson(request, choiceJson -> {
            if (choiceJson == null || choiceJson.isEmpty()) {
                log.error("[Formulaire@QuestionChoiceController::update] No choice to update.");
                noContent(request);
                return;
            }

            if (shouldAdaptData) ApiVersionHelper.convertToNextFormElementId(choiceJson);

            QuestionChoice choice = new QuestionChoice(choiceJson);
            String locale = I18n.acceptLanguage(request);

            // Check id validity
            if (!choiceId.equals(choice.getId().toString())) {
                String message = "[Formulaire@QuestionChoiceController::update] Id in URL " + choiceId +
                        " does not match with id in body : " + choice.toJson();
                log.error(message);
                badRequest(request);
                return;
            }

            // Check choice type validity
            if (!ChoiceTypes.TXT.getValue().equals(choice.getType())) {
                String message = "[Formulaire@QuestionChoiceController::update] Invalid choice type : " + choiceJson.getString(TYPE);
                log.error(message);
                badRequest(request);
                return;
            }

            questionChoiceService.isTargetValid(choice)
                .compose(choiceValidity -> {
                    if (!choiceValidity) {
                        String errorMessage = "[Formulaire@QuestionChoiceController::update] Invalid choice.";
                        return Future.failedFuture(errorMessage);
                    }
                    return questionChoiceService.update(choice, locale);
                })
                .onSuccess(result -> {
                    if (shouldAdaptData) ApiVersionHelper.convertToNextSectionId(result);
                    renderJson(request, result);
                })
                .onFailure(err -> {
                    log.error(err.getMessage());
                    renderError(request);
                });
        });
    }

    @Put("/:formId/choices")
    @ApiDoc("Update multiple choices")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void updateMultiple(HttpServerRequest request) {
        RequestUtils.bodyToJsonArray(request, choicesJson -> {
            if (choicesJson == null || choicesJson.isEmpty()) {
                log.error("[Formulaire@QuestionChoiceController::updateMultiple] No choice to update.");
                noContent(request);
                return;
            }

            List<QuestionChoice> choices = IModelHelper.toList(choicesJson, QuestionChoice.class);
            String locale = I18n.acceptLanguage(request);

            // Check choice type validity
            List<QuestionChoice> wrongChoices = choices.stream()
                    .filter(choice -> !choice.getType().equals(ChoiceTypes.TXT.getValue()))
                    .collect(Collectors.toList());
            if (wrongChoices.size() > 0) {
                String errMessage = "[Formulaire@QuestionChoiceController::updateMultiple] Invalid choice type for choices with id : ";
                log.error(errMessage + wrongChoices.stream().map(QuestionChoice::getId).collect(Collectors.toList()));
                badRequest(request);
                return;
            }

            List<Future<Boolean>> futures = new ArrayList<>();
            for (QuestionChoice choice : choices) futures.add(questionChoiceService.isTargetValid(choice));

            FutureHelper.all(futures)
                .compose(choicesValidity -> {
                    boolean hasNotValidChoice = choicesValidity.result().list().stream()
                            .map(Boolean.class::cast)
                            .anyMatch(choiceValidity -> !choiceValidity);
                    if (hasNotValidChoice) {
                        String errorMessage = "[Formulaire@QuestionChoiceController::updateMultiple] Some choices are invalid.";
                        return Future.failedFuture(errorMessage);
                    }
                    return questionChoiceService.update(choices, locale);
                })
                .onSuccess(result -> renderJson(request, new JsonArray(result)))
                .onFailure(err -> {
                    String errMessage = "[Formulaire@QuestionChoiceController::updateMultiple] Failed to update choices " + choices;
                    log.error(errMessage + " : " + err.getMessage());
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