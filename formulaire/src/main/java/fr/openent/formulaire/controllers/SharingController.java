package fr.openent.formulaire.controllers;

import fr.openent.form.core.models.ShareObject;
import fr.openent.formulaire.security.CustomShareAndOwner;
import fr.openent.formulaire.service.*;
import fr.openent.formulaire.service.impl.*;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static fr.openent.form.core.constants.Constants.MAX_USERS_SHARING;
import static fr.openent.form.core.constants.EbFields.ACTION;
import static fr.openent.form.core.constants.EbFields.WORKSPACE_ADDRESS;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.ShareRights.*;
import static fr.openent.form.helpers.RenderHelper.renderInternalError;
import static fr.openent.form.helpers.UtilsHelper.getStringIds;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class SharingController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(SharingController.class);
    private final SimpleDateFormat formDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private final FormService formService;
    private final DistributionService distributionService;
    private final FormSharesService formShareService;
    private final NeoService neoService;
    private final NotifyService notifyService;

    public SharingController(TimelineHelper timelineHelper) {
        super();
        this.formService = new DefaultFormService();
        this.distributionService = new DefaultDistributionService();
        this.formShareService = new DefaultFormSharesService();
        this.neoService = new DefaultNeoService();
        this.notifyService = new DefaultNotifyService(timelineHelper, eb);
    }

    @Override
    @Get("/share/json/:id")
    @ApiDoc("List rights for a given form")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void shareJson(final HttpServerRequest request) {
        final String id = request.params().get(ID);
        if (id != null && !id.trim().isEmpty()) {
            UserUtils.getUserInfos(this.eb, request, user -> {
                if (user == null) {
                    log.error("[Formulaire@SharingController::shareJson] User not found in session.");
                    unauthorized(request);
                    return;
                }

                super.shareService.shareInfos(user.getUserId(), id, I18n.acceptLanguage(request), request.params().get(SEARCH), shareInfosEvt -> {
                    if (shareInfosEvt.isLeft()) {
                        log.error("[Formulaire@SharingController::shareJson] Fail to get sharing infos : " + shareInfosEvt.left().getValue());
                        renderInternalError(request, shareInfosEvt);
                        return;
                    }

                    JsonObject shareInfos = shareInfosEvt.right().getValue().copy();
                    formService.get(id, user, formEvt -> {
                        if (formEvt.isLeft()) {
                            log.error("[Formulaire@SharingController::shareJson] Fail to get form with id " + id + " : " + formEvt.left().getValue());
                            renderInternalError(request, formEvt);
                            return;
                        }

                        JsonObject form = formEvt.right().getValue();
                        if (form.getBoolean(IS_PUBLIC, false)) {
                            JsonArray actions = shareInfos.getJsonArray(ACTIONS);

                            for (int i = actions.size() - 1; i >= 0; i--) {
                                JsonObject action = actions.getJsonObject(i);
                                if (action.getString(PARAM_DISPLAY_NAME).equals(RESPONDER_RESOURCE_RIGHT)) {
                                    actions.remove(i);
                                }
                            }
                        }

                        renderJson(request, shareInfos);
                    });
                });
            });
        }
        else {
            log.error("[Formulaire@SharingController::shareJson] ID parameter must not be null or empty.");
            badRequest(request);
        }
    }

    @Put("/share/json/:id")
    @ApiDoc("Add rights for a given form")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void shareSubmit(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                log.error("[Formulaire@SharingController::shareSubmit] User not found in session.");
                unauthorized(request);
                return;
            }

            SharingController.super.shareJsonSubmit(request, null, false, new JsonObject(), null);
        });
    }

    @Put("/share/resource/:id")
    @ApiDoc("Add rights for a given form")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = MANAGER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void shareResource(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "share", shareObjectJson -> {
            if (shareObjectJson == null || shareObjectJson.isEmpty()) {
                log.error("[Formulaire@SharingController::shareResource] No forms to share.");
                noContent(request);
                return;
            }
            ShareObject shareObject = new ShareObject(shareObjectJson);
            shareObject.addCommonRights();

            UserUtils.getUserInfos(eb, request, user -> {
                if (user == null) {
                    log.error("[Formulaire@SharingController::shareResource] User not found in session.");
                    unauthorized(request);
                    return;
                }

                // Get all ids, filter the one about sending (response right)
                final String formId = request.params().get(ID);
                Map<String, Object> idUsers = shareObject.getUsers().getMap();
                Map<String, Object> idGroups = shareObject.getGroups().getMap();
                Map<String, Object> idBookmarks = shareObject.getBookmarks().getMap();

                JsonArray usersIds = new JsonArray(new ArrayList<>(filterIdsForSending(idUsers).keySet()));
                JsonArray groupsIds = new JsonArray(new ArrayList<>(filterIdsForSending(idGroups).keySet()));
                JsonArray bookmarksIds = new JsonArray(new ArrayList<>(filterIdsForSending(idBookmarks).keySet()));

                // Get group ids and users ids from bookmarks and add them to previous lists
                neoService.getIdsFromBookMarks(bookmarksIds, bookmarksEvt -> {
                    if (bookmarksEvt.isLeft()) {
                        log.error("[Formulaire@SharingController::shareResource] Fail to get ids from bookmarks' ids");
                        renderInternalError(request, bookmarksEvt);
                        return;
                    }

                    JsonArray ids = bookmarksEvt.right().getValue().getJsonObject(0).getJsonArray(IDS).getJsonObject(0).getJsonArray(IDS);
                    for (int i = 0; i < ids.size(); i++) {
                        JsonObject id = ids.getJsonObject(i);
                        boolean isGroup = id.getString(NAME) != null;
                        (isGroup ? groupsIds : usersIds).add(id.getString(ID));
                    }

                    // Get all users ids from usersIds & groupsIds
                    // Sync with distribution table
                    neoService.getUsersInfosFromIds(usersIds, groupsIds, usersEvt -> {
                        if (usersEvt.isLeft()) {
                            log.error("[Formulaire@SharingController::shareResource] Fail to get users' ids from groups' ids");
                            renderInternalError(request, usersEvt);
                            return;
                        }

                        JsonArray infos = usersEvt.right().getValue();
                        JsonArray responders = new JsonArray();
                        for (int i = 0; i < infos.size(); i++) {
                            JsonArray users = infos.getJsonObject(i).getJsonArray(USERS);
                            responders.addAll(users);
                        }

                        // Check max sharing limit
                        if (responders.size() > MAX_USERS_SHARING) {
                            log.error("[Formulaire@SharingController::shareResource] " +
                                    "Share to more than " + MAX_USERS_SHARING + " people is not allowed.");
                            badRequest(request);
                            return;
                        }

                        syncDistributions(request, formId, user, responders, syncEvt -> {
                            if (syncEvt.isLeft()) {
                                log.error("[Formulaire@SharingController::shareResource] " +
                                        "Fail to sync distributions for form " + formId);
                                renderInternalError(request, syncEvt);
                                return;
                            }

                            // Update 'collab' property as needed
                            List<Map<String, Object>> idsObjects = new ArrayList<>();
                            idsObjects.add(idUsers);
                            idsObjects.add(idGroups);
                            idsObjects.add(idBookmarks);
                            updateFormCollabProp(formId, user, idsObjects, updateEvt -> {
                                if (updateEvt.isLeft()) {
                                    log.error("[Formulaire@SharingController::shareResource] " +
                                            "Fail to update collab prop for form " + formId);
                                    renderInternalError(request, updateEvt);
                                    return;
                                }
                                fixBugAutoUnsharing(request, formId, user, shareObject);
                            });
                        });
                    });
                });
            });
        });
    }

    private Map<String, Object> filterIdsForSending(Map<String, Object> map) {
        Map<String, Object> filteredMap = new HashMap<>();
        for (String key : map.keySet()) {
            ArrayList<String> values = (ArrayList<String>)map.get(key);
            for (String value : values) {
                if (value.equals(RESPONDER_RESOURCE_BEHAVIOUR)) {
                    filteredMap.put(key, map.get(key));
                }
            }
        }
        return filteredMap;
    }

    private void syncDistributions(HttpServerRequest request, String formId, UserInfos user, JsonArray responders, Handler<Either<String, JsonObject>> handler) {
        List<String> respondersFromSharing = getStringIds(responders).getList();

        distributionService.getResponders(formId, respondersEvt -> {
            if (respondersEvt.isLeft()) {
                log.error("[Formulaire@SharingController::removeDeletedDistributions] Fail to get responders to form " + formId);
                handler.handle(new Either.Left<>(respondersEvt.left().getValue()));
                return;
            }

            List<String> respondersFromBDD = getStringIds(respondersEvt.right().getValue()).getList();

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
                if (!respondersFromBDD.contains(responder.getString(ID))) {
                    newResponders.add(responder);
                }
            }

            distributionService.setActiveValue(false, formId, deactivatedResponders, deactivateEvt -> {
                if (deactivateEvt.isLeft()) {
                    log.error("[Formulaire@SharingController::removeDeletedDistributions] Fail to deactivate distributions");
                    handler.handle(new Either.Left<>(deactivateEvt.left().getValue()));
                    return;
                }

                addNewDistributions(request, formId, user, newResponders, existingResponders, addEvt -> {
                    if (addEvt.isLeft()) {
                        log.error(addEvt.left().getValue());
                        handler.handle(new Either.Left<>(addEvt.left().getValue()));
                        return;
                    }

                    updateFormSentProp(formId, user, updateSentPropEvt -> {
                        if (updateSentPropEvt.isLeft()) {
                            log.error(updateSentPropEvt.left().getValue());
                            handler.handle(new Either.Left<>(updateSentPropEvt.left().getValue()));
                            return;
                        }

                        handler.handle(new Either.Right<>(updateSentPropEvt.right().getValue()));
                    });
                });
            });
        });
    }

    private void addNewDistributions(HttpServerRequest request, String formId, UserInfos user, List<JsonObject> newResponders,
                                     List<String> existingResponders, Handler<Either<String, JsonObject>> handler) {
        distributionService.createMultiple(formId, user, newResponders, addEvt -> {
            if (addEvt.isLeft()) {
                log.error("[Formulaire@SharingController::addNewDistributions] Fail to add distributions");
                handler.handle(new Either.Left<>(addEvt.left().getValue()));
                return;
            }

            JsonArray respondersIds = getStringIds(new JsonArray(newResponders));
            if (!existingResponders.isEmpty()) {
                distributionService.setActiveValue(true, formId, existingResponders, updateEvt -> {
                    if (updateEvt.isLeft()) {
                        log.error("[Formulaire@SharingController::addNewDistributions] Fail to update distributions");
                        handler.handle(new Either.Left<>(updateEvt.left().getValue()));
                        return;
                    }

                    sendNotification(request, formId, user, respondersIds, handler);
                });
            }
            else {
                sendNotification(request, formId, user, respondersIds, handler);
            }
        });
    }

    private void sendNotification(HttpServerRequest request, String formId, UserInfos user, JsonArray respondersIds, Handler<Either<String, JsonObject>> handler) {
        formService.get(formId, user, formEvt -> {
            if (formEvt.isLeft()) {
                log.error("[Formulaire@SharingController::addNewDistributions] Fail to get infos for form with id " + formId);
                handler.handle(new Either.Right<>(formEvt.right().getValue()));
                return;
            }
            if (formEvt.right().getValue().isEmpty()) {
                String message = "[Formulaire@SharingController::addNewDistributions] No form found for id " + formId;
                log.error(message);
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonObject form = formEvt.right().getValue();

            // Check openingDate to send notification or not
            try {
                Date openingDate = formDateFormatter.parse(form.getString(DATE_OPENING));
                Date now = new Date();
                if (openingDate.before(now)) {
                    notifyService.notifyNewForm(request, form, respondersIds);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            handler.handle(new Either.Right<>(formEvt.right().getValue()));
        });
    }

    private void updateFormSentProp(String formId, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        distributionService.listByForm(formId, distributionsEvt -> {
            if (distributionsEvt.isLeft()) {
                log.error("[Formulaire@SharingController::updateFormSentProp] Fail to get distributions of the form");
                handler.handle(new Either.Left<>(distributionsEvt.left().getValue()));
                return;
            }

            boolean hasDistributions = !distributionsEvt.right().getValue().isEmpty();
            formService.get(formId, user, formEvt -> {
                if (formEvt.isLeft()) {
                    log.error("[Formulaire@SharingController::updateFormSentProp] Fail to get form");
                    handler.handle(new Either.Left<>(formEvt.left().getValue()));
                    return;
                }
                if (formEvt.right().getValue().isEmpty()) {
                    String message = "[Formulaire@SharingController::updateFormSentProp] No form found for id " + formId;
                    log.error(message);
                    handler.handle(new Either.Left<>(message));
                    return;
                }

                JsonObject form = formEvt.right().getValue();
                form.put(SENT, hasDistributions);
                formService.update(formId, form, updateEvt -> {
                    if (updateEvt.isLeft()) {
                        log.error("[Formulaire@SharingController::updateFormSentProp] Fail to update form");
                        handler.handle(new Either.Left<>(updateEvt.left().getValue()));
                        return;
                    }

                    handler.handle(new Either.Right<>(updateEvt.right().getValue()));
                });
            });
        });
    }

    private void updateFormCollabProp(String formId, UserInfos user, List<Map<String, Object>> idsObjects, Handler<Either<String, JsonObject>> handler) {
        formService.get(formId, user, formEvt -> {
            if (formEvt.isLeft()) {
                log.error("[Formulaire@SharingController::updateFormCollabProp] Fail to get form : " + formEvt.left().getValue());
                handler.handle(new Either.Left<>(formEvt.left().getValue()));
                return;
            }
            if (formEvt.right().getValue().isEmpty()) {
                String message = "[Formulaire@SharingController::updateFormCollabProp] No form found for id " + formId;
                log.error(message);
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonObject form = formEvt.right().getValue();

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
                        if (actions.get(k).equals(CONTRIB_RESOURCE_BEHAVIOUR) ||
                                actions.get(k).equals(MANAGER_RESOURCE_BEHAVIOUR)) {
                            isShared = true;
                        }
                        k++;
                    }
                    j++;
                }
                i++;
            }

            if (!isShared && !form.getString(OWNER_ID).equals(user.getUserId())) {
                isShared = true;
            }

            form.put(COLLAB, isShared);
            formService.update(formId, form, updateEvt -> {
                if (updateEvt.isLeft()) {
                    log.error("[Formulaire@SharingController::updateFormCollabProp] Fail to update form : " + updateEvt.left().getValue());
                    handler.handle(new Either.Left<>(updateEvt.left().getValue()));
                }
                else {
                    handler.handle(new Either.Right<>(updateEvt.right().getValue()));
                }
            });
        });
    }

    private void fixBugAutoUnsharing(HttpServerRequest request, String formId, UserInfos user, ShareObject shareObject) {
        formShareService.getSharedWithMe(formId, user, formSharedEvt -> {
            if (formSharedEvt.isLeft()) {
                log.error("[Formulaire@SharingController::getSharedWithMe] Fail to get user's shared rights");
                badRequest(request);
                return;
            }

            JsonArray rights = formSharedEvt.right().getValue();
            String id = user.getUserId();
            shareObject.getUsers().put(id, new JsonArray());

            for (int i = 0; i < rights.size(); i++) {
                JsonObject right = rights.getJsonObject(i);
                shareObject.getUsers().getJsonArray(id).add(right.getString(ACTION));
            }

            JsonObject jsonShareObject = shareObject.toJson();

            // Classic sharing stuff (putting or removing ids from form_shares table accordingly)
            this.getShareService().share(user.getUserId(), formId, jsonShareObject, (r) -> {
                if (r.isRight()) {
                    this.doShareSucceed(request, formId, user, jsonShareObject, r.right().getValue(), false);
                } else {
                    JsonObject error = (new JsonObject()).put(ERROR, r.left().getValue());
                    Renders.renderJson(request, error, 400);
                }
            });
        });
    }
}