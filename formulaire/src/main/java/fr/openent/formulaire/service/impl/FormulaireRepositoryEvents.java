package fr.openent.formulaire.service.impl;

import fr.openent.form.core.constants.Tables;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.entcore.common.user.RepositoryEvents;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.openent.form.core.constants.Constants.DELETED_USER;
import static fr.openent.form.core.constants.Constants.DELETED_USER_FILE;
import static fr.openent.form.core.constants.Fields.ID;
import static fr.openent.form.core.constants.ShareRights.MANAGER_RESOURCE_BEHAVIOUR;
import static fr.openent.form.core.constants.Tables.*;

public class FormulaireRepositoryEvents implements RepositoryEvents {
    private static final Logger log = LoggerFactory.getLogger(FormulaireRepositoryEvents.class);

    @Override
    public void exportResources(JsonArray resourcesIds, boolean exportDocuments, boolean exportSharedResources, String exportId, String userId, JsonArray groups, String exportPath, String locale, String host, final Handler<Boolean> handler) {
        log.info("[Formulaire@FormulaireRepositoryEvents] exportResources event is not implemented");
    }

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
                        log.info("[Formulaire@FormulaireRepositoryEvents] Sharing rights deleted for groups : " +
                                groupsIds.getList().toString());
                    }
                    else {
                        log.error("[Formulaire@FormulaireRepositoryEvents] Failed to remove sharing rights deleted for groups (" +
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
                        log.info("[Formulaire@FormulaireRepositoryEvents] Sharing rights deleted for users : " +
                                userIds.getList().toString());
                    }
                    else {
                        log.error("[Formulaire@FormulaireRepositoryEvents] Failed to remove sharing rights deleted for users (" +
                                userIds.getList().toString() + ") : " + deleteEvent.left().getValue());
                    }
                }));
            }
        }
    }
}
