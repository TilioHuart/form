package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import java.util.List;

public interface FormService {
    /**
     * List all forms created by me or shared with me
     * @param groupsAndUserIds list of neo ids including the connected user
     * @param user user connected
     * @param handler function handler returning JsonArray data
     */
    void list(List<String> groupsAndUserIds, UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * List all the forms sent to me
     * @param user user connected
     * @param handler function handler returning JsonArray data
     */
    void listSentForms(UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * List all the forms for the linker
     * @param groupsAndUserIds list of neo ids including the connected user
     * @param user user connected
     * @param handler function handler returning JsonArray data
     */
    void listForLinker(List<String> groupsAndUserIds, UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * List all the managers to a form
     * @param formId form identifier
     * @param handler function handler returning JsonArray data
     */
    void listContributors(String formId, Handler<Either<String, JsonArray>> handler);

    /**
     * List all the contributors to a form
     * @param formId form identifier
     * @param handler function handler returning JsonArray data
     */
    void listManagers(String formId, Handler<Either<String, JsonArray>> handler);

    /**
     * Get a specific form by id
     * @param formId form identifier
     * @param user user connected
     * @param handler function handler returning JsonObject data
     */
    void get(String formId, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Create a form
     * @param form JsonObject data
     * @param user user connected
     * @param handler function handler returning JsonObject data
     */
    void create(JsonObject form, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Create several forms
     * @param forms JsonArray data
     * @param user user connected
     * @param handler function handler returning JsonArray data
     */
    void createMultiple(JsonArray forms, UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * Duplicate several forms
     * @param formId form identifier
     * @param user user connected
     * @param handler function handler returning JsonArray data
     */
    void duplicate(int formId, UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * Update a specific form
     * @param formId form identifier
     * @param form JsonObject data
     * @param handler function handler returning JsonObject data
     */
    void update(String formId, JsonObject form, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete a scpecific form
     * @param formId form identifier
     * @param handler function handler returning JsonObject data
     */
    void delete(String formId, Handler<Either<String, JsonObject>> handler);

    /**
     * Get my rights for a specific form
     * @param formId form identifier
     * @param groupsAndUserIds list of neo ids including the connected user
     * @param handler function handler returning JsonArray data
     */
    void getMyFormRights(String formId, List<String> groupsAndUserIds, Handler<Either<String, JsonArray>> handler);

    /**
     * Get my rights for all the forms
     * @param groupsAndUserIds list of neo ids including the connected user
     * @param handler function handler returning JsonArray data
     */
    void getAllMyFormRights(List<String> groupsAndUserIds, Handler<Either<String, JsonArray>> handler);

    /**
     * Get image info form workspace for a specific image
     * @param eb event bus
     * @param idImage image identifier
     * @param handler function handler returning JsonObject data
     */
    void getImage (EventBus eb, String idImage, Handler<Either<String, JsonObject>> handler);
}
