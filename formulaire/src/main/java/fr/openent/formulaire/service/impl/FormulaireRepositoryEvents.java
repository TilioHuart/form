package fr.openent.formulaire.service.impl;

import fr.openent.form.helpers.FutureHelper;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.entcore.common.service.impl.SqlRepositoryEvents;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static fr.openent.form.core.constants.Constants.*;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.ShareRights.CONTRIB_RESOURCE_BEHAVIOUR;
import static fr.openent.form.core.constants.ShareRights.MANAGER_RESOURCE_BEHAVIOUR;
import static fr.openent.form.core.constants.Tables.*;
import static fr.openent.form.core.constants.Tables.QUESTION;

public class FormulaireRepositoryEvents extends SqlRepositoryEvents {
    private static final Logger log = LoggerFactory.getLogger(FormulaireRepositoryEvents.class);
    private ImportExportService importExportService;

    public FormulaireRepositoryEvents(Vertx vertx) {
        super(vertx);
        this.importExportService = new ImportExportService(sql, fs);
    }

    // Export/Import events

    /**
     * Function called by Archive module when the user wants to export his Formulaire data
     * @param resourcesIds ids of the forms we want to export, by default (null) we export all the available ones for the connected user
     * @param exportDocuments (??)
     * @param exportSharedResources (??)
     * @param exportId id given to the generated folder (= name of the folder)
     * @param userId id of the connected user
     * @param groups groups where the connected user belongs
     * @param exportPath path of the directory generated and exported
     * @param locale time zone of the user
     * @param host host
     * @param handler function handler
     */
    @Override
    public void exportResources(JsonArray resourcesIds, boolean exportDocuments, boolean exportSharedResources, String exportId,
                                String userId, JsonArray groups, String exportPath, String locale, String host, final Handler<Boolean> handler) {

        HashMap<String,JsonArray> infos = new HashMap<>();
        HashMap<String, JsonArray> fieldsToNull = new HashMap<>();
        AtomicBoolean exported = new AtomicBoolean(false);

        JsonArray groupsAndUserIds = new JsonArray();
        groupsAndUserIds.add(userId);
        if (groups != null) { groupsAndUserIds.addAll(groups); }

        // Form query
        Promise<JsonArray> promise = Promise.promise();
        String formTableQuery = "SELECT f.* FROM " + FORM_TABLE + " f " +
                "LEFT JOIN " + FORM_SHARES_TABLE + " fs ON f.id = fs.resource_id " +
                "LEFT JOIN " + MEMBERS_TABLE + " m ON (fs.member_id = m.id AND m.group_id IS NOT NULL) " +
                "WHERE ((fs.member_id IN " + Sql.listPrepared(groupsAndUserIds) + " AND (fs.action = ? OR fs.action = ?)) OR f.owner_id = ?) " +
                (resourcesIds != null && !resourcesIds.isEmpty() ? "AND f.id IN " + Sql.listPrepared(resourcesIds) + " " : "") +
                "GROUP BY f.id " +
                "ORDER BY f.date_modification DESC;";
        JsonArray formTableParams = new JsonArray()
                .addAll(groupsAndUserIds)
                .add(MANAGER_RESOURCE_BEHAVIOUR)
                .add(CONTRIB_RESOURCE_BEHAVIOUR)
                .add(userId)
                .addAll(resourcesIds != null && !resourcesIds.isEmpty() ? resourcesIds : new JsonArray());

        String errorMessage = "[Formulaire@FormulaireRepositoryEvents::exportResources] Failed to list user's forms : ";
        Sql.getInstance().prepared(formTableQuery, formTableParams, SqlResult.validResultHandler(FutureHelper.handlerEither(promise, errorMessage)));

        promise.future()
            .onFailure(err -> log.error(err.getMessage()))
            .onSuccess(forms -> {
                infos.put(FORM_TABLE, new SqlStatementsBuilder().prepared(formTableQuery, formTableParams).build());
                importExportService.onSuccessGetUserForms(forms, infos);

                // Directory creation and continue the export
                createExportDirectory(exportPath, locale, path -> {
                    if (path != null) {
                        exportTables(infos, new JsonArray(), fieldsToNull, exportDocuments, path, exported, handler);
                    }
                    else {
                        handler.handle(exported.get());
                    }
                });
            });
    }

