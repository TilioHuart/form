package fr.openent.formulaire_public.controllers;

import fr.openent.form.core.constants.DistributionStatus;
import fr.openent.form.helpers.MessageResponseHelper;
import fr.openent.form.helpers.UtilsHelper;
import fr.openent.formulaire_public.service.DistributionService;
import fr.openent.formulaire_public.service.FormService;
import fr.openent.formulaire_public.service.NotifyService;
import fr.openent.formulaire_public.service.ResponseService;
import fr.openent.formulaire_public.service.impl.DefaultDistributionService;
import fr.openent.formulaire_public.service.impl.DefaultFormService;
import fr.openent.formulaire_public.service.impl.DefaultNotifyService;
import fr.openent.formulaire_public.service.impl.DefaultResponseService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.notification.TimelineHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import static fr.openent.form.helpers.RenderHelper.renderBadRequest;
import static fr.openent.form.helpers.RenderHelper.renderInternalError;

public class FormController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(FormController.class);
    private final FormService publicFormService;
    private final DistributionService publicDistributionService;
    private final ResponseService publicResponseService;
    private final NotifyService publicNotifyService;
    private final SimpleDateFormat formDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
    private final long minTimeToAllowResponse = 4 * 1000;

    public FormController(TimelineHelper timelineHelper) {
        super();
        this.publicFormService = new DefaultFormService();
        this.publicDistributionService = new DefaultDistributionService();
        this.publicResponseService = new DefaultResponseService();
        this.publicNotifyService = new DefaultNotifyService(timelineHelper);
    }

    @Get("/forms/key/:formKey")
    @ApiDoc("Create a distribution and get a specific form by key")
    public void getPublicFormByKey(HttpServerRequest request) {
        String formKey = request.getParam("formKey");
        Cookie distributionKeyCookie = request.getCookie("distribution_key_" + formKey);

        if (distributionKeyCookie != null) {
            log.error("[FormulairePublic@createPublicResponses] The form has already been answered for distributionKey " + distributionKeyCookie.getValue());
            renderError(request);
            return;
        }

        publicFormService.getFormByKey(formKey, formEvt -> {
            if (formEvt.isLeft() || formEvt.right().getValue().isEmpty()) {
                log.error("[FormulairePublic@getPublicFormByKey] Fail to get form with key : " + formKey);
                renderBadRequest(request, formEvt);
                return;
            }

            JsonObject form = formEvt.right().getValue();

            // Check date_ending validity
            if (form.getString("date_ending") == null || form.getString("date_ending") == null) {
                log.error("[FormulairePublic@getPublicFormByKey] A public form must have an opening and ending date.");
                badRequest(request);
                return;
            }
            else {
                try {
                    Date startDate = formDateFormatter.parse(form.getString("date_opening"));
                    Date endDate = formDateFormatter.parse(form.getString("date_ending"));
                    if (endDate.before(new Date())) {
                        log.error("[FormulairePublic@getPublicFormByKey] This form is closed, you cannot access it anymore.");
                        unauthorized(request);
                        return;
                    }
                    if (endDate.before(startDate)) {
                        log.error("[FormulairePublic@getPublicFormByKey] The ending date must be after the opening date.");
                        unauthorized(request);
                        return;
                    }
                }
                catch (ParseException e) {
                    e.printStackTrace();
                    return;
                }
            }

            // Get all form data (sections, questions, question_choices)
            String formId = form.getInteger("id").toString();

            JsonObject sectionsMessage = new JsonObject().put("action", "list-sections").put("formId", formId);
            eb.request("fr.openent.formulaire", sectionsMessage, MessageResponseHelper.messageJsonArrayHandler(sectionsEvt -> {
                if (sectionsEvt.isLeft()) {
                    log.error("[FormulairePublic@getPublicFormByKey] Fail to get sections for form with key : " + formKey);
                    renderBadRequest(request, formEvt);
                    return;
                }

                JsonArray sections = sectionsEvt.right().getValue();
                form.put("form_elements", sections);

                JsonObject questionsMessage = new JsonObject().put("action", "list-question-for-form-and-section").put("formId", formId);
                eb.request("fr.openent.formulaire", questionsMessage, MessageResponseHelper.messageJsonArrayHandler(questionsEvt -> {
                    if (questionsEvt.isLeft()) {
                        log.error("[FormulairePublic@getPublicFormByKey] Fail to get questions for form with key : " + formKey);
                        renderBadRequest(request, formEvt);
                        return;
                    }

                    JsonArray questions = questionsEvt.right().getValue();
                    JsonArray questionIds = UtilsHelper.getIds(questions);

                    JsonObject questionChoicesMessage = new JsonObject().put("action", "list-question-choices").put("questionIds", questionIds);
                    eb.request("fr.openent.formulaire", questionChoicesMessage, MessageResponseHelper.messageJsonArrayHandler(questionChoicesEvt -> {
                        if (questionChoicesEvt.isLeft()) {
                            log.error("[FormulairePublic@getPublicFormByKey] Fail to get choices for questions with ids : " + questionIds);
                            renderBadRequest(request, formEvt);
                            return;
                        }

                        JsonArray questionsChoices = questionChoicesEvt.right().getValue();
                        HashMap<Integer, JsonArray> questionsChoicesMapped = new HashMap<>();
                        HashMap<Integer, JsonObject> sectionsMapped = new HashMap<>();

                        // Group questionChoices by questionId
                        for (Object qc : questionsChoices) {
                            JsonObject questionChoice = (JsonObject)qc;
                            int questionId = questionChoice.getInteger("question_id");
                            if (questionsChoicesMapped.get(questionId) == null) {
                                questionsChoicesMapped.put(questionId, new JsonArray());
                                questionsChoicesMapped.get(questionId).add(questionChoice);
                            }
                            else {
                                questionsChoicesMapped.get(questionId).add(questionChoice);
                            }
                        }

                        // Map sections by id
                        for (Object s : sections) {
                            JsonObject section = (JsonObject)s;
                            section.put("questions", new JsonArray());
                            sectionsMapped.put(section.getInteger("id"), section);
                        }

                        // Fill questions and add it where necessary
                        for (Object q : questions) {
                            JsonObject question = (JsonObject)q;
                            question.put("choices", new JsonArray());

                            // Fill question with questionChoices
                            JsonArray questionChoices = questionsChoicesMapped.get(question.getInteger("id"));
                            if (questionChoices != null) question.put("choices", questionChoices);

                            // Add question to its section or directly to form_elements
                            Integer sectionId = question.getInteger("section_id", null);
                            if (sectionId == null) {
                                form.getJsonArray("form_elements").add(question);
                            }
                            else {
                                sectionsMapped.get(sectionId).getJsonArray("questions").add(question);
                            }
                        }

                        // Create a new distribution, get the generated key and return the form data with it
                        publicDistributionService.createDistribution(form, distributionEvt -> {
                            if (distributionEvt.isLeft()) {
                                log.error("[FormulairePublic@createPublicDistribution] Fail to create a distribution for form with key : " + formKey);
                                renderBadRequest(request, formEvt);
                                return;
                            }

                            form.put("distribution_key", distributionEvt.right().getValue().getString("public_key"));
                            renderJson(request, form);
                        });
                    }));
                }));
            }));
        });
    }

    @Post("/responses/:formKey/:distributionKey")
    @ApiDoc("Create multiple responses")
    public void createResponses(HttpServerRequest request) {
        String formKey = request.getParam("formKey");
        String distributionKey = request.getParam("distributionKey");
        Cookie distributionKeyCookie = request.getCookie("distribution_key_" + formKey);

        if (distributionKeyCookie != null) {
            log.error("[FormulairePublic@createPublicResponses] The form has already been answered for distributionKey " + distributionKeyCookie.getValue());
            renderError(request);
            return;
        }

        if (formKey == null || distributionKey == null) {
            log.error("[FormulairePublic@createPublicResponses] FormKey and distributionKey must be not null.");
            badRequest(request);
            return;
        }

        request.pause();

        // Check if 'formKey' and 'distributionKey' are existing and matching
        publicFormService.getFormByKey(formKey, formEvt -> {
            if (formEvt.isLeft() || formEvt.right().getValue().isEmpty()) {
                log.error("[FormulairePublic@createPublicResponses] Fail to get form for key : " + formKey);
                renderBadRequest(request, formEvt);
                return;
            }

            JsonObject form = formEvt.right().getValue();
            Integer formId = form.getInteger("id");
            publicDistributionService.getDistributionByKey(distributionKey, distributionEvt -> {
                if (distributionEvt.isLeft() || distributionEvt.right().getValue().isEmpty()) {
                    log.error("[FormulairePublic@createPublicResponses] Fail to get distribution for key : " + distributionKey);
                    renderBadRequest(request, distributionEvt);
                    return;
                }

                JsonObject distribution = distributionEvt.right().getValue();
                if (!distribution.getInteger("form_id").equals(formId)) {
                    log.error("[FormulairePublic@createPublicResponses] The distributionKey provided is not matching the formKey " + formKey);
                    badRequest(request);
                    return;
                }

                // Check if the found distribution is not already with status FINISHED
                if (!distribution.getString("status").equals(DistributionStatus.TO_DO)) {
                    log.error("[FormulairePublic@createPublicResponses] The form has already been answered for distributionKey " + distributionKey);
                    renderError(request);
                    return;
                }

                // Check if the response time was too fast (to detect potential robot attacks)
                try {
                    Date sendingDate = formDateFormatter.parse(distribution.getString("date_sending"));
                    Date now = new Date();
                    long diff = now.getTime() - sendingDate.getTime();

                    if (diff < minTimeToAllowResponse) {
                        log.error("[FormulairePublic@createPublicResponses] The time of answer was too fast : " + diff/1000 + " seconds.");
                        renderError(request);
                        return;
                    }
                }
                catch (ParseException e) {
                    log.error("[FormulairePublic@createPublicResponses] Fail to parse the date_sending of the distribution with key " + distributionKey);
                    renderError(request);
                    return;
                }

                request.resume();

                RequestUtils.bodyToJsonArray(request, responses -> {
                    // Get question and question_choices information
                    JsonObject questionsMessage = new JsonObject().put("action", "list-question-for-form-and-section").put("formId", formId.toString());
                    eb.request("fr.openent.formulaire", questionsMessage, MessageResponseHelper.messageJsonArrayHandler(questionsEvt -> {
                        if (questionsEvt.isLeft()) {
                            log.error("[FormulairePublic@createPublicResponses] Fail to get questions corresponding to form with id : " + formId);
                            renderBadRequest(request, questionsEvt);
                            return;
                        }

                        JsonArray questions = questionsEvt.right().getValue();
                        JsonArray questionIds = UtilsHelper.getIds(questions);

                        JsonObject questionChoicesMessage = new JsonObject().put("action", "list-question-choices").put("questionIds", questionIds);
                        eb.request("fr.openent.formulaire", questionChoicesMessage, MessageResponseHelper.messageJsonArrayHandler(questionChoicesEvt -> {
                            if (questionChoicesEvt.isLeft()) {
                                log.error("[FormulairePublic@createPublicResponses] Fail to get choices for questions with ids : " + questionIds);
                                renderBadRequest(request, formEvt);
                                return;
                            }

                            JsonArray questionsChoices = questionChoicesEvt.right().getValue();
                            HashMap<Integer, JsonArray> questionsChoicesMapped = new HashMap<>();
                            HashMap<Integer, JsonObject> questionsMapped = new HashMap<>();

                            // Group questionChoices by questionId
                            for (Object qc : questionsChoices) {
                                JsonObject questionChoice = (JsonObject) qc;
                                int questionId = questionChoice.getInteger("question_id");
                                if (questionsChoicesMapped.get(questionId) == null) {
                                    questionsChoicesMapped.put(questionId, new JsonArray());
                                    questionsChoicesMapped.get(questionId).add(questionChoice);
                                } else {
                                    questionsChoicesMapped.get(questionId).add(questionChoice);
                                }
                            }

                            // Fill questions and add it where necessary
                            for (Object q : questions) {
                                JsonObject question = (JsonObject)q;
                                questionsMapped.put(question.getInteger("id"), question);

                                // Fill question with questionChoices
                                question.put("choices", new JsonArray());
                                JsonArray questionChoices = questionsChoicesMapped.get(question.getInteger("id"));
                                if (questionChoices != null) question.put("choices", questionChoices);
                            }

                            // Check if all the question_ids from the responses match existing questions from the form
                            Integer wrongQuestionId = null;
                            int i = 0;
                            while (wrongQuestionId == null && i < responses.size()) {
                                JsonObject response = responses.getJsonObject(i);
                                Integer questionId = response.getInteger("question_id");

                                if (!questionIds.contains(questionId.toString())) {
                                    wrongQuestionId = questionId;
                                }
                                else {
                                    JsonObject question = questionsMapped.get(questionId);
                                    int questionType = question.getInteger("question_type");
                                    Integer choiceId = response.getInteger("choice_id");

                                    // If there is a choice it should match an existing QuestionChoice for this question
                                    if (choiceId != null && Arrays.asList(4,5,9).contains(questionType)) {
                                        JsonArray choices = question.getJsonArray("choices");
                                        boolean isChoiceValid = false;
                                        int j = 0;
                                        while (!isChoiceValid && j < choices.size()) {
                                            JsonObject choice = choices.getJsonObject(j);
                                            if (choice.getInteger("id").equals(choiceId) && choice.getString("value").equals(response.getString("answer"))) {
                                                isChoiceValid = true;
                                            }
                                            j++;
                                        }

                                        if (!isChoiceValid) {
                                            log.error("[FormulairePublic@createPublicResponses] Wrong choice for response : " + response);
                                            badRequest(request);
                                            return;
                                        }
                                    }
                                    else { // If it's a type 6 or 7 check parsing into Date or Time
                                        if (questionType == 6 && response.getString("answer") != null && !response.getString("answer").isEmpty()) {
                                            try { dateFormatter.parse(response.getString("answer")); }
                                            catch (ParseException e) {
                                                log.error("[FormulairePublic@createPublicResponses] Fail to parse as date the answer " + response.getString("answer"));
                                                renderError(request);
                                                return;
                                            }
                                        }
                                        if (questionType == 7 && response.getString("answer") != null && !response.getString("answer").isEmpty()) {
                                            try { timeFormatter.parse(response.getString("answer")); }
                                            catch (ParseException e) {
                                                log.error("[FormulairePublic@createPublicResponses] Fail to parse as time the answer " + response.getString("answer"));
                                                renderError(request);
                                                return;
                                            }
                                        }
                                    }
                                }
                                i++;
                            }

                            if (wrongQuestionId != null) {
                                log.error("[FormulairePublic@createPublicResponses] Wrong value for the question_id, there's no question in this form with id : " + wrongQuestionId);
                                badRequest(request);
                                return;
                            }

                            // Save responses and change the status of the distribution
                            publicResponseService.createResponses(responses, distribution, responsesEvt -> {
                                if (responsesEvt.isLeft()) {
                                    log.error("[FormulairePublic@createPublicResponses] Fail to create responses : " + responses);
                                    renderBadRequest(request, responsesEvt);
                                    return;
                                }

                                // Set cookie
                                Cookie cookie = new CookieImpl("distribution_key_" + formKey, distributionKey);
                                cookie.setPath("/formulaire/p");
                                cookie.setSameSite(CookieSameSite.STRICT);
                                request.response().addCookie(cookie);

                                publicDistributionService.finishDistribution(distributionKey, finalDistributionEvt -> {
                                    if (finalDistributionEvt.isLeft()) {
                                        log.error("[FormulairePublic@createPublicResponses] Fail to finish distribution with key : " + distributionKey);
                                        renderBadRequest(request, finalDistributionEvt);
                                        return;
                                    }

                                    JsonObject finalDistribution = finalDistributionEvt.right().getValue();
                                    if (form.getBoolean("response_notified")) {
                                        publicFormService.listManagers(form.getInteger("id").toString(), listManagersEvt -> {
                                            if (listManagersEvt.isLeft()) {
                                                log.error("[FormulairePublic@createPublicResponses] Error in listing managers for form with id " + formId);
                                                renderInternalError(request, listManagersEvt);
                                                return;
                                            }

                                            JsonArray managers = listManagersEvt.right().getValue();
                                            JsonArray managerIds = new JsonArray();
                                            for (int j = 0; j < managers.size(); j++) {
                                                managerIds.add(managers.getJsonObject(j).getString("id"));
                                            }

                                            publicNotifyService.notifyResponse(request, form, managerIds);
                                            renderJson(request, finalDistribution, 200);
                                        });
                                    }
                                    else {
                                        renderJson(request, finalDistribution, 200);
                                    }
                                });
                            });
                        }));
                    }));
                });
            });
        });
    }
}