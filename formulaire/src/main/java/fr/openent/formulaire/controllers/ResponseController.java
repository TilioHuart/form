package fr.openent.formulaire.controllers;

import fr.openent.form.core.constants.Constants;
import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.security.CustomShareAndOwner;
import fr.openent.formulaire.service.DistributionService;
import fr.openent.formulaire.service.QuestionChoiceService;
import fr.openent.formulaire.service.QuestionService;
import fr.openent.formulaire.service.ResponseService;
import fr.openent.formulaire.service.impl.DefaultDistributionService;
import fr.openent.formulaire.service.impl.DefaultQuestionChoiceService;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
import fr.openent.formulaire.service.impl.DefaultResponseService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import static fr.openent.form.core.constants.Constants.CHOICES_TYPE_QUESTIONS;
import static fr.openent.form.core.constants.Constants.QUESTIONS_WITHOUT_RESPONSES;
import static fr.openent.form.core.constants.DistributionStatus.FINISHED;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.ShareRights.CONTRIB_RESOURCE_RIGHT;
import static fr.openent.form.core.constants.ShareRights.RESPONDER_RESOURCE_RIGHT;
import static fr.openent.form.helpers.RenderHelper.*;
import static fr.openent.form.helpers.UtilsHelper.getIds;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ResponseController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(ResponseController.class);
    private final ResponseService responseService;
    private final QuestionService questionService;
    private final QuestionChoiceService questionChoiceService;
    private final DistributionService distributionService;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");

    public ResponseController() {
        super();
        this.responseService = new DefaultResponseService();
        this.questionService = new DefaultQuestionService();
        this.questionChoiceService = new DefaultQuestionChoiceService();
        this.distributionService = new DefaultDistributionService();
    }

    @Get("/questions/:questionId/responses")
    @ApiDoc("List all the responses to a specific question")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void list(HttpServerRequest request) {
        String questionId = request.getParam(PARAM_QUESTION_ID);
        String nbLines = request.params().get(PARAM_NB_LINES);
        String formId = request.params().get(PARAM_FORM_ID);

        distributionService.listByFormAndStatus(formId, FINISHED, nbLines, getDistribsEvent -> {
            if (getDistribsEvent.isLeft()) {
                log.error("[Formulaire@listResponse] Fail to list finished distributions for form wih id : " + formId);
                renderInternalError(request, getDistribsEvent);
                return;
            }
            if (getDistribsEvent.right().getValue().isEmpty()) {
                String message = "[Formulaire@listResponse] No distribution found for form with id " + formId;
                log.error(message);
                notFound(request, message);
                return;
            }

            responseService.list(questionId, nbLines, getDistribsEvent.right().getValue(), arrayResponseHandler(request));
        });
    }

    @Get("/questions/:questionId/distributions/:distributionId/responses")
    @ApiDoc("List all my responses to a specific question for a specific distribution")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void listMineByDistribution(HttpServerRequest request) {
        String questionId = request.getParam(PARAM_QUESTION_ID);
        String distributionId = request.getParam(PARAM_DISTRIBUTION_ID);
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                String message = "[Formulaire@listMineByDistribution] User not found in session.";
                log.error(message);
                unauthorized(request, message);
                return;
            }
            responseService.listMineByDistribution(questionId, distributionId, user, arrayResponseHandler(request));
        });
    }

    @Get("/distributions/:distributionId/responses")
    @ApiDoc("List all responses for a specific distribution")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void listByDistribution(HttpServerRequest request) {
        String distributionId = request.getParam(PARAM_DISTRIBUTION_ID);
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                String message = "[Formulaire@listByDistribution] User not found in session.";
                log.error(message);
                unauthorized(request, message);
                return;
            }
            responseService.listByDistribution(distributionId, arrayResponseHandler(request));
        });
    }

    @Get("/forms/:formId/responses")
    @ApiDoc("List all the responses to a specific form")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void listByForm(HttpServerRequest request) {
        String formId = request.params().get(PARAM_FORM_ID);
        responseService.listByForm(formId, arrayResponseHandler(request));
    }

    @Get("/responses/count")
    @ApiDoc("Count responses by questionId")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void countByQuestions(HttpServerRequest request){
        JsonArray questionIds = new JsonArray();
        for (Integer i = 0; i < request.params().size(); i++) {
            questionIds.add(request.getParam(i.toString()));
        }
        if (questionIds.size() <= 0) {
            log.error("[Formulaire@countByQuestions] No questionIds to count.");
            noContent(request);
            return;
        }
        responseService.countByQuestions(questionIds, defaultResponseHandler(request));
    }

    @Post("/questions/:questionId/responses")
    @ApiDoc("Create a response")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void create(HttpServerRequest request) {
        String questionId = request.getParam(PARAM_QUESTION_ID);
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                String message = "[Formulaire@createResponse] User not found in session.";
                log.error(message);
                unauthorized(request, message);
                return;
            }

            RequestUtils.bodyToJson(request, response -> {
                if (response == null || response.isEmpty()) {
                    log.error("[Formulaire@createResponse] No response to create.");
                    noContent(request);
                    return;
                }

                questionService.get(questionId, questionEvt -> {
                    if (questionEvt.isLeft()) {
                        log.error("[Formulaire@createResponse] Fail to get question corresponding to id : " + questionId);
                        renderInternalError(request, questionEvt);
                        return;
                    }
                    if (questionEvt.right().getValue().isEmpty()) {
                        String message = "[Formulaire@createResponse] No question found for id " + questionId;
                        log.error(message);
                        notFound(request, message);
                        return;
                    }

                    JsonObject question = questionEvt.right().getValue();
                    int question_type = question.getInteger(QUESTION_TYPE);
                    Integer choice_id = response.getInteger(CHOICE_ID);

                    // Check if it's a question type you can respond to
                    if (QUESTIONS_WITHOUT_RESPONSES.contains(question_type)) {
                        String message = "[Formulaire@createResponse] You cannot create a response for a question of type " + question_type;
                        log.error(message);
                        badRequest(request, message);
                        return;
                    }

                    // If there is a choice it should match an existing QuestionChoice for this question
                    if (choice_id != null && CHOICES_TYPE_QUESTIONS.contains(question_type)) {
                        questionChoiceService.get(choice_id.toString(), choiceEvt -> {
                            if (choiceEvt.isLeft()) {
                                log.error("[Formulaire@createResponse] Fail to get question choice corresponding to id : " + choice_id);
                                renderInternalError(request, choiceEvt);
                                return;
                            }
                            if (choiceEvt.right().getValue().isEmpty()) {
                                String message = "[Formulaire@createResponse] No choice found for id " + choice_id;
                                log.error(message);
                                notFound(request, message);
                                return;
                            }

                            JsonObject choice = choiceEvt.right().getValue();

                            // Check choice validity
                            if (question.getInteger(MATRIX_ID) != null &&
                                (!choice.getInteger(QUESTION_ID).equals(question.getInteger(MATRIX_ID)) ||
                                !choice.getString(VALUE).equals(response.getString(ANSWER)))) {
                                String message ="[Formulaire@createResponse] Wrong choice for response " + response;
                                log.error(message);
                                badRequest(request, message);
                                return;

                            }
                            else if (question.getInteger(MATRIX_ID) == null &&
                                    (!choice.getInteger(QUESTION_ID).toString().equals(questionId) ||
                                    !choice.getString(VALUE).equals(response.getString(ANSWER)))) {
                                String message ="[Formulaire@createResponse] Wrong choice for response " + response;
                                log.error(message);
                                badRequest(request, message);
                                return;
                            }
                            createResponse(request, response, user, questionId);
                        });
                    }
                    else {
                        if (question_type == 6) {
                            try { dateFormatter.parse(response.getString(ANSWER)); }
                            catch (ParseException e) { e.printStackTrace(); }
                        }
                        if (question_type == 7) {
                            try { timeFormatter.parse(response.getString(ANSWER)); }
                            catch (ParseException e) { e.printStackTrace(); }
                        }
                        createResponse(request, response, user, questionId);
                    }
                });
            });
        });
    }

    private void createResponse(HttpServerRequest request, JsonObject response, UserInfos user, String questionId) {
        Integer distributionId = response.getInteger(DISTRIBUTION_ID, null);
        Integer choiceId = response.getInteger(CHOICE_ID, null);
        String answer = response.getString(ANSWER, null);

        responseService.listMineByDistribution(questionId, distributionId.toString(), user, listEvt -> {
            if (listEvt.isLeft()) {
                log.error("[Formulaire@createResponse] Fail to list responses : " + listEvt.left().getValue());
                renderInternalError(request, listEvt);
                return;
            }

            JsonArray responses = listEvt.right().getValue();
            boolean found = false;
            int i = 0;
            while (!found && i < responses.size()) {
                JsonObject r = responses.getJsonObject(i);
                boolean checkQuestionId = r.getInteger(QUESTION_ID).toString().equals(questionId);
                boolean checkResponderId = r.getString(RESPONDER_ID).equals(user.getUserId());
                boolean checkDistributionId = r.getInteger(DISTRIBUTION_ID).equals(distributionId);
                boolean checkAnswerId = r.getString(ANSWER).equals(answer);
                boolean checkChoiceId = r.getInteger(CHOICE_ID) == choiceId;
                found = checkQuestionId && checkResponderId && checkDistributionId && checkAnswerId && checkChoiceId;
                i++;
            }

            if (found) {
                String message = "[Formulaire@createResponse] An identical response already exists " + responses.getJsonObject(i-1);
                log.error(message);
                conflict(request, message);
                return;
            }

            responseService.create(response, user, questionId, defaultResponseHandler(request));
        });
    }

    @Put("/responses/:responseId")
    @ApiDoc("Update a specific response")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void update(HttpServerRequest request) {
        String responseId = request.getParam(PARAM_RESPONSE_ID);
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                String message = "[Formulaire@updateResponse] User not found in session.";
                log.error(message);
                unauthorized(request, message);
                return;
            }

            RequestUtils.bodyToJson(request, response -> {
                if (response == null || response.isEmpty()) {
                    log.error("[Formulaire@updateResponse] No response to update.");
                    noContent(request);
                    return;
                }

                Integer questionId = response.getInteger(QUESTION_ID);
                questionService.get(questionId.toString(), questionEvt -> {
                    if (questionEvt.isLeft()) {
                        log.error("[Formulaire@updateResponse] Fail to get question corresponding to id : " + questionId);
                        renderBadRequest(request, questionEvt);
                        return;
                    }
                    if (questionEvt.right().getValue().isEmpty()) {
                        String message = "[Formulaire@updateResponse] No question found for id " + questionId;
                        log.error(message);
                        notFound(request, message);
                        return;
                    }

                    JsonObject question = questionEvt.right().getValue();
                    int question_type = question.getInteger(QUESTION_TYPE);
                    Integer choice_id = response.getInteger(CHOICE_ID);

                    // Check if it's a question type you can respond to
                    if (QUESTIONS_WITHOUT_RESPONSES.contains(question_type)) {
                        String message = "[Formulaire@updateResponse] You cannot create a response for a question of type " + question_type;
                        log.error(message);
                        badRequest(request, message);
                        return;
                    }

                    // If there is a choice it should match an existing QuestionChoice for this question
                    if (choice_id != null && CHOICES_TYPE_QUESTIONS.contains(question_type)) {
                        questionChoiceService.get(choice_id.toString(), choiceEvt -> {
                            if (choiceEvt.isLeft()) {
                                log.error("[Formulaire@updateResponse] Fail to get question choice corresponding to id : " + choice_id);
                                renderBadRequest(request, choiceEvt);
                                return;
                            }
                            if (choiceEvt.right().getValue().isEmpty()) {
                                String message = "[Formulaire@updateResponse] No choice found for id " + choice_id;
                                log.error(message);
                                notFound(request, message);
                                return;
                            }

                            JsonObject choice = choiceEvt.right().getValue();

                            // Check choice validity
                            if (question.getInteger(MATRIX_ID) != null &&
                                (!choice.getInteger(QUESTION_ID).equals(question.getInteger(MATRIX_ID)) ||
                                !choice.getString(VALUE).equals(response.getString(ANSWER)))) {
                                String message ="[Formulaire@updateResponse] Wrong choice for response " + response;
                                log.error(message);
                                badRequest(request, message);
                                return;

                            }
                            else if (question.getInteger(MATRIX_ID) == null &&
                                (!choice.getInteger(QUESTION_ID).equals(questionId) ||
                                !choice.getString(VALUE).equals(response.getString(ANSWER)))) {
                                String message = "[Formulaire@updateResponse] Wrong choice for response " + response;
                                log.error(message);
                                badRequest(request, message);
                                return;
                            }

                            responseService.update(user, responseId, response, defaultResponseHandler(request));
                        });
                    }
                    else {
                        if (question_type == 6) {
                            try { dateFormatter.parse(response.getString(ANSWER)); }
                            catch (ParseException e) { e.printStackTrace(); }
                        }
                        if (question_type == 7) {
                            try { timeFormatter.parse(response.getString(ANSWER)); }
                            catch (ParseException e) { e.printStackTrace(); }
                        }
                        responseService.update(user, responseId, response, defaultResponseHandler(request));
                    }
                });
            });
        });
    }

    @Delete("/responses/:formId")
    @ApiDoc("Delete specific responses")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void delete(HttpServerRequest request) {
        String formId = request.getParam(PARAM_FORM_ID);
        RequestUtils.bodyToJsonArray(request, responses -> {
            if (responses == null || responses.isEmpty()) {
                log.error("[Formulaire@deleteResponses] No responses to delete.");
                noContent(request);
                return;
            }

            responseService.delete(getIds(responses), formId, arrayResponseHandler(request));
        });
    }
}