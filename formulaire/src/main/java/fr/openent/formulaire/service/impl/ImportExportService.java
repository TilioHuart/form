package fr.openent.formulaire.service.impl;

import fr.openent.form.helpers.UtilsHelper;
import fr.openent.form.helpers.FutureHelper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Constants.*;
import static fr.openent.form.core.constants.Fields.QUESTION_TYPE;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.FolderIds.ID_ROOT_FOLDER;
import static fr.openent.form.core.constants.Tables.*;

public class ImportExportService {
    private static final Logger log = LoggerFactory.getLogger(ImportExportService.class);
    private final Sql sql;
    private final FileSystem fs;

    public ImportExportService(Sql sql, FileSystem fs) {
        this.sql = sql;
        this.fs = fs;
    }

    // Export

    /**
     * Write queries for export of data and store them into the 'infos' object
     * @param forms forms available for the connected user
     * @param infos object that will store queries for the data exports
     */
    public void onSuccessGetUserForms(JsonArray forms, HashMap<String,JsonArray> infos) {
        JsonArray formIds = UtilsHelper.getIds(forms);

        // Section query
        String sectionTableQuery = "SELECT * FROM " + SECTION_TABLE + " WHERE form_id IN " + Sql.listPrepared(formIds);
        JsonArray sectionTableParams = new JsonArray().addAll(formIds);
        infos.put(SECTION_TABLE, new SqlStatementsBuilder().prepared(sectionTableQuery, sectionTableParams).build());

        // Question query
        String questionTableQuery = "SELECT * FROM " + QUESTION_TABLE + " WHERE form_id IN " + Sql.listPrepared(formIds);
        JsonArray questionTableParams = new JsonArray().addAll(formIds);
        infos.put(QUESTION_TABLE, new SqlStatementsBuilder().prepared(questionTableQuery, questionTableParams).build());

        // Question specific query
        String questionSpecificTableQuery = "SELECT qs.* FROM " + QUESTION_SPECIFIC_FIELDS_TABLE + " qs " +
                "JOIN " + QUESTION_TABLE + " q ON q.id = qs.question_id " +
                (!formIds.isEmpty() ? "WHERE form_id IN " + Sql.listPrepared(formIds) + " " : "");
        JsonArray questionSpecificTableParams = new JsonArray().addAll(formIds);
        infos.put(QUESTION_SPECIFIC_FIELDS_TABLE, new SqlStatementsBuilder().prepared(questionSpecificTableQuery, questionSpecificTableParams).build());

        // QuestionChoice query
        String questionChoiceTableQuery = "SELECT qc.* FROM " + QUESTION_CHOICE_TABLE + " qc " +
                "JOIN " + QUESTION_TABLE + " q ON q.id = qc.question_id " +
                (!formIds.isEmpty() ? "WHERE form_id IN " + Sql.listPrepared(formIds) + " " : "");
        JsonArray questionChoiceTableParams = new JsonArray().addAll(formIds);
        infos.put(QUESTION_CHOICE_TABLE, new SqlStatementsBuilder().prepared(questionChoiceTableQuery, questionChoiceTableParams).build());
    }

    // Import

    /**
     * Read imported file and get content of the given table
     * @param importPath path to the imported file
     * @param schema schema name of the imported data
     * @param table name of the table to import data
     * @param tableContents Map where to store the content of the table
     * @return return a future containing the content of the table
     */
    public Future<JsonObject> getTableContent(String importPath, String schema, String table, Map<String, JsonObject> tableContents) {
        Promise<JsonObject> promise = Promise.promise();

        String path = importPath + File.separator + schema + "." + table;
        this.fs.readFile(path, (result) -> {
            if (result.failed()) {
                String errorMessage = "[Formulaire@ImportExportService::getTableContent] Failed to read table " + schema + "." + table + " in archive : ";
                log.error(errorMessage + result.cause());
                promise.fail(result.cause());
            }
            else {
                log.info("[Formulaire@ImportExportService::getTableContent] Succeed to get content for " + schema + "." + table + " in archive.");
                JsonObject results = result.result().toJsonObject();
                tableContents.put(table, results);
                promise.complete(results);
            }
        });

        return promise.future();
    }