    /**
     * Function called by Archive module when the user wants to import his exported Formulaire data
     * @param importId id of the folder imported (= name of the folder)
     * @param userId id of the connected user
     * @param userLogin login of the connected user
     * @param userName username of the connected user
     * @param importPath path of the directory imported
     * @param locale time zone of the user
     * @param host host
     * @param forceImportAsDuplication (useless)
     * @param handler function handler
     */
    @Override
    public void importResources(String importId, String userId, String userLogin, String userName, String importPath,
                                String locale, String host, boolean forceImportAsDuplication, Handler<JsonObject> handler) {

        SqlStatementsBuilder builder = new SqlStatementsBuilder();

        // Query Users
        String usersQuery= "INSERT INTO " + USERS_TABLE + " (id, username) VALUES (?,?) ON CONFLICT DO NOTHING";
        JsonArray usersParams = new JsonArray().add(userId).add(userName);
        builder.prepared(usersQuery, usersParams);

        // Query Members
        String membersQuery= "INSERT INTO " + MEMBERS_TABLE + " (id, user_id) VALUES (?,?) ON CONFLICT DO NOTHING";
        JsonArray membersParams = new JsonArray().add(userId).add(userId);
        builder.prepared(membersQuery, membersParams);

        // Continue
        sql.transaction(builder.build(), message -> {
            if (OK.equals(message.body().getString(STATUS))) {
                List<String> tables = new ArrayList<>(Arrays.asList(FORM, SECTION, QUESTION, QUESTION_CHOICE));
                Map<String,String> tablesWithId = new HashMap<>();
                tablesWithId.put(FORM, DEFAULT);
                tablesWithId.put(SECTION, DEFAULT);
                tablesWithId.put(QUESTION, DEFAULT);
                tablesWithId.put(QUESTION_SPECIFIC_FIELDS, DEFAULT);
                tablesWithId.put(QUESTION_CHOICE, DEFAULT);

                // Calls our custom importTables function (cf under)
                importTables(importPath, DB_SCHEMA, tables, tablesWithId, userId, userName, locale, new SqlStatementsBuilder(), forceImportAsDuplication, handler);
            }
            else {
                String errorMessage = "[Formulaire@FormulaireRepositoryEvents::importResources] Failed to create users/members for import : ";
                log.error(errorMessage + message.body().getString(MESSAGE));
                handler.handle(new JsonObject().put(STATUS, ERROR));
            }
        });
    }

