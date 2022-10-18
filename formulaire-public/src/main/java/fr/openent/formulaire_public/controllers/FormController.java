package fr.openent.formulaire_public.controllers;

import fr.openent.form.core.constants.DistributionStatus;
import fr.openent.form.helpers.MessageResponseHelper;
import fr.openent.form.helpers.UtilsHelper;
import fr.openent.formulaire_public.helpers.CaptchaHelper;
import fr.openent.formulaire_public.service.*;
import fr.openent.formulaire_public.service.impl.*;
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

import static fr.openent.form.core.constants.Constants.CHOICES_TYPE_QUESTIONS;
import static fr.openent.form.core.constants.EbFields.*;
import static fr.openent.form.core.constants.EbFields.ACTION;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.helpers.RenderHelper.renderBadRequest;
import static fr.openent.form.helpers.RenderHelper.renderInternalError;

public class FormController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(FormController.class);
    private final FormService publicFormService;
    private final DistributionService publicDistributionService;
    private final ResponseService publicResponseService;
    private final NotifyService publicNotifyService;
    private final CaptchaService publicCaptchaService;
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
        this.publicCaptchaService = new DefaultCaptchaService();
    }

    @Get("/forms/key/:formKey")
    @ApiDoc("Create a distribution and get a specific form by key")
    public void getPublicFormByKey(HttpServerRequest request) {
        String formKey = request.getParam(PARAM_FORM_KEY);
        Cookie distributionKeyCookie = request.getCookie(DISTRIBUTION_KEY_ + formKey);

        if (distributionKeyCookie != null) {
            String message = "[FormulairePublic@createPublicResponses] The form has already been answered for distributionKey " + distributionKeyCookie.getValue();
            log.error(message);
            badRequest(request, message);
            return;
        }

        publicFormService.getFormByKey(formKey, formEvt -> {
            if (formEvt.isLeft()) {
                String message = "[FormulairePublic@getPublicFormByKey] Fail to get form with key " + formKey;
                renderInternalError(request, formEvt, message);
                return;
            }
            if (formEvt.right().getValue().isEmpty()) {
                log.error("[FormulairePublic@getPublicFormByKey] No form found for key " + formKey);
                notFound(request);
                return;
            }

            JsonObject form = formEvt.right().getValue();

            // Check date_ending validity
            if (form.getString(DATE_ENDING) == null || form.getString(DATE_ENDING) == null) {
                String message = "[FormulairePublic@getPublicFormByKey] A public form must have an opening and ending date.";
                log.error(message);
                badRequest(request, message);
                return;
            }
            else {
                try {
                    Date startDate = formDateFormatter.parse(form.getString(DATE_OPENING));
                    Date endDate = formDateFormatter.parse(form.getString(DATE_ENDING));
                    if (endDate.before(new Date())) {
                        log.error("[FormulairePublic@getPublicFormByKey] This form is closed, you cannot access it anymore.");
                        forbidden(request);
                        return;
                    }
                    if (endDate.before(startDate)) {
                        log.error("[FormulairePublic@getPublicFormByKey] The ending date must be after the opening date.");
                        forbidden(request);
                        return;
                    }
                }
                catch (ParseException e) {
                    e.printStackTrace();
                    return;
                }
            }

            // Get all form data (sections, questions, question_choices, children)
            String formId = form.getInteger(ID).toString();

            JsonObject sectionsMessage = new JsonObject().put(ACTION, LIST_SECTIONS).put(PARAM_FORM_ID, formId);
            eb.request(FORMULAIRE_ADDRESS, sectionsMessage, MessageResponseHelper.messageJsonArrayHandler(sectionsEvt -> {
                if (sectionsEvt.isLeft()) {
                    String message = "[FormulairePublic@getPublicFormByKey] Fail to get sections for form with key " + formKey;
                    renderInternalError(request, sectionsEvt, message);
                    return;
                }

                JsonArray sections = sectionsEvt.right().getValue();
                form.put(FORM_ELEMENTS, sections);

                // Map sections by id
                HashMap<Integer, JsonObject> sectionsMapped = new HashMap<>();
                for (Object s : sections) {
                    JsonObject section = (JsonObject)s;
                    section.put(QUESTIONS, new JsonArray());
                    sectionsMapped.put(section.getInteger(ID), section);
                }

                JsonObject questionsMessage = new JsonObject().put(ACTION, LIST_QUESTION_FOR_FORM_AND_SECTION).put(PARAM_FORM_ID, formId);
                eb.request(FORMULAIRE_ADDRESS, questionsMessage, MessageResponseHelper.messageJsonArrayHandler(questionsEvt -> {
                    if (questionsEvt.isLeft()) {
                        String message = "[FormulairePublic@getPublicFormByKey] Fail to get questions for form with key " + formKey;
                        renderInternalError(request, questionsEvt, message);
                        return;
                    }

                    JsonArray questions = questionsEvt.right().getValue();
                    JsonArray questionIds = UtilsHelper.getIds(questions);

                    JsonObject childrenMessage = new JsonObject().put(ACTION, LIST_QUESTION_CHILDREN).put(PARAM_QUESTION_IDS, questionIds);
                    eb.request(FORMULAIRE_ADDRESS, childrenMessage, MessageResponseHelper.messageJsonArrayHandler(childrenEvt -> {
                        if (childrenEvt.isLeft()) {
                            String message = "[FormulairePublic@getPublicFormByKey] Fail to get children questions for form with key " + formKey;
                            renderInternalError(request, childrenEvt, message);
                            return;
                        }

                        JsonArray children = childrenEvt.right().getValue();

                        // Group children by questionId
                        HashMap<Integer, JsonArray> childrenMapped = new HashMap<>();
                        for (Object c : children) {
                            JsonObject child = (JsonObject)c;
                            int matrixId = child.getInteger(MATRIX_ID);
                            if (childrenMapped.get(matrixId) == null) {
                                childrenMapped.put(matrixId, new JsonArray());
                            }
                            childrenMapped.get(matrixId).add(child);
                        }

                        JsonObject questionChoicesMessage = new JsonObject().put(ACTION, LIST_QUESTION_CHOICES).put(PARAM_QUESTION_IDS, questionIds);
                        eb.request(FORMULAIRE_ADDRESS, questionChoicesMessage, MessageResponseHelper.messageJsonArrayHandler(questionChoicesEvt -> {
                            if (questionChoicesEvt.isLeft()) {
                                String message = "[FormulairePublic@getPublicFormByKey] Fail to get choices for questions with ids " + questionIds;
                                renderInternalError(request, questionChoicesEvt, message);
                                return;
                            }

                            JsonArray questionsChoices = questionChoicesEvt.right().getValue();

                            // Group questionChoices by questionId
                            HashMap<Integer, JsonArray> questionsChoicesMapped = new HashMap<>();
                            for (Object qc : questionsChoices) {
                                JsonObject questionChoice = (JsonObject)qc;
                                int questionId = questionChoice.getInteger(QUESTION_ID);
                                if (questionsChoicesMapped.get(questionId) == null) {
                                    questionsChoicesMapped.put(questionId, new JsonArray());
                                }
                                questionsChoicesMapped.get(questionId).add(questionChoice);
                            }

                            // Fill questions and add it where necessary
                            for (Object q : questions) {
                                JsonObject question = (JsonObject)q;
                                question.put(CHILDREN, new JsonArray());
                                question.put(CHOICES, new JsonArray());

                                // Fill question with questionChildren
                                JsonArray questionChildren = childrenMapped.get(question.getInteger(ID));
                                if (questionChildren != null) question.put(CHILDREN, questionChildren);

                                // Fill question with questionChoices
                                JsonArray questionChoices = questionsChoicesMapped.get(question.getInteger(ID));
                                if (questionChoices != null) question.put(CHOICES, questionChoices);

                                // Add question to its section or directly to form_elements
                                Integer sectionId = question.getInteger(SECTION_ID, null);
                                if (sectionId == null) {
                                    form.getJsonArray(FORM_ELEMENTS).add(question);
                                }
                                else {
                                    sectionsMapped.get(sectionId).getJsonArray(QUESTIONS).add(question);
                                }
                            }

                            // Create a new distribution, get the generated key and return the form data with it
                            publicDistributionService.createDistribution(form, distributionEvt -> {
                                if (distributionEvt.isLeft()) {
                                    String message = "[FormulairePublic@createPublicDistribution] Fail to create a distribution for form with key " + formKey;
                                    renderInternalError(request, distributionEvt, message);
                                    return;
                                }

                                JsonObject distribution = distributionEvt.right().getValue();
                                form.put(DISTRIBUTION_KEY, distribution.getString(PUBLIC_KEY));
                                form.put(DISTRIBUTION_CAPTCHA, distribution.getInteger(CAPTCHA_ID));
                                renderJson(request, form);
                            });
                        }));
                    }));
                }));
            }));
        });
    }

    @Post("/responses/:formKey/:distributionKey")
    @ApiDoc("Create multiple responses")
    public void createResponses(HttpServerRequest request) {
        String formKey = request.getParam(PARAM_FORM_KEY);
        String distributionKey = request.getParam(PARAM_DISTRIBUTION_KEY);
        Cookie distributionKeyCookie = request.getCookie(DISTRIBUTION_KEY_ + formKey);

        if (distributionKeyCookie != null) {
            String message = "[FormulairePublic@createPublicResponses] The form has already been answered for distributionKey " + distributionKeyCookie.getValue();
            log.error(message);
            badRequest(request, message);
            return;
        }

        if (formKey == null || distributionKey == null) {
            String message = "[FormulairePublic@createPublicResponses] FormKey and distributionKey must be not null.";
            log.error(message);
            badRequest(request, message);
            return;
        }

        request.pause();

        // Check if 'formKey' and 'distributionKey' are existing and matching
        publicFormService.getFormByKey(formKey, formEvt -> {
            if (formEvt.isLeft()) {
                String message = "[FormulairePublic@createPublicResponses] Fail to get form for key " + formKey;
                renderInternalError(request, formEvt, message);
                return;
            }
            if (formEvt.right().getValue().isEmpty()) {
                log.error("[FormulairePublic@createPublicResponses] No form found for key " + formKey);
                notFound(request);
                return;
            }

            JsonObject form = formEvt.right().getValue();
            Integer formId = form.getInteger(ID);
            publicDistributionService.getDistributionByKey(distributionKey, distributionEvt -> {
                if (distributionEvt.isLeft()) {
                    String message = "[FormulairePublic@createPublicResponses] Fail to get distribution for key " + distributionKey;
                    renderInternalError(request, distributionEvt, message);
                    return;
                }
                if (distributionEvt.right().getValue().isEmpty()) {
                    log.error("[FormulairePublic@createPublicResponses] No distribution found for key " + distributionKey);
                    notFound(request);
                    return;
                }

                JsonObject distribution = distributionEvt.right().getValue();
                // Check if the distribution matches the formKey received
                if (!distribution.getInteger(FORM_ID).equals(formId)) {
                    String message = "[FormulairePublic@createPublicResponses] The distributionKey provided is not matching the formKey " + formKey;
                    log.error(message);
                    badRequest(request, message);
                    return;
                }

                // Check if the found distribution is not already with status FINISHED
                if (!distribution.getString(STATUS).equals(DistributionStatus.TO_DO)) {
                    String message = "[FormulairePublic@createPublicResponses] The form has already been answered for distributionKey " + distributionKey;
                    log.error(message);
                    forbidden(request, message);
                    return;
                }

                // Check if the response time was too fast (to detect potential robot attacks)
                try {
                    Date sendingDate = formDateFormatter.parse(distribution.getString(DATE_SENDING));
                    Date now = new Date();
                    long diff = now.getTime() - sendingDate.getTime();

                    if (diff < minTimeToAllowResponse) {
                        String message = "[FormulairePublic@createPublicResponses] The time of answer was too fast : " + diff/1000 + " seconds.";
                        log.error(message);
                        renderInternalError(request, message);
                        return;
                    }
                }
                catch (ParseException e) {
                    String message = "[FormulairePublic@createPublicResponses] Fail to parse the date_sending of the distribution with key " + distributionKey;
                    log.error(message);
                    renderInternalError(request, message);
                    return;
                }

                request.resume();

                RequestUtils.bodyToJson(request, data -> {

                    // Check CAPTCHA response
                    String captchaResponse = data.getJsonObject(CAPTCHA).getString(ANSWER);
                    String captchaAnswer = CaptchaHelper.formatCaptchaAnswer(captchaResponse);
                    String captchaId = distribution.getInteger(CAPTCHA_ID).toString();
                    publicCaptchaService.get(captchaId, captchaEvt -> {
                        if (captchaEvt.isLeft()) {
                            String message = "[FormulairePublic@createPublicResponses] Fail to get captcha with id " + captchaId;
                            renderInternalError(request, captchaEvt, message);
                            return;
                        }

                        String captchaSolution = captchaEvt.right().getValue().getString(ANSWER);
                        if (captchaAnswer == null || !captchaAnswer.equals(captchaSolution)) {
                            String message = "[FormulairePublic@createPublicResponses] Wrong response for CAPTCHA with id " + captchaId + " : " + captchaResponse;
                            log.error(message);
                            renderJson(request, distribution, 200);
                            return;
                        }

                        JsonArray responses = data.getJsonArray(RESPONSES);

                        // Get question and question_choices information
                        JsonObject questionsMessage = new JsonObject().put(ACTION, LIST_QUESTION_FOR_FORM_AND_SECTION).put(PARAM_FORM_ID, formId.toString());
                        eb.request(FORMULAIRE_ADDRESS, questionsMessage, MessageResponseHelper.messageJsonArrayHandler(questionsEvt -> {
                            if (questionsEvt.isLeft()) {
                                String message = "[FormulairePublic@createPublicResponses] Fail to get questions corresponding to form with id " + formId;
                                renderInternalError(request, questionsEvt, message);
                                return;
                            }

                            JsonArray questions = questionsEvt.right().getValue();
                            JsonArray questionIds = UtilsHelper.getIds(questions);

                            JsonObject childrenMessage = new JsonObject().put(ACTION, LIST_QUESTION_CHILDREN).put(PARAM_QUESTION_IDS, questionIds);
                            eb.request(FORMULAIRE_ADDRESS, childrenMessage, MessageResponseHelper.messageJsonArrayHandler(childrenEvt -> {
                                if (childrenEvt.isLeft()) {
                                    String message = "[FormulairePublic@createPublicResponses] Fail to get children questions for form with key " + formKey;
                                    renderInternalError(request, childrenEvt, message);
                                    return;
                                }

                                JsonArray children = childrenEvt.right().getValue();
                                JsonArray childrenIds = UtilsHelper.getIds(children);

                                JsonObject questionChoicesMessage = new JsonObject().put(ACTION, LIST_QUESTION_CHOICES).put(PARAM_QUESTION_IDS, questionIds);
                                eb.request(FORMULAIRE_ADDRESS, questionChoicesMessage, MessageResponseHelper.messageJsonArrayHandler(questionChoicesEvt -> {
                                    if (questionChoicesEvt.isLeft()) {
                                        String message = "[FormulairePublic@createPublicResponses] Fail to get choices for questions with ids " + questionIds;
                                        renderInternalError(request, questionChoicesEvt, message);
                                        return;
                                    }

                                    JsonArray questionsChoices = questionChoicesEvt.right().getValue();
                                    HashMap<Integer, JsonArray> questionsChoicesMapped = new HashMap<>();
                                    HashMap<Integer, JsonObject> questionsMapped = new HashMap<>();

                                    // Group questionChoices by questionId
                                    for (Object qc : questionsChoices) {
                                        JsonObject questionChoice = (JsonObject) qc;
                                        int questionId = questionChoice.getInteger(QUESTION_ID);
                                        if (questionsChoicesMapped.get(questionId) == null) {
                                            questionsChoicesMapped.put(questionId, new JsonArray());
                                            questionsChoicesMapped.get(questionId).add(questionChoice);
                                        } else {
                                            questionsChoicesMapped.get(questionId).add(questionChoice);
                                        }
                                    }

                                    // Fill question choices and add it in mapping where necessary
                                    questions.addAll(children);
                                    for (Object q : questions) {
                                        JsonObject question = (JsonObject)q;
                                        questionsMapped.put(question.getInteger(ID), question);

                                        // Fill question with questionChoices
                                        question.put(CHOICES, new JsonArray());
                                        JsonArray questionChoices = questionsChoicesMapped.get(question.getInteger(ID));
                                        if (questionChoices != null) question.put(CHOICES, questionChoices);
                                    }

                                    // Check if all the question_ids from the responses match existing questions from the form
                                    Integer wrongQuestionId = null;
                                    int i = 0;
                                    while (wrongQuestionId == null && i < responses.size()) {
                                        JsonObject response = responses.getJsonObject(i);
                                        Integer questionId = response.getInteger(QUESTION_ID);

                                        if (!questionIds.contains(questionId.toString()) && !childrenIds.contains(questionId.toString())) {
                                            wrongQuestionId = questionId;
                                        }
                                        else {
                                            JsonObject question = questionsMapped.get(questionId);
                                            int questionType = question.getInteger(QUESTION_TYPE);
                                            Integer choiceId = response.getInteger(CHOICE_ID);

                                            // If there is a choice it should match an existing QuestionChoice for this question
                                            if (choiceId != null && CHOICES_TYPE_QUESTIONS.contains(questionType)) {
                                                JsonArray choices = question.getJsonArray(CHOICES);

                                                // If matrix child, it's the parent which contains the choices
                                                Integer matrixId = question.getInteger(MATRIX_ID);
                                                if (matrixId != null) {
                                                    choices = questionsMapped.get(matrixId).getJsonArray(CHOICES);
                                                }

                                                boolean isChoiceValid = false;
                                                int j = 0;
                                                while (!isChoiceValid && j < choices.size()) {
                                                    JsonObject choice = choices.getJsonObject(j);
                                                    if (choice.getInteger(ID).equals(choiceId) && choice.getString(VALUE).equals(response.getString(ANSWER))) {
                                                        isChoiceValid = true;
                                                    }
                                                    j++;
                                                }

                                                if (!isChoiceValid) {
                                                    String message = "[FormulairePublic@createPublicResponses] Wrong choice for response " + response;
                                                    log.error(message);
                                                    badRequest(request, message);
                                                    return;
                                                }
                                            }
                                            else { // If it's a type 6 or 7 check parsing into Date or Time
                                                if (questionType == 6 && response.getString(ANSWER) != null && !response.getString(ANSWER).isEmpty()) {
                                                    try { dateFormatter.parse(response.getString(ANSWER)); }
                                                    catch (ParseException e) {
                                                        String message = "[FormulairePublic@createPublicResponses] Fail to parse as date the answer " + response.getString(ANSWER);
                                                        log.error(message);
                                                        renderInternalError(request, message);
                                                        return;
                                                    }
                                                }
                                                if (questionType == 7 && response.getString(ANSWER) != null && !response.getString(ANSWER).isEmpty()) {
                                                    try { timeFormatter.parse(response.getString(ANSWER)); }
                                                    catch (ParseException e) {
                                                        String message = "[FormulairePublic@createPublicResponses] Fail to parse as time the answer " + response.getString(ANSWER);
                                                        log.error(message);
                                                        renderInternalError(request, message);
                                                        return;
                                                    }
                                                }
                                            }
                                        }
                                        i++;
                                    }

                                    if (wrongQuestionId != null) {
                                        String message = "[FormulairePublic@createPublicResponses] Wrong value for the question_id, there's no question in this form with id : " + wrongQuestionId;
                                        log.error(message);
                                        badRequest(request, message);
                                        return;
                                    }

                                    // Save responses and change the status of the distribution
                                    publicResponseService.createResponses(responses, distribution, responsesEvt -> {
                                        if (responsesEvt.isLeft()) {
                                            String message = "[FormulairePublic@createPublicResponses] Fail to create responses " + responses;
                                            renderInternalError(request, responsesEvt, message);
                                            return;
                                        }

                                        // Set cookie
                                        Cookie cookie = new CookieImpl(DISTRIBUTION_KEY_ + formKey, distributionKey);
                                        cookie.setPath("/formulaire-public");
                                        cookie.setSameSite(CookieSameSite.STRICT);
                                        request.response().addCookie(cookie);

                                        finishDistribution(request, distributionKey, formId, form);
                                    });
                                }));
                            }));
                        }));
                    });
                });
            });
        });
    }

    private void finishDistribution(HttpServerRequest request, String distributionKey, Integer formId, JsonObject form) {
        publicDistributionService.finishDistribution(distributionKey, finalDistributionEvt -> {
            if (finalDistributionEvt.isLeft()) {
                String message = "[FormulairePublic@createPublicResponses] Fail to finish distribution with key " + distributionKey;
                renderInternalError(request, finalDistributionEvt, message);
                return;
            }

            JsonObject finalDistribution = finalDistributionEvt.right().getValue();
            if (form.getBoolean(RESPONSE_NOTIFIED)) {
                publicFormService.listManagers(formId.toString(), listManagersEvt -> {
                    if (listManagersEvt.isLeft()) {
                        String message = "[FormulairePublic@createPublicResponses] Error in listing managers for form with id " + formId;
                        renderInternalError(request, listManagersEvt, message);
                        return;
                    }

                    JsonArray managers = listManagersEvt.right().getValue();
                    JsonArray managerIds = new JsonArray();
                    for (int j = 0; j < managers.size(); j++) {
                        managerIds.add(managers.getJsonObject(j).getString(ID));
                    }

                    publicNotifyService.notifyResponse(request, form, managerIds);
                    renderJson(request, finalDistribution, 200);
                });
            }
            else {
                renderJson(request, finalDistribution, 200);
            }
        });
    }
}