    /**
     * Insert in BDD the imported forms data
     * @param forms content of the imported forms to insert
     * @param userId id of the connected user
     * @param userName username of the connected user
     * @return return a future containing the content the imported forms
     */
    public Future<JsonArray> importForms(JsonObject forms, String userId, String userName) {
        Promise<JsonArray> promise = Promise.promise();

        List<String> fields = forms.getJsonArray(FIELDS).getList();
        JsonArray results = forms.getJsonArray(RESULTS);

        SqlStatementsBuilder s = new SqlStatementsBuilder();
        String query = "INSERT INTO " + FORM_TABLE + " (title, description, owner_id, owner_name, date_opening, date_ending, " +
                "multiple, anonymous, response_notified, editable, rgpd, rgpd_goal, rgpd_lifetime, is_public, public_key, original_form_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "RETURNING original_form_id, id;";

        s.raw(TRANSACTION_BEGIN_QUERY);
        for (int i = 0; i < results.size(); i++) {
            JsonArray entry = results.getJsonArray(i);
            boolean isPublic = entry.getBoolean(fields.indexOf(IS_PUBLIC));
            JsonArray params = new JsonArray()
                    .add(entry.getString(fields.indexOf(TITLE)))
                    .add(entry.getString(fields.indexOf(DESCRIPTION)))
                    .add(userId)
                    .add(userName)
                    .add(entry.getString(fields.indexOf(DATE_OPENING)))
                    .add(entry.getString(fields.indexOf(DATE_ENDING)))
                    .add(entry.getBoolean(fields.indexOf(MULTIPLE)))
                    .add(entry.getBoolean(fields.indexOf(ANONYMOUS)))
                    .add(entry.getBoolean(fields.indexOf(RESPONSE_NOTIFIED)))
                    .add(entry.getBoolean(fields.indexOf(EDITABLE)))
                    .add(entry.getBoolean(fields.indexOf(RGPD)))
                    .add(entry.getString(fields.indexOf(RGPD_GOAL)))
                    .add(entry.getInteger(fields.indexOf(RGPD_LIFETIME)))
                    .add(isPublic)
                    .add(isPublic ? UUID.randomUUID().toString() : null)
                    .add(entry.getInteger(fields.indexOf(ID)));
            s.prepared(query, params);
        }
        s.raw(TRANSACTION_COMMIT_QUERY);

        String errorMessage = "[Formulaire@ImportExportService::importForms] Failed to import forms from file : ";
        sql.transaction(s.build(), SqlResult.validResultsHandler(FutureHelper.handlerEither(promise, errorMessage)));

        return promise.future();
    }

    /**
     * Insert in BDD the imported sections data
     * @param sections content of the imported sections to insert
     * @param oldNewFormIdsMap Map containing old and new form ids
     * @return return a future containing the content the imported sections
     */
    public Future<JsonArray> importSections(JsonObject sections, Map<Integer, Integer> oldNewFormIdsMap) {
        Promise<JsonArray> promise = Promise.promise();

        List<String> fields = sections.getJsonArray(FIELDS).getList();
        JsonArray results = sections.getJsonArray(RESULTS);

        SqlStatementsBuilder s = new SqlStatementsBuilder();
        String query = "INSERT INTO " + SECTION_TABLE + " (form_id, title, description, position, original_section_id) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "RETURNING original_section_id, id;";

        s.raw(TRANSACTION_BEGIN_QUERY);
        for (int i = 0; i < results.size(); i++) {
            JsonArray entry = results.getJsonArray(i);
            JsonArray params = new JsonArray()
                    .add(oldNewFormIdsMap.get(entry.getInteger(fields.indexOf(FORM_ID))))
                    .add(entry.getString(fields.indexOf(TITLE)))
                    .add(entry.getString(fields.indexOf(DESCRIPTION)))
                    .add(entry.getInteger(fields.indexOf(POSITION)))
                    .add(entry.getInteger(fields.indexOf(ID)));
            s.prepared(query, params);
        }
        s.raw(TRANSACTION_COMMIT_QUERY);

        String errorMessage = "[Formulaire@ImportExportService::importSections] Failed to import sections from file : ";
        sql.transaction(s.build(), SqlResult.validResultsHandler(FutureHelper.handlerEither(promise, errorMessage)));

        return promise.future();
    }

