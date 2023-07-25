package fr.openent.formulaire.export;

import fr.openent.form.core.enums.I18nKeys;
import fr.openent.form.core.enums.QuestionTypes;
import fr.openent.form.core.models.Form;
import fr.openent.form.helpers.FutureHelper;
import fr.openent.form.helpers.I18nHelper;
import fr.openent.formulaire.service.QuestionChoiceService;
import fr.openent.formulaire.service.QuestionService;
import fr.openent.formulaire.service.QuestionSpecificFieldsService;
import fr.openent.formulaire.service.SectionService;
import fr.openent.formulaire.service.impl.DefaultQuestionChoiceService;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
import fr.openent.formulaire.service.impl.DefaultQuestionSpecificFieldsService;
import fr.openent.formulaire.service.impl.DefaultSectionService;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.pdf.Pdf;
import org.entcore.common.pdf.PdfFactory;
import org.entcore.common.pdf.PdfGenerator;
import org.entcore.common.storage.Storage;
import io.vertx.core.Promise;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.ConfigFields.NODE_PDF_GENERATOR;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.helpers.UtilsHelper.getIds;

public class FormQuestionsExportPDF extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(FormQuestionsExportPDF.class);
    private String node;
    private final HttpServerRequest request;
    private final JsonObject config;
    private final Vertx vertx;
    private final Renders renders;
    private final Form form;
    private final QuestionService questionService = new DefaultQuestionService();
    private final SectionService sectionService = new DefaultSectionService();
    private final QuestionSpecificFieldsService questionSpecificFieldsService = new DefaultQuestionSpecificFieldsService();
    private final QuestionChoiceService questionChoiceService = new DefaultQuestionChoiceService();
    private final PdfFactory pdfFactory;
    private final Storage storage;
    private final EventBus eb;
    private JsonArray questionsInfos;
    private JsonArray sectionsInfos;
    private JsonObject choicesInfos;
    private JsonArray matrixChildren;

    public FormQuestionsExportPDF(HttpServerRequest request, Vertx vertx, JsonObject config, Storage storage, EventBus eb, Form form) {
        this.request = request;
        this.config = config;
        this.vertx = vertx;
        this.renders = new Renders(this.vertx, config);
        this.form = form;
        this.storage = storage;
        this.eb = eb;
        this.questionsInfos = new JsonArray();
        this.sectionsInfos = new JsonArray();
        this.choicesInfos = new JsonObject();
        this.matrixChildren = new JsonArray();
        pdfFactory = new PdfFactory(vertx, new JsonObject().put(NODE_PDF_GENERATOR, config.getJsonObject(NODE_PDF_GENERATOR, new JsonObject())));
    }


    // Getters / Setters

    public JsonArray getQuestionsInfos() {
        return this.questionsInfos;
    }

    public void setQuestionsInfos(JsonArray questionsInfos) {
        this.questionsInfos = questionsInfos;
    }

    public JsonArray getSectionsInfos() {
        return this.sectionsInfos;
    }

    public void setSectionsInfos(JsonArray sectionsInfos) {
        this.sectionsInfos = sectionsInfos;
    }

    public JsonObject getChoicesInfos() {
        return this.choicesInfos;
    }

    public void setChoicesInfos(JsonObject choicesInfos) {
        this.choicesInfos = choicesInfos;
    }

    public JsonArray getMatrixChildren() {
        return this.matrixChildren;
    }

    public void setMatrixChildren(JsonArray matrixChildren) {
        this.matrixChildren = matrixChildren;
    }


    // Functions

    public Future<JsonObject> launch() {
        Promise<JsonObject> promise = Promise.promise();
        String formId = form.getId().toString();
        AtomicReference<JsonArray> questionsIds = new AtomicReference<>(new JsonArray());
        Map<Integer, JsonObject> mapQuestions = new HashMap<>();
        Map<Integer, JsonObject> mapSections = new HashMap<>();
        List<Future<JsonObject>> imageInfos = new ArrayList<>();
        JsonArray formElements = new JsonArray();
        Map<String, String> localChoicesMap = new HashMap<>();

        sectionService.list(formId)
            .compose(sectionData -> {
                setSectionsInfos(sectionData);
                return questionService.getExportInfos(formId, true);
            })
            .compose(questionData -> {
                if (questionData.isEmpty()) {
                    String errMessage = "No questions found for form with id " + formId;
                    log.error("[Formulaire@FormQuestionsExportPDF::fetchQuestionsInfos] " + errMessage);
                    return Future.failedFuture(errMessage);
                }
                setQuestionsInfos(questionData);
                questionsIds.set(getIds(questionsInfos));
                fillMap(sectionsInfos, mapSections);
                fillMap(questionsInfos, mapQuestions);
                return questionSpecificFieldsService.syncQuestionSpecs(questionsInfos);
            })
            .compose(questionsWithSpecifics -> questionChoiceService.listChoices(questionsIds.get()))
            .compose(listChoices -> {
                JsonObject listChoicesInfos = new JsonObject();
                listChoicesInfos.put(QUESTIONS_CHOICES, listChoices);
                setChoicesInfos(listChoicesInfos);
                return questionService.listChildren(questionsIds.get());
            })
            .compose(listChildren -> {
                setMatrixChildren(listChildren);
                fillChoices(choicesInfos, mapSections, mapQuestions, imageInfos);
                return FutureHelper.all(imageInfos);
            })
            .compose(imageInfosResults -> {
                //Get choices images, affect them to their respective choice and send the result
                fillChoicesImages(imageInfos, localChoicesMap, choicesInfos);
                fillMatrixQuestions(questionsInfos, matrixChildren);
                fillQuestionsAndSections(questionsInfos, choicesInfos, mapSections, formElements);

                List<JsonObject> sorted_form_elements = formElements.getList();
                sorted_form_elements.removeIf(element -> element.getInteger(POSITION) == null);
                sorted_form_elements.sort(Comparator.nullsFirst(Comparator.comparingInt(a -> a.getInteger(POSITION))));
                JsonObject results = new JsonObject()
                        .put(FORM_ELEMENTS, sorted_form_elements)
                        .put(FORM_TITLE, form.getTitle());

                return generatePDF(request, results,"questions.xhtml");
            })
            .compose(buffer -> {
                JsonObject pdfInfos = new JsonObject()
                        .put(TITLE, "Questions_" + form.getTitle() + ".pdf");
                return uploadPdfAndSetFileId(pdfInfos, buffer);
            })
            .onSuccess(promise::complete)
            .onFailure(err -> {
                String errMessage = String.format("[Formulaire@FormQuestionsExportPDF::launch] Failed to export PDF for form with id " + formId + " : " + err.getMessage());
                log.error(errMessage);
                promise.fail(err.getMessage());
            });

        return promise.future();
    }


    /**
     * Fill questions with their choices and sections with their questions
     * @param imageInfos all the datas from the images
     * @param localChoicesMap a map with id of image as key and image Datas as value
     * @param choicesInfos all choices infos to put in questions
     */
    private void fillChoicesImages(List<Future<JsonObject>> imageInfos, Map<String, String> localChoicesMap, JsonObject choicesInfos) {
        imageInfos.stream()
                .map(Future::result)
                .map(JsonObject.class::cast)
                .filter(Objects::nonNull)
                .filter(imageInfo -> imageInfo.containsKey(ID) && imageInfo.containsKey(DATA))
                .forEach(imageInfo -> localChoicesMap.put(imageInfo.getString(ID), imageInfo.getString(DATA)));

        // Affect images to corresponding choices (we have to iterate like previously to keep the same order)
        choicesInfos.getJsonArray(QUESTIONS_CHOICES).stream()
                .filter(Objects::nonNull)
                .map(JsonObject.class::cast)
                .filter(choice -> choice.containsKey(IMAGE))
                .forEach(choice -> {
                    String choiceImageId = getImageId(choice);
                    String imageData = localChoicesMap.get(choiceImageId);
                    choice.put(PARAM_CHOICE_IMAGE, imageData);
                });
    }


    /**
     * Fill questions with their choices and sections with their questions
     * @param questionChoices all the datas from the choices
     * @param mapSections a map with id of section as key and sectionsInfos as value
     * @param mapQuestions a map with id of question as key and questionsInfos as value
     * @param imageInfos all the datas from the images
     */
    private void fillChoices(JsonObject questionChoices, Map<Integer, JsonObject> mapSections, Map<Integer, JsonObject> mapQuestions, List<Future<JsonObject>> imageInfos) {
        questionChoices.getJsonArray(QUESTIONS_CHOICES).stream()
                .filter(Objects::nonNull)
                .map(JsonObject.class::cast)
                .forEach(choice -> {
                    if (choice.containsKey(IMAGE) && choice.getString(IMAGE) != null) imageInfos.add(getImageData(choice));
                    Integer nextQuestionId = choice.getInteger(NEXT_FORM_ELEMENT_ID);
                    if (nextQuestionId != null) {
                        String titleNext = null;
                        JsonObject nextQuestion = mapQuestions.get(nextQuestionId);
                        JsonObject nextSection = mapSections.get(nextQuestionId);
                        if (nextQuestion != null) {
                            titleNext = nextQuestion.getString(TITLE);
                        } else if (nextSection != null) {
                            titleNext = nextSection.getString(TITLE);
                        }
                        if (titleNext != null) {
                            choice.put(TITLE_NEXT, titleNext);
                        }
                    } else {
                        choice.put(TITLE_NEXT, I18nHelper.getI18nValue(I18nKeys.END_FORM, request));
                    }
                });
    }


    /**
     * Handle Matrix questions and its children
     * @param questionsInfos all questionsInfos when not filled yet
     * @param matrixChildren columns and lines of a matrix question
     */
    private void fillMatrixQuestions(JsonArray questionsInfos, JsonArray matrixChildren) {
        List<JsonObject> childrenList = matrixChildren.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .sorted(Comparator.nullsFirst(Comparator.comparingInt(a -> a.getInteger(MATRIX_POSITION))))
                .collect(Collectors.toList());
        JsonArray children = new JsonArray(childrenList);
        questionsInfos.stream()
            .filter(Objects::nonNull)
            .map(JsonObject.class::cast)
            .forEach(question -> {
                for (int k = 0; k < children.size(); k++) {
                    JsonObject child = children.getJsonObject(k);
                    if (Objects.equals(child.getInteger(MATRIX_ID), question.getInteger(ID))) {
                        if (question.containsKey(CHILDREN)) {
                            question.getJsonArray(CHILDREN).add(child);
                            if (Integer.valueOf(QuestionTypes.SINGLEANSWERRADIO.getCode()).equals(child.getInteger(QUESTION_TYPE))) {
                                question.put(IS_MATRIX_SINGLE, true);
                            }
                            if (Integer.valueOf(QuestionTypes.MULTIPLEANSWER.getCode()).equals(child.getInteger(QUESTION_TYPE))) {
                                question.put(IS_MATRIX_MULTIPLE, true);
                            }

                        } else {
                            question.put(CHILDREN, new JsonArray().add(child));
                        }
                    }
                }
            });
    }


    /**
     * Fill questions with their choices and sections with their questions
     * @param questionsInfos all questionsInfos when not filled yet
     * @param choicesInfos all choices infos to put in questions
     * @param mapSections a map with id of section as key and sectionsInfos as value
     * @param formElements the datas which will be sent to the PDF template
     */
    private void fillQuestionsAndSections(JsonArray questionsInfos, JsonObject choicesInfos, Map<Integer, JsonObject> mapSections, JsonArray formElements) {
        questionsInfos.stream()
                .map(JsonObject.class::cast)
                .forEach(question -> {
                    //Set isQuestion && is_Conditional && type
                    if (question.getInteger(SECTION_ID) == null)question.put(IS_QUESTION, true);
                    setType(question);
                    JsonArray choices = choicesInfos.getJsonArray(QUESTIONS_CHOICES).stream()
                            .map(JsonObject.class::cast)
                            .filter(choice -> Objects.equals(choice.getInteger(QUESTION_ID), question.getInteger(ID)))
                            .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

                    List<JsonObject> choicesList = choices.stream()
                            .filter(JsonObject.class::isInstance)
                            .map(JsonObject.class::cast)
                            .sorted(Comparator.nullsFirst(Comparator.comparingInt(a -> a.getInteger(POSITION))))
                            .collect(Collectors.toList());
                    choices = new JsonArray(choicesList);
                    question.put(CHOICES, choices);

                    if (mapSections.containsKey(question.getInteger(SECTION_ID))) {
                        JsonObject section = mapSections.get(question.getInteger(SECTION_ID));
                        section.put(QUESTIONS, question);
                    } else {
                        formElements.add(question);
                    }
                });

        mapSections.values().stream()
                .filter(Objects::nonNull)
                .map(JsonObject.class::cast)
                .forEach(section -> {
                    section.put(IS_QUESTION, false);
                    formElements.add(section);
                });
    }

    /**
     * Fill a HashMap with the id of JsonObjects as key and JsonObject as value
     * @param jsonArray array to be streamed
     * @param hashMap to be filled
     */
    private void fillMap(JsonArray jsonArray, Map<Integer, JsonObject> hashMap) {
        jsonArray.stream()
                .filter(Objects::nonNull)
                .map(JsonObject.class::cast)
                .forEach(infos -> {
                    hashMap.put(infos.getInteger(ID), infos);
                });
    }

    /**
     * Take a question and add its type corresponding to the question type integer value
     * @param question question which type must be defined
     */
    private void setType(JsonObject question) {
        switch (QuestionTypes.values()[question.getInteger(QUESTION_TYPE) - 1]) {
            case FREETEXT:
                question.put(IS_TYPE_FREETEXT, true);
                break;
            case SHORTANSWER:
                question.put(IS_SHORT_ANSWER, true);
                break;
            case LONGANSWER:
                question.put(IS_LONG_ANSWER, true);
                break;
            case SINGLEANSWERRADIO:
            case SINGLEANSWER:
                question.put(IS_RADIO_BTN, true);
                break;
            case MULTIPLEANSWER:
                question.put(IS_MULTIPLE_CHOICE, true);
                break;
            case DATE:
            case TIME:
                question.put(IS_DATE_HOUR, true);
                break;
            case MATRIX:
                question.put(IS_MATRIX, true);
                break;
            case CURSOR:
                question.put(IS_CURSOR, true);
                break;
            case RANKING:
                question.put(IS_RANKING, true);
                break;
            default:
                break;
        }
    }

    // Get image data thanks to its id
    public Future<JsonObject> getImageData(JsonObject choice) {
        Promise<JsonObject> promise = Promise.promise();

        String documentId = getImageId(choice);
        if (documentId == null || documentId.equals("")) {
            String errorMessage = "[Formulaire@FormQuestionsExportPDF::getImageData] The document id must not be empty.";
            log.error(errorMessage);
            promise.complete(null);
            return promise.future();
        }

        WorkspaceHelper workspaceHelper = new WorkspaceHelper(eb, storage);
        workspaceHelper.readDocument(documentId, documentEvt -> {
            String graph = Base64.getEncoder().encodeToString(documentEvt.getData().getBytes());
            graph = "data:" + documentEvt.getDocument().getJsonObject(METADATA, new JsonObject()).getString(CONTENT_TYPE, "image/png") + ";base64," + graph;
            JsonObject imageInfos = new JsonObject().put(ID, documentId).put(DATA, graph);
            promise.complete(imageInfos);
        });

        return promise.future();
    }

    private String getImageId(JsonObject questionChoice) {
        String imagePath = questionChoice.getString(IMAGE);
        return getImageId(imagePath);
    }

    private String getImageId(String imagePath) {
        if (imagePath == null) return null;
        int lastSeparator = imagePath.lastIndexOf(SLASH);
        return lastSeparator > 0 ? imagePath.substring(lastSeparator + 1) : imagePath;
    }

    private Future<Buffer> generatePDF(HttpServerRequest request, JsonObject templateProps, String templateName) {
        Promise<Buffer> promise = Promise.promise();
        final String templatePath = "./public/template/pdf/";
        final String baseUrl = getScheme(request) + "://" + Renders.getHost(request) + config.getString("app-address") + "/public/";

        final String path = FileResolver.absolutePath(templatePath + templateName);

        vertx.fileSystem().readFile(path, result -> {
            if (!result.succeeded()) {
                String message = "Failed to read template file.";
                log.error("[Formulaire@FormQuestionsExportPDF::generatePDF] " + message);
                promise.fail(message);
                return;
            }

            StringReader reader = new StringReader(result.result().toString(StandardCharsets.UTF_8));
            renders.processTemplate(request, templateProps, templateName, reader, writer -> {
                String processedTemplate = ((StringWriter) writer).getBuffer().toString();
                if (processedTemplate.isEmpty()) {
                    String message = "Failed to process template.";
                    log.error("[Formulaire@FormQuestionsExportPDF::generatePDF] " + message);
                    promise.fail(message);
                    return;
                }

                JsonObject actionObject = new JsonObject();
                byte[] bytes;
                bytes = processedTemplate.getBytes(StandardCharsets.UTF_8);

                node = (String) vertx.sharedData().getLocalMap(SERVER).get(NODE);
                if (node == null) {
                    node = "";
                }

                actionObject.put(CONTENT, bytes).put(BASE_URL, baseUrl);
                generatePDF(templateProps.getString(FORM_TITLE), processedTemplate)
                    .onSuccess(res -> promise.complete(res.getContent()))
                    .onFailure(error -> {
                        String message = "[Formulaire@FormQuestionsExportPDF::generatePDF] Failed to generatePDF : " + error.getMessage();
                        log.error(message);
                        promise.fail(error.getMessage());
                    });
            });
        });

        return promise.future();
    }

    public Future<Pdf> generatePDF(String filename, String buffer) {
        Promise<Pdf> promise = Promise.promise();
        try {
            PdfGenerator pdfGenerator = pdfFactory.getPdfGenerator();
            pdfGenerator.generatePdfFromTemplate(filename, buffer, ar -> {
                if (ar.failed()) {
                    log.error("[Formulaire@FormQuestionsExportPDF::generatePDF] Failed to generatePdfFromTemplate : " + ar.cause().getMessage());
                    promise.fail(ar.cause().getMessage());
                } else {
                    promise.complete(ar.result());
                }
            });
        }
        catch (Exception e) {
            log.error("[Formulaire@FormQuestionsExportPDF::generatePDF] Failed to generatePDF : " + e.getMessage());
            promise.fail(e.getMessage());
        }
        return promise.future();
    }

    private Future<JsonObject> uploadPdfAndSetFileId(JsonObject pdfInfos, Buffer buffer) {
        Promise<JsonObject> promise = Promise.promise();
        storage.writeBuffer(buffer, "application/pdf", pdfInfos.getString(TITLE), uploadEvt -> {
            if (!uploadEvt.getString(STATUS).equals(OK)) {
                String errorMessage = "[Formulaire@FormQuestionsExportPDF::uploadPdf] Failed to upload PDF to storage : " + uploadEvt.getString(MESSAGE);
                log.error(errorMessage);
                promise.fail(uploadEvt.getString(MESSAGE));
                return;
            }
            pdfInfos.put(FILE_ID, uploadEvt.getString(_ID));
            promise.complete(pdfInfos);
        });
        return promise.future();
    }
}
