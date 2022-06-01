package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface SectionService {
    /**
     * List all the sections of a specific form
     * @param formId form identifier
     * @param handler function handler returning JsonArray data
     */
    void list(String formId, Handler<Either<String, JsonArray>> handler);

    /**
     * Get a specific section by id
     * @param sectionId section identifier
     * @param handler function handler returning JsonObject data
     */
    void get(String sectionId, Handler<Either<String, JsonObject>> handler);

    /**
     * Create a section in a specific form
     * @param section JsonObject data
     * @param formId form identifier
     * @param handler function handler returning JsonObject data
     */
    void create(JsonObject section, String formId, Handler<Either<String, JsonObject>> handler);

    /**
     * Update specific sections
     * @param formId form identifier
     * @param sections JsonArray data
     * @param handler function handler returning JsonArray data
     */
    void update(String formId, JsonArray sections, Handler<Either<String, JsonArray>> handler);

    /**
     * Delete a specific section
     * @param section section
     * @param handler function handler returning JsonObject data
     */
    void delete(JsonObject section, Handler<Either<String, JsonObject>> handler);
}
