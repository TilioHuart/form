package fr.openent.formulaire.export;

import fr.openent.form.core.constants.DateFormats;
import fr.openent.form.core.enums.QuestionTypes;
import fr.openent.formulaire.service.DistributionService;
import fr.openent.formulaire.service.QuestionService;
import fr.openent.formulaire.service.ResponseService;
import fr.openent.formulaire.service.SectionService;
import fr.openent.formulaire.service.impl.DefaultDistributionService;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
import fr.openent.formulaire.service.impl.DefaultResponseService;
import fr.openent.formulaire.service.impl.DefaultSectionService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.Storage;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.entcore.common.pdf.Pdf;
import org.entcore.common.pdf.PdfFactory;
import org.entcore.common.pdf.PdfGenerator;

import static fr.openent.form.core.constants.ConfigFields.NODE_PDF_GENERATOR;
import static fr.openent.form.core.constants.Constants.*;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.helpers.RenderHelper.renderInternalError;
import static fr.wseduc.webutils.http.Renders.*;


public class FormResponsesExportPDF {
    private static final Logger log = LoggerFactory.getLogger(FormResponsesExportPDF.class);
    private String node;
    private final HttpServerRequest request;
    private final JsonObject config;
    private final Vertx vertx;
    private final Storage storage;
    private final Renders renders;
    private final JsonObject form;
    private final ResponseService responseService = new DefaultResponseService();
    private final QuestionService questionService = new DefaultQuestionService();
    private final SectionService sectionService = new DefaultSectionService();
    private final DistributionService distributionService = new DefaultDistributionService();
    private final SimpleDateFormat dateGetter = new SimpleDateFormat(DateFormats.YYYY_MM_DD_T_HH_MM_SS_SSS);
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat(DateFormats.DD_MM_YYYY_HH_MM);
    private final PdfFactory pdfFactory;

    public FormResponsesExportPDF(HttpServerRequest request, Vertx vertx, JsonObject config, Storage storage, JsonObject form) {
        this.request = request;
        this.config = config;
        this.vertx = vertx;
        this.storage = storage;
        this.renders = new Renders(this.vertx, config);
        this.form = form;
        dateFormatter.setTimeZone(TimeZone.getTimeZone(EUROPE_PARIS)); // TODO to adapt for not France timezone
        pdfFactory = new PdfFactory(vertx, new JsonObject().put(NODE_PDF_GENERATOR, config.getJsonObject(NODE_PDF_GENERATOR, new JsonObject())));
    }

    public void launch() {
        String formId = form.getInteger(ID).toString();
        questionService.getExportInfos(formId, true, getQuestionsEvt -> {
            if (getQuestionsEvt.isLeft()) {
                log.error("[Formulaire@FormResponsesExportPDF::launch] Failed to retrieve all questions for the form with id " + formId);
                renderInternalError(request, getQuestionsEvt);
                return;
            }
            if (getQuestionsEvt.right().getValue().isEmpty()) {
                String message = "[Formulaire@FormResponsesExportPDF::launch] No questions found for form with id " + formId;
                log.error(message);
                notFound(request, message);
                return;
            }

            JsonArray questionsInfo = getQuestionsEvt.right().getValue();
            sectionService.list(formId, getSectionsEvt -> {
                if (getSectionsEvt.isLeft()) {
                    log.error("[Formulaire@FormResponsesExportPDF::launch] Failed to retrieve all sections for the form with id " + formId);
                    renderInternalError(request, getSectionsEvt);
                    return;
                }

                JsonArray sectionsInfos = getSectionsEvt.right().getValue();
                distributionService.countFinished(form.getInteger(ID).toString(), countRepEvt -> {
                    if (countRepEvt.isLeft()) {
                        log.error("[Formulaire@FormResponsesExportPDF::launch] Failed to count nb responses for the form with id " + formId);
                        renderInternalError(request, countRepEvt);
                        return;
                    }
                    if (countRepEvt.right().getValue().isEmpty()) {
                        String message = "[Formulaire@FormResponsesExportPDF::launch] No distributions found for form with id " + formId;
                        log.error(message);
                        notFound(request, message);
                        return;
                    }

                    int nbResponseTot = countRepEvt.right().getValue().getInteger(COUNT);
                    boolean hasTooManyResponses = nbResponseTot > MAX_RESPONSES_EXPORT_PDF;

                    responseService.exportPDFResponses(formId, getResponsesEvt -> {
                        if (getResponsesEvt.isLeft()) {
                            log.error("[Formulaire@FormResponsesExportPDF::launch] Failed to get data for PDF export for the form with id " + formId);
                            renderInternalError(request, getResponsesEvt);
                            return;
                        }
                        if (getResponsesEvt.right().getValue().isEmpty()) {
                            String message = "[Formulaire@FormResponsesExportPDF::launch] No responses found for form with id " + formId;
                            log.error(message);
                            notFound(request, message);
                            return;
                        }

                        formatData(getResponsesEvt.right().getValue(), questionsInfo, sectionsInfos, hasTooManyResponses, formatDataEvt -> {
                            if (formatDataEvt.isLeft()) {
                                log.error("[Formulaire@FormResponsesExportPDF::launch] Failed to format data for the form with id" + formId);
                                renderInternalError(request, formatDataEvt);
                                return;
                            }

                            JsonObject results = formatDataEvt.right().getValue();
                            results.put(PARAM_NB_RESPONSE_TOT, nbResponseTot);

                            generatePDF(request, results,"results.xhtml", pdf ->
                                request.response()
                                        .putHeader("Content-Type", "application/pdf; charset=utf-8")
                                        .putHeader("Content-Disposition", "attachment; filename=Réponses_" + form.getString(TITLE) + ".pdf")
                                        .end(pdf)
                            );
                        });
                    });
                });
            });
        });
    }

