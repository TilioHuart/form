package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface DistributionService {
    /**
     * List all the distributions of the forms sent by me
     * @param user user connected
     * @param handler function handler returning JsonArray data
     */
    void listBySender(UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * List all the distributions of the forms sent to me
     * @param user user connected
     * @param handler function handler returning JsonArray data
     */
    void listByResponder(UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * List all the distributions of a specific form
     * @param formId form identifier
     * @param handler function handler returning JsonArray data
     */
    void listByForm(String formId, Handler<Either<String, JsonArray>> handler);

    /**
     * List all the distributions of a specific form with specific status
     * @param formId form identifier
     * @param status status
     * @param nbLines number of lines already loaded
     * @param handler function handler returning JsonArray data
     */
    void listByFormAndStatus(String formId, String status, String nbLines, Handler<Either<String, JsonArray>> handler);

    /**
     * List all the distributions for a specific form sent to me
     * @param formId form identifier
     * @param user user connected
     * @param handler function handler returning JsonArray data
     */
    void listByFormAndResponder(String formId, UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * Get the number of distributions for a specific form
     * @param formId form identifier
     * @param handler function handler returning JsonObject data
     */
    void count(String formId, Handler<Either<String, JsonObject>> handler);

    /**
     * Get a specific distribution by id
     * @param distributionId distribution identifier
     * @param handler function handler returning JsonObject data
     */
    void get(String distributionId, Handler<Either<String, JsonObject>> handler);

    /**
     * Get a specific distribution by form, responder and status
     * @param formId form identifier
     * @param user user connected
     * @param handler function handler returning JsonObject data
     */
    void getByFormResponderAndStatus(String formId, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Distribute a form to a list of responders
     * @param formId form identifier
     * @param user user connected
     * @param responders responders to whom we give respond right
     * @param handler function handler returning JsonObject data
     */
    void create(String formId, UserInfos user, JsonArray responders, Handler<Either<String, JsonObject>> handler);

    /**
     * Create a new distribution based on an already existing one
     * @param distribution JsonObject data
     * @param handler function handler returning JsonObject data
     */
    void add(JsonObject distribution, Handler<Either<String, JsonObject>> handler);

    /**
     * Update a specific distribution
     * @param distributionId distribution identifier
     * @param distribution JsonObject data
     * @param handler function handler returning JsonObject data
     */
    void update(String distributionId, JsonObject distribution, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete a specific distribution
     * @param distributionId distribution identifier
     * @param handler function handler returning JsonObject data
     */
    void delete(String distributionId, Handler<Either<String, JsonObject>> handler);

    /**
     * List of the responders who had respond right and won't have anymore
     * @param formId form identifier
     * @param responders responders to whom we give respond right
     * @param handler function handler returning JsonArray data
     */
    void getDeactivated(String formId, JsonArray responders, Handler<Either<String, JsonArray>> handler);

    /**
     * List of the responders who already had respond right
     * @param formId form identifier
     * @param responders responders to whom we give respond right
     * @param handler function handler returning JsonArray data
     */
    void getDuplicates(String formId, JsonArray responders, Handler<Either<String, JsonArray>> handler);

    /**
     * Distribute a form to a list of responders who do not already have respond right
     * @param formId form identifier
     * @param user user connected
     * @param responders responders to whom we give respond right
     * @param duplicates responders who already had respond right
     * @param handler function handler returning JsonObject data
     */
    void createMultiple(String formId, UserInfos user, JsonArray responders, JsonArray duplicates, Handler<Either<String, JsonObject>> handler);

    /**
     * Update active status of all distributions for a specific form
     * @param active status value to give to all the distributions
     * @param formId form identifier
     * @param duplicates distributions
     * @param handler function handler returning JsonObject data
     */
    void setActiveValue(boolean active, String formId, JsonArray duplicates, Handler<Either<String, JsonObject>> handler);
}