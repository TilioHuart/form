package fr.openent.formulaire.service;

import fr.openent.form.core.models.Folder;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.Optional;

public interface FolderService {
    /**
     * List all the folders
     * @param user      user connected
     * @param handler   function handler returning JsonArray data
     */
    void list(UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * List all the folders from a list of ids
     * @param folderIds folder identifiers
     * @param handler   function handler returning JsonArray data
     */
    void listByIds(JsonArray folderIds, Handler<Either<String, JsonArray>> handler);

    /**
     * @deprecated Use {@link #get(String)}
     * Get a folder by id
     * @param folderId  folder identifier
     * @param handler   function handler returning JsonObject data
     */
    void get(String folderId, Handler<Either<String, JsonObject>> handler);

    /**
     * Get a folder by id
     * @param folderId  folder identifier
     */
    Future<Optional<Folder>> get(String folderId);

    /**
     * Create a folder
     * @param folder    folder to create
     * @param user      connected user
     * @param handler   function handler returning JsonObject data
     */
    void create(JsonObject folder, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Update a folder
     * @param folderId  folder identifier
     * @param folder    folder to update
     * @param handler   function handler returning JsonObject data
     */
    void update(String folderId, JsonObject folder, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete several folders
     * @param folderIds JsonArray of folder identifier
     * @param handler   function handler returning JsonArray data
     */
    void delete(JsonArray folderIds, Handler<Either<String, JsonArray>> handler);

    /**
     * Move folders
     * @param folderIds JsonArray of folder identifiers to move
     * @param parentId  folder identifier of new parent folder
     * @param handler   function handler returning JsonArray data
     */
    void move(JsonArray folderIds, String parentId, Handler<Either<String, JsonArray>> handler);

    /**
     * Sync number of children for a list of specific folders
     * @param user      user connected
     * @param folderIds  folder identifiers
     * @param handler   function handler returning JsonObject data
     */
    void syncNbChildren(UserInfos user, JsonArray folderIds, Handler<Either<String, JsonArray>> handler);
}