    private void formatData(JsonArray data, JsonArray questionsInfo, JsonArray sectionsInfos, boolean hasTooManyResponses, Handler<Either<String, JsonObject>> handler) {
        JsonObject results = new JsonObject();
        JsonArray questions = new JsonArray();
        List<Future<String>> questionsGraphs = new ArrayList<>();

        // Fill final object with question's data
        int nbQuestions = questionsInfo.size();
        for (int i = 0; i < nbQuestions; i++) {
            JsonObject questionInfo = questionsInfo.getJsonObject(i);

            int question_type = questionInfo.getInteger(QUESTION_TYPE);
            boolean isGraph = GRAPH_QUESTIONS.contains(question_type);

            if (!hasTooManyResponses || isGraph) {
                String statementNotNull = questionInfo.getString(STATEMENT) == null ? "" : questionInfo.getString(STATEMENT);
                String formatedStatement = "<div>" + statementNotNull
                        .replace("\"", "'")
                        .replace("<o:p></o:p>", " ")
                        + "</div>";

                questions.add(
                    new JsonObject()
                    .put(ID, questionInfo.getInteger(ID))
                    .put(TITLE, questionInfo.getString(TITLE))
                    .put(QUESTION_TYPE, new JsonObject())
                    .put(STATEMENT, formatedStatement)
                    .put(MANDATORY, questionInfo.getBoolean(MANDATORY))
                    .put(SECTION_ID, questionInfo.getInteger(SECTION_ID))
                    .put(POSITION, questionInfo.getInteger(POSITION))
                    .put(RESPONSES, new JsonArray())
                    .put(HAS_CUSTOM_ANSWERS, false)
                );

                // Affect boolean to each type of answer (freetext, simple text, graph)
                // type_freetext (FREETEXT), type_text (SHORTANSWER, LONGANSWER, DATE, TIME, FILE), type_graph (SINGLEANSWER, MULTIPLEANSWER, MATRIX)
                questions.getJsonObject(questions.size() - 1).getJsonObject(QUESTION_TYPE)
                    .put(QUESTION_TYPE_ID, question_type)
                    .put(IS_TYPE_FREETEXT, question_type == QuestionTypes.FREETEXT.getCode())
                    .put(TYPE_TEXT, question_type != QuestionTypes.FREETEXT.getCode() && !isGraph)
                    .put(TYPE_GRAPH, isGraph)
                    .put(IS_CURSOR, question_type == QuestionTypes.CURSOR.getCode());


                // Prepare futures to get graph images
                if (GRAPH_QUESTIONS.contains(question_type)) {
                    questionsGraphs.add(getGraphData(questionInfo));
                }
            }
        }

        // Make a Map from the questions
        HashMap<Integer, JsonObject> mapQuestions = new HashMap<>();
        for (Object q : questions) {
            JsonObject question = (JsonObject)q;
            mapQuestions.put(question.getInteger(ID), question);
        }

        if (!hasTooManyResponses) {
            // Fill each question with responses' data
            for (int i = 0; i < data.size(); i++) {
                JsonObject response = data.getJsonObject(i);
                if (response == null) continue;

                // Format answer (empty string, simple text, html)
                JsonObject question = mapQuestions.get(response.getInteger(QUESTION_ID));
                Integer questionType = question != null ? question.getJsonObject(QUESTION_TYPE).getInteger(QUESTION_TYPE_ID) : null;
                if (response.getString(ANSWER, "").isEmpty()) {
                    response.put(ANSWER, "-");
                }
                if (questionType != null) {
                    if (questionType == QuestionTypes.FREETEXT.getCode()) {
                        response.put(ANSWER,
                                "<div>" + response.getString(ANSWER, "")
                                        .replace("<o:p></o:p>", " ")
                                        + "</div>");
                    }
                    else if (questionType == QuestionTypes.SHORTANSWER.getCode()) {
                        response.put(ANSWER, "<div>" + response.getString(ANSWER) + "</div>");
                    }
                    else if (questionType == QuestionTypes.LONGANSWER.getCode()) {
                        response.put(ANSWER, response.getString(ANSWER, "").replace("\"","'"));
                    }
                }

                // Format date_response
                try {
                    Date displayDate = dateGetter.parse(response.getString(DATE_RESPONSE));
                    response.put(DATE_RESPONSE, dateFormatter.format(displayDate));
                }
                catch (ParseException e) { e.printStackTrace(); }

                if (question != null) {
                    question.getJsonArray(RESPONSES).add(response);
                    if (question.getJsonObject(QUESTION_TYPE).getBoolean(IS_CURSOR)) {
                        question.put(NB_RESPONSES, question.getInteger(NB_RESPONSES, 0) + 1);
                        question.put(SUM_RESPONSES, question.getDouble(SUM_RESPONSES, 0d) + Double.parseDouble(response.getString(ANSWER)));
                        Double newAverage = question.getDouble(SUM_RESPONSES, 0d) / question.getInteger(NB_RESPONSES, 1);
                        question.put(CURSOR_AVERAGE, (double)Math.round(newAverage * 100d) / 100d);
                    }
                    if (!question.getBoolean(HAS_CUSTOM_ANSWERS) && response.getString(CUSTOM_ANSWER) != null) {
                        question.put(HAS_CUSTOM_ANSWERS, true);
                    }
                }
            }
        }

        // Get graph images, affect them to their respective questions and send the result
        Future.all(questionsGraphs).onComplete(evt -> {
            if (evt.failed()) {
                log.error("[Formulaire@FormResponsesExportPDF] Failed to retrieve graphs' data : " + evt.cause());
                Future.failedFuture(evt.cause());
                return;
            }

            // Affect graph images to corresponding questions (it is sorted so we just have to do it by following the order)
            int j = 0;
            for (int i = 0; i < questions.size(); i++) {
                JsonObject question = questions.getJsonObject(i);
                if (question.getJsonObject(QUESTION_TYPE).getBoolean(TYPE_GRAPH)) {
                    String graphData = questionsGraphs.get(j).result().toString();
                    question.put(PARAM_GRAPH_DATA, graphData);
                    j++;
                }
            }

            JsonArray form_elements = fillFormElements(sectionsInfos, questions);

            // Finish to fill final object with useful form's data
            results.put(FORM_TITLE, form.getString(TITLE));
            results.put(ANONYMOUS, form.getBoolean(ANONYMOUS));
            results.put(FORM_ELEMENTS, form_elements);
            handler.handle(new Either.Right<>(results));
        });
    }