    /**
     * Insert in BDD the imported questions data
     * @param questions content of the imported questions to insert
     * @param oldNewSectionIdsMap Map containing old and new section ids
     * @param oldNewFormIdsMap Map containing old and new form ids
     * @return return a future containing the content the imported questions
     */
    public Future<JsonArray> importQuestions(JsonObject questions, Map<Integer, Integer> oldNewSectionIdsMap, Map<Integer, Integer> oldNewFormIdsMap) {
        Promise<JsonArray> promise = Promise.promise();

        List<String> fields = questions.getJsonArray(FIELDS).getList();
        int indexMatrixIdField = fields.indexOf(MATRIX_ID);
        List<JsonArray> results = questions.getJsonArray(RESULTS).stream()
                .filter(entry -> ((JsonArray) entry).getInteger(indexMatrixIdField) == null)
                .map(JsonArray.class::cast)
                .collect(Collectors.toList());

        SqlStatementsBuilder s = new SqlStatementsBuilder();
        String query = "INSERT INTO " + QUESTION_TABLE + " (form_id, title, position, question_type, statement, " +
                "mandatory, original_question_id, section_id, section_position, conditional, placeholder, matrix_id, matrix_position) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "RETURNING original_question_id, id;";

        s.raw(TRANSACTION_BEGIN_QUERY);
        for (JsonArray entry : results) {
            JsonArray params = new JsonArray()
                    .add(oldNewFormIdsMap.get(entry.getInteger(fields.indexOf(FORM_ID))))
                    .add(entry.getString(fields.indexOf(TITLE)))
                    .add(entry.getInteger(fields.indexOf(POSITION)))
                    .add(entry.getInteger(fields.indexOf(QUESTION_TYPE)))
                    .add(entry.getString(fields.indexOf(STATEMENT)))
                    .add(entry.getBoolean(fields.indexOf(MANDATORY)))
                    .add(entry.getInteger(fields.indexOf(ID)))
                    .add(oldNewSectionIdsMap.get(entry.getInteger(fields.indexOf(SECTION_ID))))
                    .add(entry.getInteger(fields.indexOf(SECTION_POSITION)))
                    .add(entry.getBoolean(fields.indexOf(CONDITIONAL)))
                    .add(entry.getString(fields.indexOf(PLACEHOLDER)))
                    .add(entry.getInteger(fields.indexOf(MATRIX_ID)))
                    .add(entry.getInteger(fields.indexOf(MATRIX_POSITION)));
            s.prepared(query, params);
        }
        s.raw(TRANSACTION_COMMIT_QUERY);

        String errorMessage = "[Formulaire@ImportExportService::importQuestions] Failed to import questions from file : ";
        sql.transaction(s.build(), SqlResult.validResultsHandler(FutureHelper.handlerEither(promise, errorMessage)));

        return promise.future();
    }


