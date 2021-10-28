package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface FolderService {
    /**
     * List all the folders
     * @param user      user connected
     * @param handler   function handler returning JsonArray data
     */
    void list(UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * Get a folder by id
     * @param folderId  folder identifier
     * @param handler   function handler returning JsonObject data
     */
    void get(String folderId, Handler<Either<String, JsonObject>> handler);

    /**
     * Create a folder
     * @param folder    folder to create
     * @param handler   function handler returning JsonObject data
     */
    void create(JsonObject folder, Handler<Either<String, JsonObject>> handler);

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
     * Sync number of children for a folder
     * @param user      user connected
     * @param folderId  folder identifier
     * @param handler   function handler returning JsonObject data
     */
    void syncNbChildren(UserInfos user, String folderId, Handler<Either<String, JsonObject>> handler);
}