    private JsonArray fillFormElements(JsonArray sections, JsonArray questions) {
        HashMap<Integer, JsonObject> mapSectionsId = new HashMap<>();
        HashMap<Integer, JsonObject> mapSectionsPosition = new HashMap<>();
        for (Object s : sections) {
            JsonObject section = (JsonObject)s;
            if(section.containsKey(DESCRIPTION) && section.getString(DESCRIPTION) != null) {
                section.put(DESCRIPTION,
                        "<div>" + section.getString(DESCRIPTION)
                                .replace("\"","'")
                                .replace("<o:p></o:p>", " ")
                                + "</div>"
                );
            }
            section.put(QUESTIONS, new JsonArray());
            mapSectionsId.put(section.getInteger(ID), section);
            mapSectionsPosition.put(section.getInteger(POSITION), section);
        }

        SortedMap<Integer, JsonObject> form_elements = new TreeMap<>();
        int i = 0;
        while (i < questions.size()) {
            JsonObject question = questions.getJsonObject(i);
            if (question.getInteger(SECTION_ID) != null) {
                JsonObject section = mapSectionsId.get(question.getInteger(SECTION_ID));
                section.getJsonArray(QUESTIONS).add(question);
            }
            else {
                question.put(IS_QUESTION, true);
                form_elements.put(question.getInteger(POSITION), question);
            }
            i++;
        }
        form_elements.putAll(mapSectionsPosition);

        return new JsonArray(new ArrayList<>(form_elements.values()));
    }

