package fr.openent.formulaire.controllers;

import fr.openent.formulaire.helpers.RenderHelper;
import fr.openent.formulaire.helpers.UtilsHelper;
import fr.openent.formulaire.service.POCService;
import fr.openent.formulaire.service.QuestionChoiceService;
import fr.openent.formulaire.service.QuestionService;
import fr.openent.formulaire.service.SectionService;
import fr.openent.formulaire.service.impl.DefaultPOCService;
import fr.openent.formulaire.service.impl.DefaultQuestionChoiceService;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
import fr.openent.formulaire.service.impl.DefaultSectionService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserUtils;
import java.util.HashMap;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class POCController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(POCController.class);
    private final POCService pocService;
    private final SectionService sectionService;
    private final QuestionService questionService;
    private final QuestionChoiceService questionChoiceService;

    public POCController() {
        super();
        this.pocService = new DefaultPOCService();
        this.sectionService = new DefaultSectionService();
        this.questionService = new DefaultQuestionService();
        this.questionChoiceService = new DefaultQuestionChoiceService();
    }

    @Get("/public")
    @ApiDoc("Render view")
    public void renderPublic(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            JsonObject context = new JsonObject().put("notLoggedIn", user == null);
            renderView(request, context, "formulaire_public.html", null);
        });
    }

    @Get("/forms/key/:formKey")
    @ApiDoc("Create a distribution and get a specific form by key")
    public void getPublicFormByKey(HttpServerRequest request) {
        String formKey = request.getParam("formKey");

        // TODO checking

        pocService.getFormByKey(formKey, formEvent -> {
            if (formEvent.isLeft()) {
                log.error("[Formulaire@getPublicFormByKey] Fail to get form with key : " + formKey);
                RenderHelper.badRequest(request, formEvent);
                return;
            }

            JsonObject form = formEvent.right().getValue();
            String formId = form.getInteger("id").toString();
            sectionService.list(formId, sectionsEvent -> {
                if (sectionsEvent.isLeft()) {
                    log.error("[Formulaire@getPublicFormByKey] Fail to get sections for form with key : " + formKey);
                    RenderHelper.badRequest(request, formEvent);
                    return;
                }

                JsonArray sections = sectionsEvent.right().getValue();
                form.put("form_elements", sections);

                questionService.listForFormAndSection(formId, questionsEvent -> {
                    if (questionsEvent.isLeft()) {
                        log.error("[Formulaire@getPublicFormByKey] Fail to get questions for form with key : " + formKey);
                        RenderHelper.badRequest(request, formEvent);
                        return;
                    }

                    JsonArray questions = questionsEvent.right().getValue();
                    JsonArray questionIds = UtilsHelper.getIds(questions);

                    questionChoiceService.listChoices(questionIds, questionChoicesEvent -> {
                        if (questionChoicesEvent.isLeft()) {
                            log.error("[Formulaire@getPublicFormByKey] Fail to get choices for questions with ids : " + questionIds);
                            RenderHelper.badRequest(request, formEvent);
                            return;
                        }

                        JsonArray questionsChoices = questionChoicesEvent.right().getValue();
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

                        pocService.createDistribution(form, distributionEvent -> {
                            if (distributionEvent.isLeft()) {
                                log.error("[Formulaire@createPublicDistribution] Fail to create a distribution for form with key : " + formKey);
                                RenderHelper.badRequest(request, formEvent);
                                return;
                            }

                            form.put("distribution_key", distributionEvent.right().getValue().getInteger("id")); // TODO 'key' instead of 'id'
                            renderJson(request, form);
                        });
                    });
                });
            });
        });
    }

    @Post("/responses/:formKey/:distributionKey")
    @ApiDoc("Create multiple responses")
    public void createResponses(HttpServerRequest request) {
        String formKey = request.getParam("formKey");
        String distributionKey = request.getParam("distributionKey");

        // TODO check 'formKey' and 'distributionKey' are existing and 'distributionKey' match 'formKey'

        // TODO check 'distributionKey' not already with status FINISHED

        // TODO check all the question_ids if they match existing questions from the form with key 'formKey'

        // TODO if there's a choice it should match 'id' and 'value' of an existing QuestionChoice for this Question
        // TODO if it's a type 6 or 7 check parsing into Date or Time

        RequestUtils.bodyToJsonArray(request, responses -> {
            pocService.getDistributionByKey(distributionKey, distributionEvent -> {
                if (distributionEvent.isLeft()) {
                    log.error("[Formulaire@createPublicResponses] Fail to get distribution for key : " + distributionKey);
                    RenderHelper.badRequest(request, distributionEvent);
                    return;
                }

                JsonObject distribution = distributionEvent.right().getValue();
                pocService.createResponses(responses, distribution, responsesEvent -> {
                    if (responsesEvent.isLeft()) {
                        log.error("[Formulaire@createPublicResponses] Fail to create responses : " + responses);
                        RenderHelper.badRequest(request, responsesEvent);
                        return;
                    }

                    pocService.finishDistribution(distributionKey, defaultResponseHandler(request));
                });
            });
        });
    }
}