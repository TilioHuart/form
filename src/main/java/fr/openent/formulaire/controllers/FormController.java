package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.export.FormResponsesExport;
import fr.openent.formulaire.security.CreationRight;
import fr.openent.formulaire.security.ShareAndOwner;
import fr.openent.formulaire.service.DistributionService;
import fr.openent.formulaire.service.FormService;
import fr.openent.formulaire.service.FormSharesService;
import fr.openent.formulaire.service.NeoService;
import fr.openent.formulaire.service.impl.DefaultDistributionService;
import fr.openent.formulaire.service.impl.DefaultFormService;
import fr.openent.formulaire.service.impl.DefaultFormSharesService;
import fr.openent.formulaire.service.impl.DefaultNeoService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.http.response.DefaultResponseHandler;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class FormController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(FormController.class);
    private final Storage storage;
    private FormService formService;
    private DistributionService distributionService;
    private FormSharesService formShareService;
    private NeoService neoService;

    public FormController(final Storage storage) {
        super();
        this.storage = storage;
        this.formService = new DefaultFormService();
        this.distributionService = new DefaultDistributionService();
        this.formShareService = new DefaultFormSharesService();
        this.neoService = new DefaultNeoService();
    }

    // Init rights

    @SecuredAction(Formulaire.CREATION_RIGHT)
    public void initCreationRight(final HttpServerRequest request) {
    }

    @SecuredAction(Formulaire.RESPONSE_RIGHT)
    public void initResponseRight(final HttpServerRequest request) {
    }

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
    @ApiDoc("List all the forms created or shared by me")
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
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
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
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void delete(HttpServerRequest request) {
        String formId = request.getParam("formId");
        formService.delete(formId, defaultResponseHandler(request));
    }

    @Get("/forms/:formId/rights")
    @ApiDoc("Export given form")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getMyFormRights(HttpServerRequest request) {
        String formId = request.getParam("formId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                formService.getMyFormRights(formId, user, arrayResponseHandler(request));
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
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
    @ApiDoc("Get info image workspace")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getInfoImg(final HttpServerRequest request) {
        String idImage = request.getParam("idImage");
        formService.getImage(eb, idImage, DefaultResponseHandler.defaultResponseHandler(request));
    }

    // Share/Sending functions

    @Override
    @Get("/share/json/:id")
    @ApiDoc("Lists rights for a given form.")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void shareJson(final HttpServerRequest request) {
        super.shareJson(request, false);
    }

    @Put("/share/json/:id")
    @ApiDoc("Adds rights for a given form.")
    @ResourceFilter(ShareAndOwner.class)
    @SecuredAction(value = Formulaire.MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
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

                            // Get all users ids from usersIds & groupsIds and sync with distribution table
                            neoService.getUsersInfosFromIds(usersIds, groupsIds, eventUsers -> {
                                if (eventUsers.isRight()) {
                                    JsonArray infos = eventUsers.right().getValue();
                                    syncDistributions(formId, user, infos);
                                } else {
                                    log.error("[Formulaire@getUserIds] Fail to get users' ids from groups' ids");
                                }
                            });
                        } else {
                            log.error("[Formulaire@getUserIds] Fail to get ids from bookmarks' ids");
                        }
                    });

                    // Update 'collab' property as needed
                    List<Map<String, Object>> idsObjects = new ArrayList<>();
                    idsObjects.add(idUsers);
                    idsObjects.add(idGroups);
                    idsObjects.add(idBookmarks);
                    updateFormCollabProp(formId, user, idsObjects);

                    // Fix bug auto-unsharing
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
                                    this.doShareSucceed(request, formId, user, shareFormObject, (JsonObject)r.right().getValue(), false);
                                } else {
                                    JsonObject error = (new JsonObject()).put("error", (String)r.left().getValue());
                                    Renders.renderJson(request, error, 400);
                                }
                            });
                        }
                        else {
                            log.error("[Formulaire@getSharedWithMe] Fail to get user's shared rights");
                        }
                    });
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

    private void updateFormCollabProp(String formId, UserInfos user, List<Map<String, Object>> idsObjects) {
        formService.get(formId, getEvent -> {
            if (getEvent.isRight()) {
                JsonObject form = getEvent.right().getValue();

                boolean isShared = false;
                int i = 0;
                while (!isShared && i < idsObjects.size()) { // Iterate over "users", "groups", "bookmarks"
                    int j = 0;
                    Map<String, Object> o = idsObjects.get(i);
                    List<List<String>> values = new ArrayList(o.values());

                    while (!isShared && j < values.size()) { // Iterate over each pair id-actions
                        List<String> actions = new ArrayList<>(values.get(j));

                        int k = 0;
                        while (!isShared && k < values.size()) { // Iterate over each action for an id
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
                        log.error("[Formulaire@updateFormCollabProp] Fail to update form : " + updateEvent.left().getValue());
                    }
                });
            } else {
                log.error("[Formulaire@updateFormCollabProp] Fail to get form : " + getEvent.left().getValue());
            }
        });
    }
}