    /**
     * Import the data contents given by the user
     * @param importPath path to the imported folder
     * @param schema schema name of the imported data
     * @param tables table names of the imported data
     * @param tablesWithId table names and their default 'id' value of the imported data
     * @param userId id of the connected user
     * @param userName username of the connected user
     * @param locale  time zone of the user
     * @param builder SqlStatementsBuilder that will contain the queries (unused here in our custom function)
     * @param forceImportAsDuplication (useless)
     * @param handler  function handler
     * @param idsMapByTable map that will contain for each table a map of old id/new id
     * @param duplicateSuffix (useless)
     */
    @Override
    protected void importTables(String importPath, String schema, List<String> tables, Map<String, String> tablesWithId,
                                String userId, String userName, String locale, SqlStatementsBuilder builder, boolean forceImportAsDuplication,
                                Handler<JsonObject> handler, Map<String, JsonObject> idsMapByTable, String duplicateSuffix) {

        // For each table we'll store in 'tableMappings' a map of the old ids with new ids
        Map<String, Map<Integer, Integer>> tableMappingIds = new HashMap();

        // For each table we'll store in 'tableContents' a map of the name of the table with the table content of the file
        Map<String, JsonObject> tableContents = new HashMap();

        importExportService.getTableContent(importPath, schema, FORM, tableContents)
            .compose(forms -> importExportService.getTableContent(importPath, schema, SECTION, tableContents))
            .compose(sections -> importExportService.getTableContent(importPath, schema, QUESTION, tableContents))
            .compose(questions -> importExportService.getTableContent(importPath, schema, QUESTION_CHOICE, tableContents))
            .compose(questionSpecifics -> importExportService.getTableContent(importPath, schema, QUESTION_SPECIFIC_FIELDS, tableContents))
            .compose(questionChoices -> importExportService.importForms(tableContents.get(FORM), userId, userName))
            .compose(oldNewFormIds -> {
                Map<Integer, Integer> oldNewFormIdsMap = importExportService.generateMapping(oldNewFormIds, ORIGINAL_FORM_ID, ID);
                tableMappingIds.put(FORM, oldNewFormIdsMap);
                return importExportService.importSections(tableContents.get(SECTION), oldNewFormIdsMap);
            })
            .compose(oldNewSectionIds -> {
                Map<Integer, Integer> oldNewSectionIdsMap = importExportService.generateMapping(oldNewSectionIds, ORIGINAL_SECTION_ID, ID);
                tableMappingIds.put(SECTION, oldNewSectionIdsMap);
                return importExportService.importQuestions(tableContents.get(QUESTION), oldNewSectionIdsMap, tableMappingIds.get(FORM));
            })
            .compose(oldNewQuestionIds -> {
                Map<Integer, Integer> oldNewQuestionIdsMap = importExportService.generateMapping(oldNewQuestionIds, ORIGINAL_QUESTION_ID, ID);
                tableMappingIds.put(QUESTION, oldNewQuestionIdsMap);
                return importExportService.importChildrenQuestions(tableContents.get(QUESTION), oldNewQuestionIdsMap, tableMappingIds.get(SECTION), tableMappingIds.get(FORM));
            })
            .compose(updatedChildrenQuestionsIds -> {
                Map<Integer, Integer> oldNewChildrenQuestionIdsMap = importExportService.generateMapping(updatedChildrenQuestionsIds, ORIGINAL_QUESTION_ID, ID);
                tableMappingIds.get(QUESTION).putAll(oldNewChildrenQuestionIdsMap);
                return importExportService.importQuestionSpecifics(tableContents.get(QUESTION_SPECIFIC_FIELDS), tableMappingIds.get(QUESTION));
            })
            .compose(newQuestionSpecifics -> importExportService.importQuestionChoices(tableContents.get(QUESTION_CHOICE), tableMappingIds.get(QUESTION), tableMappingIds.get(SECTION)))
            .compose(newQuestionChoices -> importExportService.createFolderLinks(tableMappingIds.get(FORM), userId))
            .onSuccess(result -> {
                int nbFormsImported = tableMappingIds.get(FORM).size();
                JsonObject finalResultInfos = new JsonObject().put(STATUS, OK)
                        .put(PARAM_RESOURCES_NUMBER, String.valueOf(nbFormsImported))
                        .put(PARAM_ERRORS_NUMBER, "-")
                        .put(PARAM_DUPLICATES_NUMBER, "-")
                        .put(PARAM_MAIN_RESOURCE_NAME, this.mainResourceName);

                log.info(this.title + " : Imported " + nbFormsImported + " forms for user " + userId);
                handler.handle(finalResultInfos);
            })
            .onFailure(err -> {
                int nbFormsImported = tableMappingIds.get(FORM).size();
                JsonObject finalResultInfos = new JsonObject().put(STATUS, ERROR)
                        .put(PARAM_RESOURCES_NUMBER, String.valueOf(0))
                        .put(PARAM_ERRORS_NUMBER, String.valueOf(nbFormsImported))
                        .put(PARAM_DUPLICATES_NUMBER, "-")
                        .put(PARAM_MAIN_RESOURCE_NAME, this.mainResourceName);
                log.error("[Formulaire@FormulaireRepositoryEvents::importTables] Failed to import data from file : " + err.getMessage());
                err.printStackTrace();
                handler.handle(finalResultInfos);
            });
    }


    // Deleted users and groups events

    @Override
    public void deleteGroups(JsonArray groups) {
        if (groups == null) {
            return;
        }

        for (int i = groups.size() - 1; i >= 0; i--) {
            if (groups.hasNull(i)) {
                groups.remove(i);
            }
        }

        if (groups.size() > 0) {
            final JsonArray groupsIds = new fr.wseduc.webutils.collections.JsonArray();
            for (Object o : groups) {
                if (o instanceof JsonObject) {
                    final JsonObject j = (JsonObject) o;
                    groupsIds.add(j.getString(ID));
                }
            }

            if (groupsIds.size() > 0) {
                SqlStatementsBuilder statementsBuilder = new SqlStatementsBuilder();

                // Delete groups from groups table (will delete their sharing rights by cascade)
                statementsBuilder.prepared("DELETE FROM " + GROUPS_TABLE + " WHERE id IN " + Sql.listPrepared(groupsIds), groupsIds);

                Sql.getInstance().transaction(statementsBuilder.build(), SqlResult.validRowsResultHandler(deleteEvent -> {
                    if (deleteEvent.isRight()) {
                        log.info("[Formulaire@FormulaireRepositoryEvents::deleteGroups] Sharing rights deleted for groups : " +
                                groupsIds.getList().toString());
                    }
                    else {
                        log.error("[Formulaire@FormulaireRepositoryEvents::deleteGroups] Failed to remove sharing rights deleted for groups (" +
                                groupsIds.getList().toString() + ") : " + deleteEvent.left().getValue());
                    }
                }));
            }
        }
    }

