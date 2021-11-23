package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.helpers.RenderHelper;
import fr.openent.formulaire.helpers.FutureHelper;
import fr.openent.formulaire.security.CreationRight;
import fr.openent.formulaire.service.FolderService;
import fr.openent.formulaire.service.FormService;
import fr.openent.formulaire.service.RelFormFolderService;
import fr.openent.formulaire.service.impl.DefaultFolderService;
import fr.openent.formulaire.service.impl.DefaultFormService;
import fr.openent.formulaire.service.impl.DefaultRelFormFolderService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class FolderController extends ControllerHelper {
    private final FolderService folderService;
    private final FormService formService;
    private final RelFormFolderService relFormFolderService;

    public FolderController() {
        super();
        this.folderService = new DefaultFolderService();
        this.formService = new DefaultFormService();
        this.relFormFolderService = new DefaultRelFormFolderService();
    }

    @Get("/folders")
    @ApiDoc("List all the folders of the connected user")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void list(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                folderService.list(user, arrayResponseHandler(request));
            }
            else {
                log.error("User not found in session.");
                unauthorized(request);
            }
        });
    }

    @Get("/folders/:folderId")
    @ApiDoc("Get a folder by id")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {
        String folderId = request.getParam("folderId");
        folderService.get(folderId, defaultResponseHandler(request));
    }

    @Post("/folders")
    @ApiDoc("Create a folder")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void create(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJson(request, folder -> {
                    folder.put("user_id", user.getUserId());
                    folderService.create(folder, createEvent -> {
                        if (createEvent.isLeft()) {
                            log.error("[Formulaire@createFolder] Error in creating folder " + folder);
                            RenderHelper.badRequest(request, createEvent);
                        }
                        folderService.syncNbChildren(user, folder.getInteger("parent_id").toString(), defaultResponseHandler(request));
                    });
                });
            }
            else {
                log.error("User not found in session.");
                unauthorized(request);
            }
        });
    }

    @Put("/folders/:folderId")
    @ApiDoc("Update a folder")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void update(final HttpServerRequest request) {
        String folderId = request.getParam("folderId");
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJson(request, folder -> {
                    folder.put("user_id", user.getUserId());
                    folderService.update(folderId, folder, defaultResponseHandler(request));
                });
            }
            else {
                log.error("User not found in session.");
                unauthorized(request);
            }
        });
    }

    @Delete("/folders")
    @ApiDoc("Delete multiple folders")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void delete(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJsonArray(request, folders -> {
                    JsonArray folderIds = getIds(folders);

                    // Get all form ids to archive
                    relFormFolderService.listFormChildrenRecursively(folderIds, childrenEvent -> {
                        if (childrenEvent.isLeft()) {
                            log.error("[Formulaire@deleteFolder] Error in listing children forms for folders " + folderIds);
                            RenderHelper.badRequest(request, childrenEvent);
                            return;
                        }

                        // Change status all children forms to "archived"
                        JsonArray forms = childrenEvent.right().getValue();
                        List<Future> syncFutures = new ArrayList<>();
                        for (int i = 0; i < forms.size(); i++) {
                            JsonObject form = forms.getJsonObject(i);
                            String formId = form.getInteger("id").toString();
                            form.remove("archived");
                            form.put("archived", true);
                            Promise<JsonObject> promise = Promise.promise();
                            syncFutures.add(promise.future());
                            formService.update(formId, form, FutureHelper.handlerJsonObject(promise));
                        }
                        CompositeFuture.all(syncFutures).onComplete(evt -> {
                            if (evt.failed()) {
                                log.error("[Formulaire@deleteFolders] Failed to sync number children of folders : " + evt.cause());
                                renderJson(request, new JsonObject().put("error", evt.cause()), 400);
                            }

                            // Change all children forms relation folder (replace with "3")
                            JsonArray formIds = getIds(forms);
                            Integer parentId = folders.getJsonObject(0).getInteger("parent_id");

                            if (formIds.size() > 0) {
                                relFormFolderService.update(user, formIds, "3", updateRelEvent -> {
                                    if (updateRelEvent.isLeft()) {
                                        log.error("[Formulaire@deleteFolders] Error in updating relation form-folder for form ids " + formIds);
                                        RenderHelper.badRequest(request, updateRelEvent);
                                        return;
                                    }
                                    deleteFolders(request, user, parentId, folderIds);
                                });
                            }
                            else {
                                deleteFolders(request, user, parentId, folderIds);
                            }
                        });
                    });
                });
            }
            else {
                log.error("User not found in session.");
                unauthorized(request);
            }
        });
    }

    @Put("/folders/move/:folderId")
    @ApiDoc("Move a folder")
    @ResourceFilter(CreationRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void move(HttpServerRequest request) {
        Integer targetFolderId = Integer.parseInt(request.getParam("folderId"));
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJsonArray(request, folders -> {
                    Integer oldFolderId = folders.getJsonObject(0).getInteger("parent_id");
                    JsonArray folderIds = getIds(folders);
                    folderService.move(folderIds, targetFolderId.toString(), moveEvent -> {
                        if (moveEvent.isLeft()) {
                            log.error("[Formulaire@moveFolder] Error in moving folders " + folderIds);
                            RenderHelper.badRequest(request, moveEvent);
                            return;
                        }

                        if (targetFolderId != Formulaire.ID_ROOT_FOLDER) { // We do not sync root folder counts (useless)
                            folderService.syncNbChildren(user, targetFolderId.toString(), syncEvent -> {
                                if (syncEvent.isLeft()) {
                                    log.error("[Formulaire@moveFolder] Error in sync children counts for folder " + targetFolderId);
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
                            renderJson(request, new JsonObject(), 200);
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

    private JsonArray getIds(JsonArray items) {
        JsonArray ids = new JsonArray();
        for (int i = 0; i < items.size(); i++) {
            ids.add(items.getJsonObject(i).getInteger("id").toString());
        }
        return ids;
    }

    private void deleteFolders(HttpServerRequest request, UserInfos user, Integer parentId, JsonArray folderIds) {
        // Delete folders (and their children by cascade effect)
        folderService.delete(folderIds, deleteEvent -> {
            if (deleteEvent.isLeft()) {
                log.error("[Formulaire@deleteFolders] Error in deleting folders " + folderIds);
                RenderHelper.badRequest(request, deleteEvent);
                return;
            }

            // Sync parent folder children counts
            if (parentId != Formulaire.ID_ROOT_FOLDER) { // We do not sync root folder counts (useless)
                folderService.syncNbChildren(user, parentId.toString(), defaultResponseHandler(request));
            }
            else {
                RenderHelper.ok(request);
            }
        });
    }
}
