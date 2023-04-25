package fr.openent.formulaire.service;

import fr.openent.form.core.models.Section;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
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
     * List all the sections of a specific form
     * @param formId form identifier
     */
    Future<JsonArray> list(String formId);

    /**
     * Get a specific section by id
     * @param sectionId section identifier
     * @param handler function handler returning JsonObject data
     */
    void get(String sectionId, Handler<Either<String, JsonObject>> handler);

    /**
     * Create a section in a specific form
     * @param section Section data
     * @param formId form identifier
     */
    Future<JsonObject> create(Section section, String formId);

    /**
     * Update specific sections
     * @param formId form identifier
     * @param sections JsonArray data
     */
    Future<JsonArray> update(String formId, JsonArray sections);

    /**
     * Delete a specific section
     * @param section section
     * @param handler function handler returning JsonObject data
     */
    void delete(JsonObject section, Handler<Either<String, JsonObject>> handler);

    /**
     * Check validity of a specific section
     * @param section Section data
     */
    Future<Boolean> isTargetValid(Section section);
}
