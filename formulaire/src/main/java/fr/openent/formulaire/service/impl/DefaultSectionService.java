package fr.openent.formulaire.service.impl;

import fr.openent.form.core.enums.FormElementTypes;
import fr.openent.form.core.models.QuestionChoice;
import fr.openent.form.core.models.Section;
import fr.openent.form.helpers.FutureHelper;
import fr.openent.form.helpers.UtilsHelper;
import fr.openent.formulaire.service.SectionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;

import static fr.openent.form.core.constants.Constants.TRANSACTION_BEGIN_QUERY;
import static fr.openent.form.core.constants.Constants.TRANSACTION_COMMIT_QUERY;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.QUESTION_TABLE;
import static fr.openent.form.core.constants.Tables.SECTION_TABLE;
import static fr.openent.form.helpers.SqlHelper.getParamsForUpdateDateModifFormRequest;
import static fr.openent.form.helpers.SqlHelper.getUpdateDateModifFormRequest;

public class DefaultSectionService implements SectionService {
    private final Sql sql = Sql.getInstance();
    private static final Logger log = LoggerFactory.getLogger(DefaultSectionService.class);

    @Override
    public void list(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + SECTION_TABLE + " WHERE form_id = ? ORDER BY position;";
        JsonArray params = new JsonArray().add(formId);
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public Future<JsonArray> list(String formId) {
        Promise<JsonArray> promise = Promise.promise();

        String errorMessage = "[Formulaire@DefaultSectionService::list] Fail to list sections for form with id " + formId + " : ";
        list(formId, FutureHelper.handlerEither(promise, errorMessage));

        return promise.future();
    }

    @Override
    public void get(String sectionId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + SECTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(sectionId);
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public Future<JsonObject> create(Section section, String formId) {
        Promise<JsonObject> promise = Promise.promise();

        String query = "INSERT INTO " + SECTION_TABLE + " (form_id, title, description, position, next_form_element_id, next_form_element_type) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(formId)
                .add(section.getTitle())
                .add(section.getDescription())
                .add(section.getPosition())
                .add(section.getNextFormElementId())
                .add(section.getNextFormElementType());

        query += getUpdateDateModifFormRequest();
        params.addAll(getParamsForUpdateDateModifFormRequest(formId));

        String errorMessage = "[Formulaire@DefaultSectionService::create] Fail to create section " + section + " : ";
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerEither(promise, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<JsonArray> update(String formId, JsonArray sections) {
        Promise<JsonArray> promise = Promise.promise();

        if (!sections.isEmpty()) {
            SqlStatementsBuilder s = new SqlStatementsBuilder();
            String nullifyerQuery = "UPDATE " + SECTION_TABLE + " SET position = NULL WHERE id IN " + Sql.listPrepared(sections) + ";";
            String query = "UPDATE " + SECTION_TABLE + " SET title = ?, description = ?, position = ?, next_form_element_id = ?, next_form_element_type = ? WHERE id = ? RETURNING *;";

            s.raw(TRANSACTION_BEGIN_QUERY);
            s.prepared(nullifyerQuery, UtilsHelper.getIds(sections, false));
            for (int i = 0; i < sections.size(); i++) {
                JsonObject section = sections.getJsonObject(i);
                JsonArray params = new JsonArray()
                        .add(section.getString(TITLE, ""))
                        .add(section.getString(DESCRIPTION, ""))
                        .add(section.getInteger(POSITION, null))
                        .add(section.getInteger(NEXT_FORM_ELEMENT_ID, null))
                        .add(section.getString(NEXT_FORM_ELEMENT_TYPE, null))
                        .add(section.getInteger(ID, null));
                s.prepared(query, params);
            }

            s.prepared(getUpdateDateModifFormRequest(), getParamsForUpdateDateModifFormRequest(formId));
            s.raw(TRANSACTION_COMMIT_QUERY);

            String errorMessage = "[Formulaire@DefaultSectionService::update] Fail to update sections " + sections + " : ";
            sql.transaction(s.build(), SqlResult.validResultsHandler(FutureHelper.handlerEither(promise, errorMessage)));
        }
        else {
            promise.complete(new JsonArray());
        }

        return promise.future();
    }

    @Override
    public void delete(JsonObject section, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + SECTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(section.getInteger(ID).toString());

        query += getUpdateDateModifFormRequest();
        params.addAll(getParamsForUpdateDateModifFormRequest(section.getInteger(FORM_ID).toString()));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }


    @Override
    public Future<Boolean> isTargetValid(Section section) {
        Promise<Boolean> promise = Promise.promise();

        if (section.getNextFormElementId() == null && section.getNextFormElementType() == null) {
            promise.complete(Boolean.TRUE);
            return promise.future();
        }
        else if (section.getNextFormElementId() == null ^ section.getNextFormElementType() == null) {
            String errorMessage = "[Formulaire@DefaultSectionService::isTargetValid] Section next_form_element_id " +
                    "and next_form_element_type must be both null or both not null.";
            log.error(errorMessage);
            promise.fail(errorMessage);
            return promise.future();
        }

        String targetedTable = section.getNextFormElementType() == FormElementTypes.QUESTION ? QUESTION_TABLE : SECTION_TABLE;

        String query =
                "SELECT COUNT(*) = 1 AS count FROM (SELECT id, form_id, position FROM " + targetedTable + ") AS targets_infos " +
                "WHERE form_id = ? AND position IS NOT NULL AND position > ? AND id = ?;";
        JsonArray params = new JsonArray().add(section.getFormId()).add(section.getPosition()).add(section.getNextFormElementId());

        String errorMessage = "[Formulaire@DefaultSectionService::isTargetValid] Fail to check if choice target is valid : ";
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue().getBoolean(COUNT));
                return;
            }
            log.error(errorMessage + event.left().getValue());
            promise.fail(event.left().getValue());
        }));

        return promise.future();
    }
}