    /**
     * Insert in BDD the imported children questions data for matrix
     * @param questions content of the imported questions to insert
     * @param oldNewQuestionIdsMap Map containing old and new question ids
     * @param oldNewSectionIdsMap Map containing old and new section ids
     * @param oldNewFormIdsMap Map containing old and new form ids
     * @return return a future containing the content the imported children questions
     */
    public Future<JsonArray> importChildrenQuestions(JsonObject questions, Map<Integer, Integer> oldNewQuestionIdsMap, Map<Integer, Integer> oldNewSectionIdsMap, Map<Integer, Integer> oldNewFormIdsMap) {
        Promise<JsonArray> promise = Promise.promise();

        List<String> fields = questions.getJsonArray(FIELDS).getList();
        int indexMatrixIdField = fields.indexOf(MATRIX_ID);
        List<JsonArray> results = questions.getJsonArray(RESULTS).stream()
                .filter(entry -> ((JsonArray) entry).getInteger(indexMatrixIdField) != null)
                .map(JsonArray.class::cast)
                .collect(Collectors.toList());

        SqlStatementsBuilder s = new SqlStatementsBuilder();
        String query = "INSERT INTO " + QUESTION_TABLE + " (form_id, title, position, question_type, statement, " +
                "mandatory, original_question_id, section_id, section_position, conditional, placeholder, matrix_id, matrix_position) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "RETURNING original_question_id, id;";

        s.raw(TRANSACTION_BEGIN_QUERY);
        for (JsonArray entry : results) {
            JsonArray params = new JsonArray()
                    .add(oldNewFormIdsMap.get(entry.getInteger(fields.indexOf(FORM_ID))))
                    .add(entry.getString(fields.indexOf(TITLE)))
                    .add(entry.getInteger(fields.indexOf(POSITION)))
                    .add(entry.getInteger(fields.indexOf(QUESTION_TYPE)))
                    .add(entry.getString(fields.indexOf(STATEMENT)))
                    .add(entry.getBoolean(fields.indexOf(MANDATORY)))
                    .add(entry.getInteger(fields.indexOf(ID)))
                    .add(oldNewSectionIdsMap.get(entry.getInteger(fields.indexOf(SECTION_ID))))
                    .add(entry.getInteger(fields.indexOf(SECTION_POSITION)))
                    .add(entry.getBoolean(fields.indexOf(CONDITIONAL)))
                    .add(entry.getString(fields.indexOf(PLACEHOLDER)))
                    .add(oldNewQuestionIdsMap.get(entry.getInteger(fields.indexOf(MATRIX_ID))))
                    .add(entry.getInteger(fields.indexOf(MATRIX_POSITION)));
            s.prepared(query, params);
        }
        s.raw(TRANSACTION_COMMIT_QUERY);

        String errorMessage = "[Formulaire@ImportExportService::importChildrenQuestions] Failed to import children questions from file : ";
        sql.transaction(s.build(), SqlResult.validResultsHandler(FutureHelper.handlerEither(promise, errorMessage)));

        return promise.future();
    }

    /**
     * Insert in BDD the imported question specifics data
     * @param questionSpecifics content of the imported question specifics to insert
     * @param oldNewQuestionIdsMap Map containing old and new question ids
     * @return return a future containing the content the imported question choices
     */
    public Future<JsonArray> importQuestionSpecifics(JsonObject questionSpecifics, Map<Integer, Integer> oldNewQuestionIdsMap) {
        Promise<JsonArray> promise = Promise.promise();

        List<String> fields = questionSpecifics.getJsonArray(FIELDS).getList();
        JsonArray results = questionSpecifics.getJsonArray(RESULTS);

        SqlStatementsBuilder s = new SqlStatementsBuilder();
        String query = "INSERT INTO " + QUESTION_SPECIFIC_FIELDS_TABLE + " (question_id, cursor_min_val, cursor_max_val, " +
                "cursor_step, cursor_min_label, cursor_max_label) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING *;";

        s.raw(TRANSACTION_BEGIN_QUERY);
        for (int i = 0; i < results.size(); ++i) {
            JsonArray entry = results.getJsonArray(i);
            JsonArray params = new JsonArray()
                    .add(oldNewQuestionIdsMap.get(entry.getInteger(fields.indexOf(QUESTION_ID))))
                    .add(entry.getInteger(fields.indexOf(CURSOR_MIN_VAL)))
                    .add(entry.getInteger(fields.indexOf(CURSOR_MAX_VAL)))
                    .add(entry.getInteger(fields.indexOf(CURSOR_STEP)))
                    .add(entry.getString(fields.indexOf(CURSOR_MIN_LABEL)))
                    .add(entry.getString(fields.indexOf(CURSOR_MAX_LABEL)));
            s.prepared(query, params);
        }
        s.raw(TRANSACTION_COMMIT_QUERY);

        String errorMessage = "[Formulaire@ImportExportService::importQuestionSpecifics] Failed to import question_specific_fields from file : ";
        sql.transaction(s.build(), SqlResult.validResultsHandler(FutureHelper.handlerEither(promise, errorMessage)));

        return promise.future();
    }