    private Future<String> getGraphData(JsonObject question) {
        Promise<String> promise = Promise.promise();
        String idFile = form.getJsonObject(IMAGES).getJsonObject(PARAM_ID_IMAGES_PER_QUESTION).getString(question.getLong(ID).toString());

        if (idFile != null) {
            storage.readFile(idFile, readFileEvt -> {
                String graph = readFileEvt.getString(0, readFileEvt.length());
                promise.complete(graph);
            });
        }
        else {
            log.error("[Formulaire@getImage] : Wrong file id.");
            promise.complete("Wrong file id");
        }
        return promise.future();
    }

    private void generatePDF(HttpServerRequest request, JsonObject templateProps, String templateName, Handler<Buffer> handler) {
        Promise<Pdf> promise = Promise.promise();
        final String templatePath = "./public/template/pdf/";
        final String baseUrl = getScheme(request) + "://" + Renders.getHost(request) + config.getString("app-address") + "/public/";

        final String path = FileResolver.absolutePath(templatePath + templateName);

        vertx.fileSystem().readFile(path, result -> {
            if (!result.succeeded()) {
                badRequest(request);
                return;
            }

            StringReader reader = new StringReader(result.result().toString(StandardCharsets.UTF_8));
            renders.processTemplate(request, templateProps, templateName, reader, writer -> {
                String processedTemplate = ((StringWriter) writer).getBuffer().toString();
                if (processedTemplate.isEmpty()) {
                    badRequest(request);
                    return;
                }
                JsonObject actionObject = new JsonObject();
                byte[] bytes;
                bytes = processedTemplate.getBytes(StandardCharsets.UTF_8);

                node = (String) vertx.sharedData().getLocalMap("server").get("node");
                if (node == null) {
                    node = "";
                }

                actionObject.put("content", bytes).put("baseUrl", baseUrl);
                generatePDF(TITLE, processedTemplate)
                    .onSuccess(res -> {
                        handler.handle(res.getContent());

                        // Remove image files generated for graph display on PDF
                        JsonArray removesFiles = templateProps.getJsonArray(PARAM_ID_IMAGES_FILES);
                        if (removesFiles != null) {
                            storage.removeFiles(removesFiles, removeEvt -> {
                                log.info(" [Formulaire@FormResponsesExportPDF::generatePDF] " + removeEvt.encode());
                            });
                        }
                    })
                    .onFailure(error -> {
                        String message = "[Formulaire@FormResponsesExportPDF::generatePDF] Failed to generatePDF : " + error.getMessage();
                        log.error(message);
                        promise.fail(message);
                    });
            });
        });
    }

    public Future<Pdf> generatePDF(String filename, String buffer) {
        Promise<Pdf> promise = Promise.promise();
        try {
            PdfGenerator pdfGenerator = pdfFactory.getPdfGenerator();
            pdfGenerator.generatePdfFromTemplate(filename, buffer, ar -> {
                if (ar.failed()) {
                    log.error("[Formulaire@FormResponsesExportPDF::generatePDF] Failed to generatePdfFromTemplate : " + ar.cause().getMessage());
                    promise.fail(ar.cause().getMessage());
                } else {
                    promise.complete(ar.result());
                }
            });
        }
        catch (Exception e) {
            log.error("[Formulaire@FormResponsesExportPDF::generatePDF] Failed to generatePDF: " + e.getMessage());
            promise.fail(e.getMessage());
        }
        return promise.future();
    }
}