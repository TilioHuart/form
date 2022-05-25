package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.helpers.RenderHelper;
import fr.openent.formulaire.helpers.UtilsHelper;
import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.security.ShareAndOwner;
import fr.openent.formulaire.service.FormService;
import fr.openent.formulaire.service.QuestionService;
import fr.openent.formulaire.service.impl.DefaultFormService;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
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
import org.entcore.common.user.UserUtils;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class QuestionController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(QuestionController.class);
    private final QuestionService questionService;
    private final FormService formService;

    public QuestionController() {
        super();
        this.formService = new DefaultFormService();
        this.questionService = new DefaultQuestionService();
    }

    @Get("/forms/:formId/questions")
    @ApiDoc("List all the questions of a specific form")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listForForm(HttpServerRequest request) {
        String formId = request.getParam("formId");
        questionService.listForForm(formId, arrayResponseHandler(request));
    }

    @Get("/sections/:sectionId/questions")
    @ApiDoc("List all the questions of a specific section")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listForSection(HttpServerRequest request) {
        String sectionId = request.getParam("sectionId");
        questionService.listForSection(sectionId, arrayResponseHandler(request));
    }

    @Get("/questions/:questionId")
    @ApiDoc("Get a specific question by id")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void get(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        questionService.get(questionId, defaultResponseHandler(request));
    }

    @Post("/forms/:formId/questions")
    @ApiDoc("Create a question in a specific form")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void create(HttpServerRequest request) {
        String formId = request.getParam("formId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                String message = "[Formulaire@updateDistribution] User not found in session.";
                log.error(message);
                unauthorized(request, message);
                return;
            }

            RequestUtils.bodyToJson(request, question -> {
                if (question == null || question.isEmpty()) {
                    log.error("[Formulaire@createQuestion] No question to create.");
                    noContent(request);
                    return;
                }

                Long sectionId = question.getLong("section_id");
                Long sectionPosition = question.getLong("section_position");

                // Check section infos validity
                if (sectionId == null ^ sectionPosition == null) {
                    String message = "[Formulaire@createQuestion] sectionId and sectionPosition must be both null or both not null.";
                    log.error(message);
                    badRequest(request, message);
                    return;
                } else if (sectionId != null && (sectionId <= 0 || sectionPosition <= 0)) {
                    String message = "[Formulaire@createQuestion] sectionId and sectionPosition must have values greater than 0.";
                    log.error(message);
                    badRequest(request, message);
                    return;
                } else if (sectionPosition != null && question.getInteger("position") != null) {
                    String message = "[Formulaire@createQuestion] A question is either in or out of a section, it cannot have a position and a section_position.";
                    log.error(message);
                    badRequest(request, message);
                    return;
                } else if (question.getBoolean("conditional") && !Formulaire.CONDITIONAL_QUESTIONS.contains(question.getInteger("question_type"))) {
                    String message = "[Formulaire@createQuestion] A question conditional question must be of type : " + Formulaire.CONDITIONAL_QUESTIONS;
                    log.error(message);
                    badRequest(request, message);
                    return;
                }

                formService.get(formId, user, formEvt -> {
                    if (formEvt.isLeft()) {
                        log.error("[Formulaire@createQuestion] Failed to get form with id : " + formId);
                        RenderHelper.internalError(request, formEvt);
                        return;
                    }
                    if (formEvt.right().getValue().isEmpty()) {
                        String message = "[Formulaire@createQuestion] No form found for form with id " + formId;
                        log.error(message);
                        notFound(request, message);
                        return;
                    }

                    JsonObject form = formEvt.right().getValue();

                    // Check if the type of question if it's for a public form (type FILE is forbidden)
                    if (form.getBoolean("is_public") && question.getInteger("question_type") != null && question.getInteger("question_type") == 8) {
                        String message = "[Formulaire@createQuestion] You cannot create a question type FILE for the public form with id " + formId;
                        log.error(message);
                        badRequest(request, message);
                        return;
                    }

                    // If it's a conditional question in a section, check if another one already exists
                    if (question.getBoolean("conditional")) {
                        questionService.getSectionIdsWithConditionalQuestions(formId, sectionIdsEvt -> {
                            if (sectionIdsEvt.isLeft()) {
                                log.error("[Formulaire@createQuestion] Failed to get section ids for form with id : " + formId);
                                RenderHelper.internalError(request, sectionIdsEvt);
                                return;
                            }
                            if (sectionIdsEvt.right().getValue().isEmpty()) {
                                String message = "[Formulaire@createQuestion] No section ids found for form with id " + formId;
                                log.error(message);
                                notFound(request, message);
                                return;
                            }

                            JsonArray sectionIds = UtilsHelper.getByProp(sectionIdsEvt.right().getValue(), "section_id");
                            if (!sectionIds.contains(sectionId)) {
                                String message = "[Formulaire@createQuestion] A conditional question is already existing for the section with id : " + sectionId;
                                log.error(message);
                                badRequest(request, message);
                                return;
                            }

                            questionService.create(question, formId, defaultResponseHandler(request));
                        });
                    } else {
                        questionService.create(question, formId, defaultResponseHandler(request));
                    }
                });
            });
        });
    }

    @Put("/forms/:formId/questions")
    @ApiDoc("Update specific questions")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void update(HttpServerRequest request) {
        String formId = request.getParam("formId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                String message = "[Formulaire@updateDistribution] User not found in session.";
                log.error(message);
                unauthorized(request, message);
                return;
            }

            RequestUtils.bodyToJsonArray(request, questions -> {
                if (questions == null || questions.isEmpty()) {
                    log.error("[Formulaire@updateQuestions] No questions to update.");
                    noContent(request);
                    return;
                }

                // Check section infos validity
                int i = 0;
                while (i < questions.size()) {
                    JsonObject question = questions.getJsonObject(i);
                    Long sectionId = question.getLong("section_id");
                    Long sectionPosition = question.getLong("section_position");

                    if (sectionId == null ^ sectionPosition == null) {
                        String message = "[Formulaire@updateQuestions] sectionId and sectionPosition must be both null or both not null : " + question;
                        log.error(message);
                        badRequest(request, message);
                        return;
                    } else if (sectionId != null && (sectionId <= 0 || sectionPosition <= 0)) {
                        String message = "[Formulaire@updateQuestions] sectionId and sectionPosition must have values greater than 0 : " + question;
                        log.error(message);
                        badRequest(request, message);
                        return;
                    } else if (sectionPosition != null && question.getInteger("position") != null) {
                        String message = "[Formulaire@updateQuestions] A question is either in or out of a section, it cannot have a position and a section_position : " + question;
                        log.error(message);
                        badRequest(request, message);
                        return;
                    } else if (question.getBoolean("conditional") && !Formulaire.CONDITIONAL_QUESTIONS.contains(question.getInteger("question_type"))) {
                        String message = "[Formulaire@updateQuestions] A question conditional question must be of type : " + Formulaire.CONDITIONAL_QUESTIONS;
                        log.error(message);
                        badRequest(request, message);
                        return;
                    }
                    i++;
                }

                formService.get(formId, user, formEvt -> {
                    if (formEvt.isLeft()) {
                        log.error("[Formulaire@createQuestion] Failed to get form with id : " + formId);
                        RenderHelper.internalError(request, formEvt);
                        return;
                    }
                    if (formEvt.right().getValue().isEmpty()) {
                        String message = "[Formulaire@createQuestion] No form found for form with id " + formId;
                        log.error(message);
                        notFound(request, message);
                        return;
                    }

                    JsonObject form = formEvt.right().getValue();

                    // Check if the type of question if it's for a public form (type FILE is forbidden)
                    JsonArray questionTypes = UtilsHelper.getByProp(questions, "question_type");
                    if (form.getBoolean("is_public") && questionTypes.contains(8)) {
                        String message = "[Formulaire@createQuestion] You cannot create a question type FILE for the public form with id " + formId;
                        log.error(message);
                        badRequest(request, message);
                        return;
                    }

                    // Check if sections of conditional questions to create already have conditional questions
                    questionService.getSectionIdsWithConditionalQuestions(formId, sectionIdsEvt -> {
                        if (sectionIdsEvt.isLeft()) {
                            log.error("[Formulaire@updateQuestions] Failed to get section ids for form with id : " + formId);
                            RenderHelper.internalError(request, sectionIdsEvt);
                            return;
                        }

                        JsonArray sectionIds = UtilsHelper.getByProp(sectionIdsEvt.right().getValue(), "section_id");
                        Long conflictingSectionId = null;
                        int j = 0;
                        while (conflictingSectionId == null && j < questions.size()) {
                            JsonObject question = questions.getJsonObject(j);
                            if (question.getBoolean("conditional") && sectionIds.contains(question.getLong("section_id"))) {
                                conflictingSectionId = question.getLong("section_id");
                            }
                            j++;
                        }

                        if (conflictingSectionId != null) {
                            String errorMessage = "[Formulaire@updateQuestions] A conditional question is already existing for the section with id : " + conflictingSectionId;
                            log.error(errorMessage);
                            badRequest(request, errorMessage);
                            return;
                        }

                        // If no conditional question conflict, we update
                        questionService.update(formId, questions, updatedQuestionsEvt -> {
                            if (updatedQuestionsEvt.isLeft()) {
                                log.error("[Formulaire@updateQuestions] Failed to update questions : " + questions);
                                RenderHelper.internalError(request, updatedQuestionsEvt);
                                return;
                            }

                            JsonArray updatedQuestionsInfos = updatedQuestionsEvt.right().getValue();
                            JsonArray updatedQuestions = new JsonArray();
                            for (int k = 0; k < updatedQuestionsInfos.size(); k++) {
                                updatedQuestions.addAll(updatedQuestionsInfos.getJsonArray(k));
                            }
                            renderJson(request, updatedQuestions);
                        });
                    });
                });
            });
        });
    }

    @Delete("/questions/:questionId")
    @ApiDoc("Delete a specific question")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void delete(HttpServerRequest request) {
        String questionId = request.getParam("questionId");
        questionService.get(questionId, questionsEvt -> {
            if (questionsEvt.isLeft()) {
                log.error("[Formulaire@deleteQuestion] Failed to get question with id : " + questionId);
                RenderHelper.internalError(request, questionsEvt);
                return;
            }
            questionService.delete(questionsEvt.right().getValue(), defaultResponseHandler(request));
        });
    }
}