    /**
     * Insert in BDD the imported question choices data
     * @param questionChoices content of the imported question choices to insert
     * @param oldNewQuestionIdsMap Map containing old and new question ids
     * @param oldNewSectionIdsMap Map containing old and new section ids
     * @return return a future containing the content the imported question choices
     */
    public Future<JsonArray> importQuestionChoices(JsonObject questionChoices, Map<Integer, Integer> oldNewQuestionIdsMap, Map<Integer, Integer> oldNewSectionIdsMap) {
        Promise<JsonArray> promise = Promise.promise();

        List<String> fields = questionChoices.getJsonArray(FIELDS).getList();
        JsonArray results = questionChoices.getJsonArray(RESULTS);

        SqlStatementsBuilder s = new SqlStatementsBuilder();
        String query = "INSERT INTO " + QUESTION_CHOICE_TABLE + " (question_id, value, type, position, is_custom, " +
                "next_form_element_id, next_form_element_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING *;";

        s.raw(TRANSACTION_BEGIN_QUERY);
        for (int i = 0; i < results.size(); ++i) {
            JsonArray entry = results.getJsonArray(i);
            JsonArray params = new JsonArray()
                    .add(oldNewQuestionIdsMap.get(entry.getInteger(fields.indexOf(QUESTION_ID))))
                    .add(entry.getString(fields.indexOf(VALUE)))
                    .add(entry.getString(fields.indexOf(TYPE)))
                    .add(entry.getInteger(fields.indexOf(POSITION)))
                    .add(entry.getBoolean(fields.indexOf(IS_CUSTOM)))
                    .add(oldNewSectionIdsMap.get(entry.getInteger(fields.indexOf(NEXT_FORM_ELEMENT_ID))))
                    .add(entry.getString(fields.indexOf(NEXT_FORM_ELEMENT_TYPE)));
            s.prepared(query, params);
        }
        s.raw(TRANSACTION_COMMIT_QUERY);

        String errorMessage = "[Formulaire@ImportExportService::importQuestionChoices] Failed to import question_choices from file : ";
        sql.transaction(s.build(), SqlResult.validResultsHandler(FutureHelper.handlerEither(promise, errorMessage)));

        return promise.future();
    }

    /**
     * Create relations form-folder for the new forms
     * @param oldNewFormIdsMap Map containing old and new form ids
     * @param userId id of the connected user
     * @return return a future containing the created form-folder relations
     */
    public Future<JsonArray> createFolderLinks(Map<Integer, Integer> oldNewFormIdsMap, String userId) {
        Promise<JsonArray> promise = Promise.promise();

        List<Integer> formIds = new ArrayList<>(oldNewFormIdsMap.values());

        SqlStatementsBuilder s = new SqlStatementsBuilder();
        String query = "INSERT INTO " + REL_FORM_FOLDER_TABLE + " (user_id, form_id, folder_id) VALUES (?, ?, ?) RETURNING *;";

        s.raw(TRANSACTION_BEGIN_QUERY);
        for (Integer id : formIds) {
            JsonArray params = new JsonArray().add(userId).add(id).add(ID_ROOT_FOLDER);
            s.prepared(query, params);
        }
        s.raw(TRANSACTION_COMMIT_QUERY);

        String errorMessage = "[Formulaire@ImportExportService::createFolderLinks] Failed to create relations form-folders for forms with id " + formIds + " : ";
        sql.transaction(s.build(), SqlResult.validResultsHandler(FutureHelper.handlerEither(promise, errorMessage)));

        return promise.future();
    }

    /**
     * Generate mappings between two fields from given data
     * @param data data results of a transaction
     * @param keyName name of the field to get to fill the map's keys
     * @param valueName name of the field to get to fill the map's values
     * @return A complete mapping between the two given fields
     */
    public Map<Integer, Integer> generateMapping(JsonArray data, String keyName, String valueName) {
        Map<Integer, Integer> map = new HashMap();
        for (int i = 1; i < data.size() - 1; i++) {
            JsonObject entry = data.getJsonArray(i).getJsonObject(0);
            map.put(entry.getInteger(keyName), entry.getInteger(valueName));
        }
        return map;
    }
}
