package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface RelFormFolderService {
    /**
     * List all the relations form-folders for a user
     * @param user      user connected
     * @param handler   function handler returning JsonArray data
     */
    void listAll(UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * List all the relations form-folders for the folder of an user
     * @param user      user connected
     * @param folderId  folder identifier
     * @param handler   function handler returning JsonArray data
     */
    void listByFolder(UserInfos user, String folderId, Handler<Either<String, JsonArray>> handler);

    /**
     * List all the children forms for the folder of an user
     * @param folderIds folder identifiers
     * @param handler   function handler returning JsonArray data
     */
    void listFormChildrenRecursively(JsonArray folderIds, Handler<Either<String, JsonArray>> handler);

    /**
     * Get a relation form-folder by form id
     * @param formId    form identifier
     * @param handler   function handler returning JsonObject data
     */
    void get(UserInfos user, String formId, Handler<Either<String, JsonObject>> handler);

    /**
     * Create multiple relations form-folder
     * @param user      user connected
     * @param formIds   form identifiers
     * @param folderId  folder identifier
     * @param handler   function handler returning JsonArray data
     */
    void create(UserInfos user, JsonArray formIds, String folderId, Handler<Either<String, JsonArray>> handler);

    /**
     * Update multiple relations form-folder
     * @param user          user connected
     * @param formIds       form identifiers
     * @param newFolderId   new folder identifier
     * @param handler       function handler returning JsonArray data
     */
    void update(UserInfos user, JsonArray formIds, String newFolderId, Handler<Either<String, JsonArray>> handler);
}