    @Override
    public void deleteUsers(JsonArray users) {
        if (users == null) {
            return;
        }

        for (int i = users.size() - 1; i >= 0; i--) {
            if (users.hasNull(i)) {
                users.remove(i);
            }
        }

        if (users.size() > 0){
            final JsonArray userIds = new fr.wseduc.webutils.collections.JsonArray();
            for (Object o : users) {
                if (o instanceof JsonObject) {
                    final JsonObject j = (JsonObject) o;
                    userIds.add(j.getString(ID));
                }
            }

            if (userIds.size() > 0) {
                SqlStatementsBuilder statementsBuilder = new SqlStatementsBuilder();

                // Delete forms on which no one else has manager rights (or is owner)
                String query =
                        "DELETE FROM " + FORM_TABLE + " WHERE id IN (" +
                            "SELECT id FROM " + FORM_TABLE + " f " +
                            "JOIN " + FORM_SHARES_TABLE + " fs ON fs.resource_id = f.id " +
                            "WHERE resource_id IN (" +
                                "SELECT id FROM " + FORM_TABLE +" f " +
                                "JOIN " + FORM_SHARES_TABLE + " fs ON fs.resource_id = f.id " +
                                "WHERE owner_id IN " + Sql.listPrepared(userIds) + " OR " +
                                "(action = ? AND member_id IN " + Sql.listPrepared(userIds) + ") " +
                                "GROUP BY id" +
                            ") AND resource_id NOT IN ( " +
                                "SELECT resource_id FROM " + FORM_SHARES_TABLE + " " +
                                "WHERE action = ? AND member_id NOT IN " + Sql.listPrepared(userIds) +
                            ") " +
                            "GROUP BY id" +
                        ");";
                JsonArray params = new JsonArray().addAll(userIds).add(MANAGER_RESOURCE_BEHAVIOUR)
                        .addAll(userIds).add(MANAGER_RESOURCE_BEHAVIOUR).addAll(userIds);
                statementsBuilder.prepared(query, params);

                // Delete users from members table (will delete their sharing rights by cascade)
                statementsBuilder.prepared("DELETE FROM " + MEMBERS_TABLE + " WHERE user_id IN " + Sql.listPrepared(userIds), userIds);

                // Set active distributions to false
                statementsBuilder.prepared("UPDATE " + DISTRIBUTION_TABLE + " SET active = ?" +
                        " WHERE responder_id IN " + Sql.listPrepared(userIds), new JsonArray().add(false).addAll(userIds));

                // Change responder_name to a fixed common default value in all his responses
                statementsBuilder.prepared("UPDATE " + DISTRIBUTION_TABLE + " SET responder_name = ? " +
                        "WHERE responder_id IN " + Sql.listPrepared(userIds), new JsonArray().add(DELETED_USER).addAll(userIds));

                // Change filename to a fixed common default value in all the response files' names of the users
                statementsBuilder.prepared("UPDATE " + RESPONSE_FILE_TABLE + " SET filename = ? " +
                        "WHERE response_id IN (" +
                            "SELECT id FROM " + RESPONSE_TABLE + " " +
                            "WHERE responder_id IN " + Sql.listPrepared(userIds) +
                        ")", new JsonArray().add(DELETED_USER_FILE).addAll(userIds));

                Sql.getInstance().transaction(statementsBuilder.build(), SqlResult.validRowsResultHandler(deleteEvent -> {
                    if (deleteEvent.isRight()) {
                        log.info("[Formulaire@FormulaireRepositoryEvents::deleteUsers] Sharing rights deleted for users : " +
                                userIds.getList().toString());
                    }
                    else {
                        log.error("[Formulaire@FormulaireRepositoryEvents::deleteUsers] Failed to remove sharing rights deleted for users (" +
                                userIds.getList().toString() + ") : " + deleteEvent.left().getValue());
                    }
                }));
            }
        }
    }
}
