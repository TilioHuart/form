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
     * Update a specific section
     * @param sectionId question identifier
     * @param section JsonObject data
     * @param handler function handler returning JsonObject data
     */
    void update(String sectionId, JsonObject section, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete a specific section
     * @param sectionId question identifier
     * @param handler function handler returning JsonObject data
     */
    void delete(String sectionId, Handler<Either<String, JsonObject>> handler);
}
