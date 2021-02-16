package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.export.FormResponsesExport;
import fr.openent.formulaire.security.CreationRight;
import fr.openent.formulaire.service.DistributionService;
import fr.openent.formulaire.service.FormService;
import fr.openent.formulaire.service.NeoService;
import fr.openent.formulaire.service.impl.DefaultDistributionService;
import fr.openent.formulaire.service.impl.DefaultFormService;
import fr.openent.formulaire.service.impl.DefaultNeoService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.http.response.DefaultResponseHandler;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;


import java.util.*;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class FormController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(FormController.class);
    private final Storage storage;
    private FormService formService;
    private DistributionService distributionService;
    private NeoService neoService;

    public FormController(final Storage storage) {
        super();
        this.storage = storage;
        this.formService = new DefaultFormService();
        this.distributionService = new DefaultDistributionService();
        this.neoService = new DefaultNeoService();
    }


//    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
//    public void initContribResourceRight(final HttpServerRequest request) { }

//    @SecuredAction(value = Formulaire.MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
//    public void initManagerResourceRight(final HttpServerRequest request) { }

    @SecuredAction(value = Formulaire.RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void initResponderResourceRight(final HttpServerRequest request) { }


    @Get("/forms")
    @ApiDoc("List all the forms created by me")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
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
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
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

    @Get("/forms/:formId")
    @ApiDoc("Get form thanks to the id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void get(HttpServerRequest request) {
        String formId = request.getParam("formId");
        formService.get(formId, defaultResponseHandler(request));
    }

    @Put("/forms/:formId")
    @ApiDoc("Update given form")
    @SecuredAction(Formulaire.CREATION_RIGHT)
    public void update(HttpServerRequest request) {
        String formId = request.getParam("formId");
        RequestUtils.bodyToJson(request, form -> {
            formService.update(formId, form, defaultResponseHandler(request));
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
                    formService.create(form, user, defaultResponseHandler(request));
                });
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Delete("/forms/:formId")
    @ApiDoc("Delete given form")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void delete(HttpServerRequest request) {
        String formId = request.getParam("formId");
        formService.delete(formId, defaultResponseHandler(request));
    }

    // Export

    @Get("/export/:formId")
    @ApiDoc("Export given form")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void export(HttpServerRequest request) {
        new FormResponsesExport(eb, request).launch();
    }

    // Image

    @Get("/info/image/:idImage")
    @ApiDoc("get info image workspace")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getInfoImg(final HttpServerRequest request) {
        String idImage = request.getParam("idImage");
        formService.getImage(eb, idImage, DefaultResponseHandler.defaultResponseHandler(request));
    }

    // Share/Sending functions

    @Get("/share/json/:id")
    @ApiDoc("Lists rights for a given form.")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void shareJson(final HttpServerRequest request) {
        super.shareJson(request, false);
    }

    @Put("/share/json/:id")
    @ApiDoc("Adds rights for a given form.")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void shareSubmit(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                request.pause();
                final String formId = request.params().get("id");
                formService.get(formId, getFormHandler -> {
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
    @ApiDoc("Adds rights for a given form.")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void shareResource(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "share", shareFormObject -> {
            UserUtils.getUserInfos(eb, request, user -> {
                if (user != null) {
                    // Get all ids, filter the one about sending (response right) and sync distribution table accordingly
                    final String formId = request.params().get("id");
                    Map<String, Object> idUsers = shareFormObject.getJsonObject("users").getMap();
                    Map<String, Object> idGroups = shareFormObject.getJsonObject("groups").getMap();
                    Map<String, Object> idBookmarks = shareFormObject.getJsonObject("bookmarks").getMap();

                    JsonArray usersIds = new JsonArray(new ArrayList<>(filterIdsForSending(idUsers).keySet()));
                    JsonArray groupsIds = new JsonArray(new ArrayList<>(filterIdsForSending(idGroups).keySet()));
                    JsonArray bookmarksIds = new JsonArray(new ArrayList<>(filterIdsForSending(idBookmarks).keySet()));

                    neoService.getUsersInfosFromIds(usersIds, groupsIds, eventUsers -> {
                        if (eventUsers.isRight()) {
                            JsonArray infos = eventUsers.right().getValue();
                            syncDistributions(formId, user, infos);
                        } else {
                            log.error("[Formulaire@getUserIds] Fail to get users' ids from groups' ids");
                        }
                    });

                    // Classic sharing stuff (putting or removing ids from form_shares table)
                    List<Map<String, Object>> idsObjects = new ArrayList<>();
                    idsObjects.add(idUsers);
                    idsObjects.add(idGroups);
                    idsObjects.add(idBookmarks);
                    updateFormCollabProp(formId, idsObjects);

                    super.shareResource(request, null, false, null, null);
                } else {
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

    private void syncDistributions(String formId, UserInfos user, JsonArray responders) {
        removeDeletedDistributions(formId, responders, removeEvt -> {
            if (removeEvt.failed()) log.error(removeEvt.cause());

            addNewDistributions(formId, user, responders, addEvt -> {
                if (addEvt.failed()) log.error(addEvt.cause());

                updateFormSentProp(formId, updateSentPropEvent -> {
                    if (updateSentPropEvent.failed()) log.error(updateSentPropEvent.cause());
                });
            });
        });
    }

    private void removeDeletedDistributions(String formId, JsonArray responders, Handler<AsyncResult<String>> handler) {
        distributionService.getRemoved(formId, responders, filteringEvent -> {
            if (filteringEvent.isRight()) {
                JsonArray removed = filteringEvent.right().getValue();
                if (!removed.isEmpty()) {
                    distributionService.removeMultiple(formId, removed, removeEvent -> {
                        if (removeEvent.isRight()) {
                            handler.handle(Future.succeededFuture());
                            log.info("[Formulaire@removeDeletedDistributions] Successful remove in distribution table");
                        } else {
                            handler.handle(Future.failedFuture(removeEvent.left().getValue()));
                            log.error("[Formulaire@removeDeletedDistributions] Fail to remove distributions");
                        }
                    });
                }
                handler.handle(Future.succeededFuture());
            } else {
                handler.handle(Future.failedFuture(filteringEvent.left().getValue()));
                log.error("[Formulaire@removeDeletedDistributions] Fail to filter distributions to remove");
            }
        });
    }

    private void addNewDistributions(String formId, UserInfos user, JsonArray responders, Handler<AsyncResult<String>> handler) {
        if (!responders.isEmpty()) {
            distributionService.getDuplicates(formId, responders, filteringEvent -> {
                if (filteringEvent.isRight()) {
                    JsonArray duplicates = filteringEvent.right().getValue();
                    distributionService.createMultiple(formId, user, responders, duplicates, addEvent -> {
                        if (addEvent.isRight()) {
                            handler.handle(Future.succeededFuture());
                            log.info("[Formulaire@addNewDistributions] Successful adding in distribution table");
                        } else {
                            handler.handle(Future.failedFuture(addEvent.left().getValue()));
                            log.error("[Formulaire@addNewDistributions] Fail to add distributions");
                        }
                    });
                } else {
                    handler.handle(Future.failedFuture(filteringEvent.left().getValue()));
                    log.error("[Formulaire@addNewDistributions] Fail to filter existing distributions");
                }
            });
        }
        handler.handle(Future.succeededFuture());
    }

    private void updateFormSentProp(String formId, Handler<AsyncResult<String>> handler) {
        distributionService.listByForm(formId, getDistributionsEvent -> {
            if (getDistributionsEvent.isRight()) {
                boolean value = !getDistributionsEvent.right().getValue().isEmpty();
                formService.get(formId, getEvent -> {
                    if (getEvent.isRight()) {
                        JsonObject form = getEvent.right().getValue();
                        form.put("sent", value);
                        formService.update(formId, form, updateEvent -> {
                            if (updateEvent.isRight()) {
                                handler.handle(Future.succeededFuture());
                                log.info("[Formulaire@updateFormSentProp] Form's sent property has been updated to : " + value);
                            } else {
                                handler.handle(Future.failedFuture(updateEvent.left().getValue()));
                                log.error("[Formulaire@updateFormSentProp] Fail to update form");
                            }
                        });
                    } else {
                        handler.handle(Future.failedFuture(getEvent.left().getValue()));
                        log.error("[Formulaire@updateFormSentProp] Fail to get form");
                    }
                });
            } else {
                handler.handle(Future.failedFuture(getDistributionsEvent.left().getValue()));
                log.error("[Formulaire@updateFormSentProp] Fail to get distributions of the form");
            }
        });
    }

    private void updateFormCollabProp(String formId, List<Map<String, Object>> idsObjects) {
        boolean isShared = false;
        int i = 0;
        while (!isShared && i < idsObjects.size()) { // Iterate over "users", "groups", "bookmarks"
            int j = 0;
            Map<String, Object> o = idsObjects.get(i);
            Object[] arrayKeys = o.keySet().toArray();
            while (!isShared && j < arrayKeys.length) { // Iterate over each pair id-actions
                ArrayList<String> values = (ArrayList<String>)o.get(arrayKeys[j]);
                int k = 0;
                while (!isShared && k < values.size()) { // Iterate over each action for an id
                    if (!values.get(k).equals(Formulaire.RESPONDER_RESOURCE_BEHAVIOUR)) {
                        isShared = true;
                    }
                    k++;
                }
                j++;
            }
            i++;
        }

        final boolean value = isShared;
        formService.get(formId, getEvent -> {
            if (getEvent.isRight()) {
                JsonObject form = getEvent.right().getValue();
                form.put("collab", value);
                formService.update(formId, form, updateEvent -> {
                    if (updateEvent.isRight()) {
                        log.info("[Formulaire@updateFormCollabProp] Form's collab property has been updated to : " + value);
                    } else {
                        log.error("[Formulaire@updateFormCollabProp] Fail to update form : " + updateEvent.left().getValue());
                    }
                });
            } else {
                log.error("[Formulaire@updateFormCollabProp] Fail to get form : " + getEvent.left().getValue());
            }
        });
    }
}