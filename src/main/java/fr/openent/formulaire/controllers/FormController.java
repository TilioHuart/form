package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.export.FormResponsesExportCSV;
import fr.openent.formulaire.export.FormResponsesExportPDF;
import fr.openent.formulaire.helpers.FutureHelper;
import fr.openent.formulaire.helpers.RenderHelper;
import fr.openent.formulaire.helpers.UtilsHelper;
import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.security.CreationRight;
import fr.openent.formulaire.security.ResponseRight;
import fr.openent.formulaire.security.ShareAndOwner;
import fr.openent.formulaire.service.*;
import fr.openent.formulaire.service.impl.*;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.http.response.DefaultResponseHandler;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.*;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class FormController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(FormController.class);
    private final EventStore eventStore;
    private final Storage storage;
    private final FormService formService;
    private final DistributionService distributionService;
    private final QuestionChoiceService questionChoiceService;
    private final ResponseFileService responseFileService;
    private final FormSharesService formShareService;
    private final FolderService folderService;
    private final RelFormFolderService relFormFolderService;
    private final NeoService neoService;
    private final NotifyService notifyService;

    public FormController(EventStore eventStore, Storage storage, TimelineHelper timelineHelper) {
        super();
        this.eventStore = eventStore;
        this.storage = storage;
        this.formService = new DefaultFormService();
        this.distributionService = new DefaultDistributionService();
        this.questionChoiceService = new DefaultQuestionChoiceService();
        this.responseFileService = new DefaultResponseFileService();
        this.formShareService = new DefaultFormSharesService();
        this.folderService = new DefaultFolderService();
        this.relFormFolderService = new DefaultRelFormFolderService();
        this.neoService = new DefaultNeoService();
        this.notifyService = new DefaultNotifyService(timelineHelper, eb);
    }

    // Init classic rights

    @SecuredAction(Formulaire.CREATION_RIGHT)
    public void initCreationRight(final HttpServerRequest request) {
    }

    @SecuredAction(Formulaire.RESPONSE_RIGHT)
    public void initResponseRight(final HttpServerRequest request) {
    }

    // Init sharing rights

    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void initContribResourceRight(final HttpServerRequest request) {
    }

    @SecuredAction(value = Formulaire.MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void initManagerResourceRight(final HttpServerRequest request) {
    }

    @SecuredAction(value = Formulaire.RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void initResponderResourceRight(final HttpServerRequest request) {
    }

    // API

    @Get("/forms")
    @ApiDoc("List all the forms created by me or shared with me")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void list(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                final List<String> groupsAndUserIds = new ArrayList<>();
                groupsAndUserIds.add(user.getUserId());
                if (user.getGroupsIds() != null) {
                    groupsAndUserIds.addAll(user.getGroupsIds());
                }
                formService.list(groupsAndUserIds, user, arrayResponseHandler(request));
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Get("/sentForms")
    @ApiDoc("List all the forms sent to me")
    @ResourceFilter(ResponseRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listSentForms(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                formService.listSentForms(user, arrayResponseHandler(request));
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Get("/linker")
    @ApiDoc("List all the forms for the linker")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listForLinker(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                final List<String> groupsAndUserIds = new ArrayList<>();
                groupsAndUserIds.add(user.getUserId());
                if (user.getGroupsIds() != null) {
                    groupsAndUserIds.addAll(user.getGroupsIds());
                }
                formService.listForLinker(groupsAndUserIds, user, arrayResponseHandler(request));
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Get("/forms/:formId")
    @ApiDoc("Get a specific form by id")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(HttpServerRequest request) {
        String formId = request.getParam("formId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                log.error("User not found in session.");
                Renders.unauthorized(request);
                return;
            }
            formService.get(formId, user, defaultResponseHandler(request));
        });
    }

    @Post("/forms")
    @ApiDoc("Create a form")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void create(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJson(request, form -> {
                    formService.create(form, user, createEvent -> {
                        if (createEvent.isLeft()) {
                            log.error("[Formulaire@create] Failed to create form : " + form);
                            RenderHelper.badRequest(request, createEvent);
                            return;
                        }

                        eventStore.createAndStoreEvent(Formulaire.FormulaireEvent.CREATE.name(), request);
                        String formId = createEvent.right().getValue().getInteger("id").toString();
                        Integer folderId = form.getInteger("folder_id");
                        relFormFolderService.create(user, new JsonArray().add(formId), folderId.toString(), createRelEvent -> {
                            if (createRelEvent.isLeft()) {
                                log.error("[Formulaire@create] Failed to create relation form-folder for form : " + form);
                                RenderHelper.badRequest(request, createRelEvent);
                                return;
                            }

                            if (folderId != Formulaire.ID_ROOT_FOLDER) { // We do not sync root folder counts (useless)
                                folderService.syncNbChildren(user, folderId.toString(), syncEvent -> {
                                    if (syncEvent.isLeft()) {
                                        log.error("[Formulaire@moveForm] Error in sync children counts for folder " + folderId);
                                        RenderHelper.badRequest(request, syncEvent);
                                        return;
                                    }
                                    renderJson(request, createEvent.right().getValue());
                                });
                            }
                            else {
                                renderJson(request, createEvent.right().getValue());
                            }
                        });
                    });
                });
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Post("/forms/multiple")
    @ApiDoc("Create several forms")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void createMultiple(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJsonArray(request, forms -> {
                    formService.createMultiple(forms, user, createEvent -> {
                        if (createEvent.isLeft()) {
                            log.error("[Formulaire@createMultiple] Failed to create forms : " + forms);
                            RenderHelper.badRequest(request, createEvent);
                            return;
                        }

                        eventStore.createAndStoreEvent(Formulaire.FormulaireEvent.CREATE.name(), request);
                        JsonArray formIds = UtilsHelper.getIds(forms);
                        Integer folderId = forms.getJsonObject(0).getInteger("folder_id");
                        relFormFolderService.create(user, formIds, folderId.toString(), createRelEvent -> {
                            if (createRelEvent.isLeft()) {
                                log.error("[Formulaire@create] Failed to create relation form-folder for forms : " + formIds);
                                RenderHelper.badRequest(request, createRelEvent);
                                return;
                            }

                            if (folderId != Formulaire.ID_ROOT_FOLDER) { // We do not sync root folder counts (useless)
                                folderService.syncNbChildren(user, folderId.toString(), syncEvent -> {
                                    if (syncEvent.isLeft()) {
                                        log.error("[Formulaire@moveForm] Error in sync children counts for folder " + folderId);
                                        RenderHelper.badRequest(request, syncEvent);
                                        return;
                                    }
                                    renderJson(request, createEvent.right().getValue());
                                });
                            }
                            else {
                                renderJson(request, createEvent.right().getValue());
                            }

                            renderJson(request, createEvent.right().getValue());
                        });
                    });
                });
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Post("/forms/duplicate/:folderId")
    @ApiDoc("Duplicate several forms")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void duplicate(HttpServerRequest request) {
        Integer folderId = Integer.parseInt(request.getParam("folderId"));
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJsonArray(request, formIds -> {
                    List<Future> formsInfos = new ArrayList<>();
                    // Duplicates form and all questions inside
                    duplicatesFormsQuestions(request, folderId, user, formIds, formsInfos);
                });
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    private void duplicatesFormsQuestions(HttpServerRequest request, Integer folderId, UserInfos user, JsonArray formIds, List<Future> formsInfos) {
        for (int i = 0; i < formIds.size(); i++) {
            Promise<JsonArray> promise = Promise.promise();
            formsInfos.add(promise.future());
            formService.duplicate(formIds.getInteger(i), user, FutureHelper.handlerJsonArray(promise));
        }
        CompositeFuture.all(formsInfos).onComplete(evt -> {
            if (evt.failed()) {
                log.error("[Formulaire@duplicate] Failed to retrieve info : " + evt.cause());
                badRequest(request);
            }
            // Duplicates all potential question choices
            duplicatesQuestionChoices(request, folderId, user, formIds, evt);
        });
    }

    private void duplicatesQuestionChoices(HttpServerRequest request, Integer folderId, UserInfos user, JsonArray formIds, AsyncResult<CompositeFuture> evt) {
        List<Future> questionsInfosFutures = new ArrayList<>();

        for (Object questions : evt.result().list()) {
            JsonArray questionsInfos = ((JsonArray) questions);
            if(questionsInfos.getJsonObject(0).getInteger("id") != null){
                for (int i = 0; i < questionsInfos.size(); i++) {
                    JsonObject questionInfo = questionsInfos.getJsonObject(i);
                    int formId = questionInfo.getInteger("form_id");
                    int questionId = questionInfo.getInteger("id");
                    int originalQuestionId = questionInfo.getInteger("original_question_id");
                    int question_type = questionInfo.getInteger("question_type");
                    if (question_type == 4 || question_type == 5 || question_type == 9) {
                        Promise<JsonObject> promise = Promise.promise();
                        questionsInfosFutures.add(promise.future());
                        questionChoiceService.duplicate(formId, questionId, originalQuestionId, FutureHelper.handlerJsonObject(promise));
                    }
                }
            }
        }
        CompositeFuture.all(questionsInfosFutures).onComplete(evt1 -> {
            if (evt1.failed()) {
                log.error("[Formulaire@duplicate] Failed to retrieve info from questions : " + evt1.cause());
                badRequest(request);
            }
            // Sync folders with this new forms
            syncFoldersForms(request, folderId, user, formIds, evt);
        });
    }

    private void syncFoldersForms(HttpServerRequest request, Integer folderId, UserInfos user, JsonArray formIds, AsyncResult<CompositeFuture> evt) {
        eventStore.createAndStoreEvent(Formulaire.FormulaireEvent.CREATE.name(), request);
        List<Object> questions = evt.result().list();
        JsonArray newFormIds = new JsonArray();
        for (Object question : questions) {
            newFormIds.add(((JsonArray) question).getJsonObject(0).getInteger("form_id"));
        }
        relFormFolderService.create(user, newFormIds, folderId.toString(), createRelEvent -> {
            if (createRelEvent.isLeft()) {
                log.error("[Formulaire@moveForm] Error in moving forms " + formIds);
                RenderHelper.badRequest(request, createRelEvent);
                return;
            }

            if (folderId != Formulaire.ID_ROOT_FOLDER) { // We do not sync root folder counts (useless)
                folderService.syncNbChildren(user, folderId.toString(), defaultResponseHandler(request));
            }
            else {
                renderJson(request, createRelEvent.right().getValue());
            }
        });
    }

    @Put("/forms/:formId")
    @ApiDoc("Update a specific form")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void update(HttpServerRequest request) {
        String formId = request.getParam("formId");
        RequestUtils.bodyToJson(request, form -> {
            formService.update(formId, form, defaultResponseHandler(request));
        });
    }

    @Delete("/forms/:formId")
    @ApiDoc("Delete a specific form")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void delete(HttpServerRequest request) {
        String formId = request.getParam("formId");
        responseFileService.listByForm(formId, responseFileIds -> {
            if (responseFileIds.isRight()) {
                JsonArray ids = responseFileIds.right().getValue();
                JsonArray fileIds = new JsonArray();

                if (ids != null && ids.size() > 0) {
                    for (Object fileId : ids) {
                        if (fileId instanceof JsonObject) {
                            fileIds.add(((JsonObject) fileId).getString("id"));
                        }
                    }

                    ResponseFileController.deleteFiles(storage, fileIds, deleteFilesEvt -> {
                        if (deleteFilesEvt.isRight()) {
                            responseFileService.deleteAll(fileIds, deleteResponseFilesEvt -> {
                                if (deleteResponseFilesEvt.isRight()) {
                                    formService.delete(formId, defaultResponseHandler(request));
                                    // TODO sync folder count children (should decrement because of the deletion)
                                }
                                else {
                                    log.error("[Formulaire@delete] Fail to delete response files in bdd");
                                    renderError(request, new JsonObject().put("message", deleteResponseFilesEvt.left().getValue()));
                                }
                            });
                        }
                        else {
                            log.error("[Formulaire@delete] Fail to delete files in storage");
                            renderError(request, new JsonObject().put("message", deleteFilesEvt.left().getValue()));
                        }
                    });
                }
                else {
                    formService.delete(formId, defaultResponseHandler(request));
                }
            }
            else {
                log.error("[Formulaire@delete] Failed to retrieve files' ids for form : " + formId);
                badRequest(request, responseFileIds.left().getValue());
            }
        });
    }

    @Put("/forms/move/:folderId")
    @ApiDoc("Update a specific form")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void move(HttpServerRequest request) {
        Integer targetFolderId = Integer.parseInt(request.getParam("folderId"));
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJsonArray(request, forms -> {
                    Integer oldFolderId = forms.getJsonObject(0).getInteger("folder_id");
                    JsonArray formIds = UtilsHelper.getIds(forms);
                    relFormFolderService.update(user, formIds, targetFolderId.toString(), updateEvent -> {
                        if (updateEvent.isLeft()) {
                            log.error("[Formulaire@moveForm] Error in moving forms " + forms);
                            RenderHelper.badRequest(request, updateEvent);
                            return;
                        }

                        if (targetFolderId != Formulaire.ID_ROOT_FOLDER) { // We do not sync root folder counts (useless)
                            folderService.syncNbChildren(user, targetFolderId.toString(), syncEvent -> {
                                if (syncEvent.isLeft()) {
                                    log.error("[Formulaire@moveForm] Error in sync children counts for folder " + targetFolderId);
                                    RenderHelper.badRequest(request, syncEvent);
                                    return;
                                }
                                if (oldFolderId != Formulaire.ID_ROOT_FOLDER) { // We do not sync root folder counts (useless)
                                    folderService.syncNbChildren(user, oldFolderId.toString(), defaultResponseHandler(request));
                                }
                                else {
                                    renderJson(request, syncEvent.right().getValue());
                                }
                            });
                        }
                        else if (oldFolderId != Formulaire.ID_ROOT_FOLDER) { // We do not sync root folder counts (useless)
                            folderService.syncNbChildren(user, oldFolderId.toString(), defaultResponseHandler(request));
                        }
                        else {
                            renderJson(request, updateEvent.right().getValue());
                        }
                    });
                });
            }
            else {
                log.error("User not found in session.");
                unauthorized(request);
            }
        });
    }

    @Post("/forms/:formId/remind")
    @ApiDoc("Send a reminder by mail to all the necessary responders")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void sendReminder(HttpServerRequest request) {
        String formId = request.getParam("formId");
        RequestUtils.bodyToJson(request, mail -> {
            UserUtils.getUserInfos(eb, request, user -> {
                if (user != null) {
                    formService.get(formId, user, getFormEvent -> {
                        if (getFormEvent.isRight()) {
                            JsonObject form = getFormEvent.right().getValue();

                            distributionService.listByForm(formId, getDistributions -> {
                                if (getDistributions.isRight()) {
                                    JsonArray distributions = getDistributions.right().getValue();
                                    JsonArray localRespondersIds = new JsonArray();
                                    JsonArray listMails = new JsonArray();

                                    // Generate list of mails to send
                                    for (int i = 0; i < distributions.size(); i++) {
                                        String id = distributions.getJsonObject(i).getString("responder_id");
                                        if (!localRespondersIds.contains(id)) {
                                            if (form.getBoolean("multiple")) {
                                                localRespondersIds.add(id);
                                            }
                                            else if (distributions.getJsonObject(i).getString("date_response") == null) {
                                                localRespondersIds.add(id);
                                            }
                                        }

                                        // Generate new mail object if limit or end loop are reached
                                        if (i == distributions.size() - 1 || localRespondersIds.size() == config.getInteger("zimbra-max-recipients", 50)) {
                                            JsonObject message = new JsonObject()
                                                    .put("subject", mail.getString("subject"))
                                                    .put("body", mail.getString("body"))
                                                    .put("to", new JsonArray())
                                                    .put("cci", localRespondersIds);

                                            JsonObject action = new JsonObject()
                                                    .put("action", "send")
                                                    .put("userId", user.getUserId())
                                                    .put("username", user.getUsername())
                                                    .put("message", message);

                                            listMails.add(action);
                                            localRespondersIds = new JsonArray();
                                        }
                                    }


                                    // Prepare futures to get message responses
                                    List<Future> mails = new ArrayList<>();
                                    mails.addAll(Collections.nCopies(listMails.size(), Promise.promise().future()));

                                    // Code to send mails
                                    for (int i = 0; i < listMails.size(); i++) {
                                        Future future = mails.get(i);

                                        // Send mail via Conversation app if it exists or else with Zimbra
                                        eb.request("org.entcore.conversation", listMails.getJsonObject(i), (Handler<AsyncResult<Message<JsonObject>>>) messageEvent -> {
                                            if (!"ok".equals(messageEvent.result().body().getString("status"))) {
                                                log.error("[Formulaire@sendReminder] Failed to send reminder : " + messageEvent.cause());
                                                future.handle(Future.failedFuture(messageEvent.cause()));
                                            }
                                            future.handle(Future.succeededFuture(messageEvent.result().body()));
                                        });
                                    }


                                    // Try to send effectively mails with code below and get results
                                    CompositeFuture.all(mails).onComplete(evt -> {
                                        if (evt.failed()) {
                                            log.error("[Zimbra@sendMessage] Failed to send reminder : " + evt.cause());
                                            Future.failedFuture(evt.cause());
                                        }

                                        // Update 'reminded' prop of the form
                                        form.put("reminded", true);
                                        formService.update(formId, form, updateEvent -> {
                                            if (updateEvent.isRight()) {
                                                renderJson(request, updateEvent.right().getValue());
                                            }
                                            else {
                                                log.error("[Formulaire@sendReminder] Fail to update form " + formId + " : " + updateEvent.left().getValue());
                                                renderError(request, new JsonObject().put("message", updateEvent.left().getValue()));
                                            }
                                        });
                                    });
                                }
                                else {
                                    String error = getDistributions.left().getValue();
                                    log.error("[Formulaire@sendReminder] Fail to retrieve distributions of form " + formId + " : " + error);
                                    renderError(request, new JsonObject().put("message", error));
                                }
                            });
                        }
                        else {
                            log.error("[Formulaire@sendReminder] Fail to get form " + formId + " : " + getFormEvent.left().getValue());
                            renderError(request, new JsonObject().put("message", getFormEvent.left().getValue()));
                        }
                    });
                } else {
                    log.error("User not found in session.");
                    Renders.unauthorized(request);
                }
            });
        });
    }

    @Get("/forms/:formId/rights")
    @ApiDoc("Get my rights for a specific form")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getMyFormRights(HttpServerRequest request) {
        String formId = request.getParam("formId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                List<String> groupsAndUserIds = new ArrayList();
                groupsAndUserIds.add(user.getUserId());
                if (user.getGroupsIds() != null) {
                    groupsAndUserIds.addAll(user.getGroupsIds());
                }
                formService.getMyFormRights(formId, groupsAndUserIds, arrayResponseHandler(request));
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Get("/forms/rights/all")
    @ApiDoc("Get my rights for all the forms")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getAllMyFormRights(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                List<String> groupsAndUserIds = new ArrayList();
                groupsAndUserIds.add(user.getUserId());
                if (user.getGroupsIds() != null) {
                    groupsAndUserIds.addAll(user.getGroupsIds());
                }
                formService.getAllMyFormRights(groupsAndUserIds, arrayResponseHandler(request));
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    // Exports

    @Post("/export/:fileType/:formId")
    @ApiDoc("Export a specific form's responses into a file (CSV or PDF)")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void export(final HttpServerRequest request) {
        String fileType = request.getParam("fileType");
        String formId = request.getParam("formId");
        RequestUtils.bodyToJson(request, images -> {
            UserUtils.getUserInfos(eb, request, user -> {
                if (user == null) {
                    log.error("User not found in session.");
                    Renders.unauthorized(request);
                    return;
                }
                formService.get(formId, user, getEvent -> {
                    if (getEvent.isRight()) {
                        switch (fileType) {
                            case "csv":
                                new FormResponsesExportCSV(request, getEvent.right().getValue()).launch();
                                break;
                            case "pdf":
                                JsonObject form = getEvent.right().getValue();
                                form.put("images", images);
                                new FormResponsesExportPDF(request, vertx, config, storage, form).launch();
                                break;
                            default:
                                badRequest(request);
                                break;
                        }
                    }
                    else {
                        log.error("[Formulaire@export] Error in getting form to export responses of form " + formId);
                    }
                });
            });
        });
    }

    // Image

    @Get("/info/image/:idImage")
    @ApiDoc("Get image info from workspace for a specific image")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getInfoImg(final HttpServerRequest request) {
        String idImage = request.getParam("idImage");
        formService.getImage(eb, idImage, DefaultResponseHandler.defaultResponseHandler(request));
    }

    // Share/Sending functions

    @Override
    @Get("/share/json/:id")
    @ApiDoc("List rights for a given form")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void shareJson(final HttpServerRequest request) {
        super.shareJson(request, false);
    }

    @Put("/share/json/:id")
    @ApiDoc("Add rights for a given form")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void shareSubmit(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                request.pause();
                final String formId = request.params().get("id");
                formService.get(formId, user, getFormHandler -> {
                    request.resume();
                    final String formName = getFormHandler.right().getValue().getString("title");
                    JsonObject params = new fr.wseduc.webutils.collections.JsonObject();
                    FormController.super.shareJsonSubmit(request, null, false, params, null);
                });
            }
            else {
                log.error("User not found in session.");
                unauthorized(request);
            }
        });
    }

    @Put("/share/resource/:id")
    @ApiDoc("Add rights for a given form")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void shareResource(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "share", shareFormObject -> {
            UserUtils.getUserInfos(eb, request, user -> {
                if (user != null) {
                    // Get all ids, filter the one about sending (response right)
                    final String formId = request.params().get("id");
                    Map<String, Object> idUsers = shareFormObject.getJsonObject("users").getMap();
                    Map<String, Object> idGroups = shareFormObject.getJsonObject("groups").getMap();
                    Map<String, Object> idBookmarks = shareFormObject.getJsonObject("bookmarks").getMap();

                    JsonArray usersIds = new JsonArray(new ArrayList<>(filterIdsForSending(idUsers).keySet()));
                    JsonArray groupsIds = new JsonArray(new ArrayList<>(filterIdsForSending(idGroups).keySet()));
                    JsonArray bookmarksIds = new JsonArray(new ArrayList<>(filterIdsForSending(idBookmarks).keySet()));

                    // Get group ids and users ids from bookmarks and add them to previous lists
                    neoService.getIdsFromBookMarks(bookmarksIds, eventBookmarks -> {
                        if (eventBookmarks.isRight()) {
                            JsonArray ids = eventBookmarks.right().getValue().getJsonObject(0).getJsonArray("ids").getJsonObject(0).getJsonArray("ids");
                            for (int i = 0; i < ids.size(); i++) {
                                JsonObject id = ids.getJsonObject(i);
                                boolean isGroup = id.getString("name") != null;
                                (isGroup ? groupsIds : usersIds).add(id.getString("id"));
                            }

                            // Get all users ids from usersIds & groupsIds
                            // Sync with distribution table
                            neoService.getUsersInfosFromIds(usersIds, groupsIds, eventUsers -> {
                                if (eventUsers.isRight()) {
                                    JsonArray infos = eventUsers.right().getValue();
                                    JsonArray responders = new JsonArray();
                                    for (int i = 0; i < infos.size(); i++) {
                                        JsonArray users = infos.getJsonObject(i).getJsonArray("users");
                                        responders.addAll(users);
                                    }

                                    if (responders.size() > Formulaire.MAX_USERS_SHARING) {
                                        String message = "You can't share to more than " + Formulaire.MAX_USERS_SHARING + " people.";
                                        log.error("[Formulaire@shareResource] " + message);
                                        renderError(request, new JsonObject().put("error", message));
                                        return;
                                    }

                                    syncDistributions(request, formId, user, responders, syncEvent -> {
                                        if (syncEvent.isRight()) {
                                            // Update 'collab' property as needed
                                            List<Map<String, Object>> idsObjects = new ArrayList<>();
                                            idsObjects.add(idUsers);
                                            idsObjects.add(idGroups);
                                            idsObjects.add(idBookmarks);
                                            updateFormCollabProp(formId, user, idsObjects, updateEvent -> {
                                                if (updateEvent.isRight()) {
                                                    fixBugAutoUnsharing(request, formId, user, shareFormObject);
                                                }
                                                else {
                                                    log.error("[Formulaire@shareResource] Fail to update collab prop for form " + formId);
                                                    renderError(request, new JsonObject().put("error", updateEvent.left().getValue()));
                                                }
                                            });
                                        }
                                        else {
                                            log.error("[Formulaire@shareResource] Fail to sync distributions for form " + formId);
                                            renderError(request, new JsonObject().put("error", syncEvent.left().getValue()));
                                        }
                                    });
                                }
                                else {
                                    log.error("[Formulaire@shareResource] Fail to get users' ids from groups' ids");
                                    renderError(request, new JsonObject().put("error", eventUsers.left().getValue()));
                                }
                            });
                        }
                        else {
                            log.error("[Formulaire@shareResource] Fail to get ids from bookmarks' ids");
                            renderError(request, new JsonObject().put("error", eventBookmarks.left().getValue()));
                        }
                    });
                }
                else {
                    log.error("User not found in session.");
                    unauthorized(request);
                }
            });
        });
    }

    private Map<String, Object> filterIdsForSending(Map<String, Object> map) {
        Map<String, Object> filteredMap = new HashMap<>();
        for (String key : map.keySet()) {
            ArrayList<String> values = (ArrayList<String>)map.get(key);
            for (String value : values) {
                if (value.equals(Formulaire.RESPONDER_RESOURCE_BEHAVIOUR)) {
                    filteredMap.put(key, map.get(key));
                }
            }
        }
        return filteredMap;
    }

    private void syncDistributions(HttpServerRequest request, String formId, UserInfos user, JsonArray responders, Handler<Either<String, JsonObject>> handler) {
        List<String> respondersFromSharing = UtilsHelper.getUserIds(responders).getList();

        distributionService.getResponders(formId, getRespondersEvt -> {
            if (getRespondersEvt.isLeft()) {
                log.error("[Formulaire@removeDeletedDistributions] Fail to get responders to form " + formId);
                handler.handle(new Either.Left<>(getRespondersEvt.left().getValue()));
                return;
            }

            List<String> respondersFromBDD = UtilsHelper.getUserIds(getRespondersEvt.right().getValue()).getList();

            // List the responders already in BDD
            List<String> existingResponders = new ArrayList<>(respondersFromSharing);
            existingResponders.retainAll(respondersFromBDD);

            // List the responder_ids to deactivate
            List<String> deactivatedResponders = new ArrayList<>(respondersFromBDD);
            deactivatedResponders.removeAll(respondersFromSharing);

            // List the new responders to add in BDD
            List<JsonObject> newResponders = new ArrayList<>();
            for (int i = 0; i < responders.size(); i++) {
                JsonObject responder = responders.getJsonObject(i);
                if (!respondersFromBDD.contains(responder.getString("id"))) {
                    newResponders.add(responder);
                }
            }


            distributionService.setActiveValue(false, formId, deactivatedResponders, deactivateEvent -> {
                if (deactivateEvent.isLeft()) {
                    log.error("[Formulaire@removeDeletedDistributions] Fail to deactivate distributions");
                    handler.handle(new Either.Left<>(deactivateEvent.left().getValue()));
                    return;
                }

                addNewDistributions(request, formId, user, newResponders, existingResponders, addEvt -> {
                    if (addEvt.isLeft()) {
                        log.error(addEvt.left().getValue());
                        handler.handle(new Either.Left<>(addEvt.left().getValue()));
                        return;
                    }

                    updateFormSentProp(formId, user, updateSentPropEvent -> {
                        if (updateSentPropEvent.isLeft()) {
                            log.error(updateSentPropEvent.left().getValue());
                            handler.handle(new Either.Left<>(updateSentPropEvent.left().getValue()));
                            return;
                        }

                        handler.handle(new Either.Right<>(updateSentPropEvent.right().getValue()));
                    });
                });
            });
        });
    }

    private void addNewDistributions(HttpServerRequest request, String formId, UserInfos user, List<JsonObject> newResponders,
                                     List<String> existingResponders, Handler<Either<String, JsonObject>> handler) {
        distributionService.createMultiple(formId, user, newResponders, addEvent -> {
            if (addEvent.isRight()) {
                JsonArray respondersIds = UtilsHelper.getUserIds(new JsonArray(newResponders));
                if (!existingResponders.isEmpty()) {
                    distributionService.setActiveValue(true, formId, existingResponders, updateEvent -> {
                        if (updateEvent.isRight()) {
                            formService.get(formId, user, getFormEvent -> {
                                if (getFormEvent.isLeft()) {
                                    handler.handle(new Either.Right<>(getFormEvent.right().getValue()));
                                    log.error("[Formulaire@addNewDistributions] Fail to get infos for form with id " + formId);
                                    return;
                                }
                                JsonObject form = getFormEvent.right().getValue();
                                notifyService.notifyNewForm(request, form, respondersIds);
                                handler.handle(new Either.Right<>(getFormEvent.right().getValue()));
                            });
                        } else {
                            log.error("[Formulaire@addNewDistributions] Fail to update distributions");
                            handler.handle(new Either.Left<>(updateEvent.left().getValue()));
                        }
                    });
                }
                else {
                    formService.get(formId, user, getFormEvent -> {
                        if (getFormEvent.isLeft()) {
                            handler.handle(new Either.Right<>(getFormEvent.right().getValue()));
                            log.error("[Formulaire@addNewDistributions] Fail to get infos for form with id " + formId);
                            return;
                        }
                        JsonObject form = getFormEvent.right().getValue();
                        notifyService.notifyNewForm(request, form, respondersIds);
                        handler.handle(new Either.Right<>(getFormEvent.right().getValue()));
                    });
                }
            }
            else {
                log.error("[Formulaire@addNewDistributions] Fail to add distributions");
                handler.handle(new Either.Left<>(addEvent.left().getValue()));
            }
        });
    }

    private void updateFormSentProp(String formId, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        distributionService.listByForm(formId, getDistributionsEvent -> {
            if (getDistributionsEvent.isRight()) {
                boolean value = !getDistributionsEvent.right().getValue().isEmpty();
                formService.get(formId, user, getEvent -> {
                    if (getEvent.isRight()) {
                        JsonObject form = getEvent.right().getValue();
                        form.put("sent", value);
                        formService.update(formId, form, updateEvent -> {
                            if (updateEvent.isRight()) {
                                handler.handle(new Either.Right<>(updateEvent.right().getValue()));
                            } else {
                                log.error("[Formulaire@updateFormSentProp] Fail to update form");
                                handler.handle(new Either.Left<>(updateEvent.left().getValue()));
                            }
                        });
                    } else {
                        log.error("[Formulaire@updateFormSentProp] Fail to get form");
                        handler.handle(new Either.Left<>(getEvent.left().getValue()));
                    }
                });
            } else {
                log.error("[Formulaire@updateFormSentProp] Fail to get distributions of the form");
                handler.handle(new Either.Left<>(getDistributionsEvent.left().getValue()));
            }
        });
    }

    private void updateFormCollabProp(String formId, UserInfos user, List<Map<String, Object>> idsObjects, Handler<Either<String, JsonObject>> handler) {
        formService.get(formId, user, getEvent -> {
            if (getEvent.isRight()) {
                JsonObject form = getEvent.right().getValue();

                boolean isShared = false;
                int i = 0;
                while (!isShared && i < idsObjects.size()) { // Iterate over "users", "groups", "bookmarks"
                    int j = 0;
                    Map<String, Object> o = idsObjects.get(i);
                    List<Object> values = new ArrayList<Object>(o.values());

                    while (!isShared && j < values.size()) { // Iterate over each pair id-actions
                        List<String> actions = (ArrayList)(values.get(j));

                        int k = 0;
                        while (!isShared && k < actions.size()) { // Iterate over each action for an id
                            if (actions.get(k).equals(Formulaire.CONTRIB_RESOURCE_BEHAVIOUR) ||
                                actions.get(k).equals(Formulaire.MANAGER_RESOURCE_BEHAVIOUR)) {
                                    isShared = true;
                            }
                            k++;
                        }
                        j++;
                    }
                    i++;
                }

                if (!isShared && !form.getString("owner_id").equals(user.getUserId())) {
                    isShared = true;
                }

                form.put("collab", isShared);
                formService.update(formId, form, updateEvent -> {
                    if (updateEvent.isLeft()) {
                        handler.handle(new Either.Left<>(updateEvent.left().getValue()));
                        log.error("[Formulaire@updateFormCollabProp] Fail to update form : " + updateEvent.left().getValue());
                    }
                    else {
                        handler.handle(new Either.Right<>(updateEvent.right().getValue()));
                    }
                });
            }
            else {
                handler.handle(new Either.Left<>(getEvent.left().getValue()));
                log.error("[Formulaire@updateFormCollabProp] Fail to get form : " + getEvent.left().getValue());
            }
        });
    }

    private void fixBugAutoUnsharing(HttpServerRequest request, String formId, UserInfos user, JsonObject shareFormObject) {
        formShareService.getSharedWithMe(formId, user, event -> {
            if (event.isRight() && event.right().getValue() != null) {
                JsonArray rights = event.right().getValue();
                String id = user.getUserId();
                shareFormObject.getJsonObject("users").put(id, new JsonArray());

                for (int i = 0; i < rights.size(); i++) {
                    JsonObject right = rights.getJsonObject(i);
                    shareFormObject.getJsonObject("users").getJsonArray(id).add(right.getString("action"));
                }

                // Classic sharing stuff (putting or removing ids from form_shares table accordingly)
                this.getShareService().share(user.getUserId(), formId, shareFormObject, (r) -> {
                    if (r.isRight()) {
                        this.doShareSucceed(request, formId, user, shareFormObject, r.right().getValue(), false);
                    } else {
                        JsonObject error = (new JsonObject()).put("error", r.left().getValue());
                        Renders.renderJson(request, error, 400);
                    }
                });
            }
            else {
                log.error("[Formulaire@getSharedWithMe] Fail to get user's shared rights");
            }
        });
    }
}