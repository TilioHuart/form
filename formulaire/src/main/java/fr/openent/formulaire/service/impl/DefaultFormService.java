package fr.openent.formulaire.service.impl;

import fr.openent.form.core.constants.Tables;
import fr.openent.formulaire.service.FormService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.entcore.common.user.UserInfos;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static fr.openent.form.core.constants.DistributionStatus.FINISHED;
import static fr.openent.form.core.constants.ShareRights.*;

public class DefaultFormService implements FormService {
    private final Sql sql = Sql.getInstance();
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Override
    public void list(List<String> groupsAndUserIds, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray params = new JsonArray();

        query.append("SELECT f.*, d.nb_responses, e.nb_elements, rff.folder_id ")
                .append("FROM ").append(Tables.FORM).append(" f ")
                .append("LEFT JOIN ").append(Tables.FORM_SHARES).append(" fs ON f.id = fs.resource_id ")
                .append("LEFT JOIN ").append(Tables.MEMBERS).append(" m ON (fs.member_id = m.id AND m.group_id IS NOT NULL) ")
                .append("LEFT JOIN (SELECT form_id, COUNT(form_id) AS nb_responses FROM ").append(Tables.DISTRIBUTION)
                .append(" WHERE status = ? GROUP BY form_id) d ON d.form_id = f.id ")
                .append("LEFT JOIN (SELECT form_id, COUNT(form_id) AS nb_elements FROM (SELECT id, form_id FROM ")
                .append(Tables.QUESTION).append(" UNION SELECT id, form_id FROM ").append(Tables.SECTION)
                .append(") AS e GROUP BY form_id) e ON e.form_id = f.id ")
                .append("LEFT JOIN ").append(Tables.REL_FORM_FOLDER).append(" rff ON rff.form_id = f.id AND rff.user_id = f.owner_id");
        params.add(FINISHED);

        query.append(" WHERE (fs.member_id IN ").append(Sql.listPrepared(groupsAndUserIds.toArray()));
        for (String groupOrUser : groupsAndUserIds) {
            params.add(groupOrUser);
        }

        query.append(" AND (fs.action = ? OR fs.action = ?)) OR f.owner_id = ? ")
                .append("GROUP BY f.id, d.nb_responses, e.nb_elements, rff.folder_id ")
                .append("ORDER BY f.date_modification DESC;");
        params.add(MANAGER_RESOURCE_BEHAVIOUR).add(CONTRIB_RESOURCE_BEHAVIOUR).add(user.getUserId());

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listSentForms(UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT f.* FROM " + Tables.FORM + " f " +
                "LEFT JOIN " + Tables.DISTRIBUTION + " d ON f.id = d.form_id " +
                "WHERE d.responder_id = ? AND NOW() BETWEEN date_opening AND COALESCE(date_ending, NOW() + interval '1 year') " +
                "AND active = ? " +
                "GROUP BY f.id " +
                "ORDER BY title;";
        JsonArray params = new JsonArray().add(user.getUserId()).add(true);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listForLinker(List<String> groupsAndUserIds, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT f.* FROM " + Tables.FORM + " f " +
                "LEFT JOIN " + Tables.FORM_SHARES + " fs ON f.id = fs.resource_id " +
                "LEFT JOIN " + Tables.MEMBERS + " m ON (fs.member_id = m.id AND m.group_id IS NOT NULL) " +
                "WHERE f.archived = ? AND f.sent = ? AND ((fs.member_id IN " + Sql.listPrepared(groupsAndUserIds.toArray()) +
                " AND fs.action = ?) OR f.owner_id = ? )" +
                "GROUP BY f.id " +
                "ORDER BY f.date_modification DESC;";

        JsonArray params = new JsonArray().add(false).add(true);
        for (String groupOrUser : groupsAndUserIds) {
            params.add(groupOrUser);
        }
        params.add(MANAGER_RESOURCE_BEHAVIOUR).add(user.getUserId());

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listContributors(String formId, Handler<Either<String, JsonArray>> handler) {
        listUsersByRights(formId, CONTRIB_RESOURCE_BEHAVIOUR, handler);
    }

    @Override
    public void listManagers(String formId, Handler<Either<String, JsonArray>> handler) {
        listUsersByRights(formId, MANAGER_RESOURCE_BEHAVIOUR, handler);
    }

    private void listUsersByRights(String formId, String right, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT fs.member_id AS id FROM " + Tables.FORM + " f " +
                "JOIN " + Tables.FORM_SHARES + " fs ON fs.resource_id = f.id " +
                "WHERE f.id = ? AND fs.action = ? " +
                "UNION " +
                "SELECT owner_id FROM "  + Tables.FORM + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(formId).add(right).add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String formId, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query =
                "WITH folder_id AS ( " +
                    "SELECT folder_id FROM " + Tables.REL_FORM_FOLDER + " " +
                    "WHERE form_id = ? AND user_id = ? " +
                ") " +
                "SELECT *, (SELECT * FROM folder_id) " +
                "FROM " + Tables.FORM + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(formId).add(user.getUserId()).add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(JsonObject form, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String public_key = null;
        if (form.getBoolean("is_public", false)) {
            if (form.getString("date_ending", null) == null || form.getString("date_opening", null) == null) {
                handler.handle(new Either.Left<>("A public form must have an ending date."));
            }
            else {
                try {
                    Date startDate = dateFormatter.parse(form.getString("date_opening"));
                    Date endDate = dateFormatter.parse(form.getString("date_ending"));
                    if (endDate.after(new Date()) && endDate.after(startDate)) {
                        public_key = UUID.randomUUID().toString();
                    }
                    else {
                        handler.handle(new Either.Left<>("This form is closed, you cannot access it anymore."));
                    }
                }
                catch (ParseException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        String query = "INSERT INTO " + Tables.FORM + " (owner_id, owner_name, title, description, picture, " +
                "date_creation, date_modification, date_opening, date_ending, multiple, anonymous, response_notified, " +
                "editable, rgpd, rgpd_goal, rgpd_lifetime, is_public" + (public_key != null ? ", public_key" : "") + ") " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" + (public_key != null ? ", ?" : "") + ") RETURNING *;";
        JsonArray params = new JsonArray()
                .add(user.getUserId())
                .add(user.getUsername())
                .add(form.getString("title", ""))
                .add(form.getString("description", ""))
                .add(form.getString("picture", ""))
                .add("NOW()").add("NOW()")
                .add(form.getString("date_opening", "NOW()"))
                .add(form.getString("date_ending", null))
                .add(form.getBoolean("multiple", false))
                .add(form.getBoolean("anonymous", false))
                .add(form.getBoolean("response_notified", false))
                .add(form.getBoolean("editable", false))
                .add(form.getBoolean("rgpd", false))
                .add(form.getString("rgpd_goal", ""))
                .add(form.getInteger("rgpd_lifetime", 12))
                .add(form.getBoolean("is_public", false));

        if (public_key != null) params.add(public_key);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void createMultiple(JsonArray forms, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        if (!forms.isEmpty()) {
            SqlStatementsBuilder s = new SqlStatementsBuilder();

            s.raw("BEGIN;");
            for (int i = 0; i < forms.size(); i++) {
                JsonObject form = forms.getJsonObject(i);
                String public_key = null;
                if (form.getBoolean("is_public", false)) {
                    if (form.getString("date_ending", null) == null || form.getString("date_opening", null) == null) {
                        handler.handle(new Either.Left<>("A public form must have an ending date."));
                    }
                    else {
                        try {
                            Date startDate = dateFormatter.parse(form.getString("date_opening"));
                            Date endDate = dateFormatter.parse(form.getString("date_ending"));
                            if (endDate.after(new Date()) && endDate.after(startDate)) {
                                public_key = UUID.randomUUID().toString();
                            }
                            else {
                                handler.handle(new Either.Left<>("This form is closed, you cannot access it anymore."));
                            }
                        }
                        catch (ParseException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                }

                String query = "INSERT INTO " + Tables.FORM + " (owner_id, owner_name, title, description, picture, " +
                        "date_creation, date_modification, date_opening, date_ending, multiple, anonymous, response_notified, " +
                        "editable, rgpd, rgpd_goal, rgpd_lifetime, is_public" + (public_key != null ? ", public_key" : "") + ") " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" + (public_key != null ? ", ?" : "") + "); ";
                JsonArray params = new JsonArray()
                        .add(user.getUserId())
                        .add(user.getUsername())
                        .add(form.getString("title", ""))
                        .add(form.getString("description", ""))
                        .add(form.getString("picture", ""))
                        .add("NOW()").add("NOW()")
                        .add(form.getString("date_opening", "NOW()"))
                        .add(form.getString("date_ending", null))
                        .add(form.getBoolean("multiple", false))
                        .add(form.getBoolean("anonymous", false))
                        .add(form.getBoolean("response_notified", false))
                        .add(form.getBoolean("editable", false))
                        .add(form.getBoolean("rgpd", false))
                        .add(form.getString("rgpd_goal", ""))
                        .add(form.getInteger("rgpd_lifetime", 12))
                        .add(form.getBoolean("is_public", false));

                if (public_key != null) params.add(public_key);

                s.prepared(query, params);
            }
            s.raw("COMMIT;");

            sql.transaction(s.build(), SqlResult.validResultsHandler(handler));
        }
        else {
            handler.handle(new Either.Right<>(new JsonArray()));
        }
    }

    @Override
    public void duplicate(int formId, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query =
                "WITH new_form_id AS (" +
                    "INSERT INTO  " + Tables.FORM + " (owner_id, owner_name, title, description, picture, " +
                    "date_opening, date_ending, multiple, anonymous, response_notified, editable, rgpd, rgpd_goal, rgpd_lifetime, is_public, public_key) " +
                    "SELECT ?, ?, concat(title, ' - Copie'), description, picture, date_opening, date_ending, multiple, " +
                    "anonymous, response_notified, editable, rgpd, rgpd_goal, rgpd_lifetime, is_public, CASE is_public WHEN TRUE THEN '" + UUID.randomUUID() + "' END " +
                    "FROM " + Tables.FORM + " WHERE id = ? RETURNING id" +
                "), " +
                "new_sections AS (" +
                    "INSERT INTO " + Tables.SECTION + " (form_id, title, description, position, original_section_id) " +
                    "SELECT (SELECT id from new_form_id), title, description, position, id " +
                    "FROM " + Tables.SECTION + " WHERE form_id = ? " +
                    "RETURNING id, form_id, original_section_id" +
                "), " +
                "new_sections_linked AS (" +
                    "SELECT ns.id, ns.original_section_id, q.id AS question_id, q.section_position FROM new_sections ns " +
                    "JOIN " + Tables.QUESTION + " q ON ns.original_section_id = q.section_id" +
                "), " +
                "rows AS (" +
                    "INSERT INTO " + Tables.QUESTION + " (form_id, title, position, question_type, statement, " +
                    "mandatory, original_question_id, section_id, section_position, conditional) " +
                    "SELECT (SELECT id from new_form_id), title, position, question_type, statement, mandatory, id, " +
                    "(SELECT id FROM new_sections_linked WHERE original_section_id = q.section_id LIMIT 1), " +
                    "(SELECT section_position FROM new_sections_linked WHERE question_id = q.id), conditional " +
                    "FROM " + Tables.QUESTION + " q WHERE form_id = ? " +
                    "ORDER BY q.id " +
                    "RETURNING id, form_id, original_question_id, question_type" +
                ") " +
                "SELECT * FROM rows " +
                "UNION ALL " +
                "SELECT null, (SELECT id FROM new_form_id), null, null " +
                "WHERE NOT EXISTS (SELECT * FROM rows)" +
                "ORDER BY id;";

        JsonArray params = new JsonArray()
                .add(user.getUserId())
                .add(user.getUsername())
                .add(formId)
                .add(formId)
                .add(formId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void update(String formId, JsonObject form, Handler<Either<String, JsonObject>> handler) {
        String query = "WITH nbResponses AS (SELECT COUNT(*) FROM " + Tables.DISTRIBUTION +
                " WHERE form_id = ? AND status = ?) " +
                "UPDATE " + Tables.FORM + " SET title = ?, description = ?, picture = ?, date_modification = ?, " +
                "date_opening = ?, date_ending = ?, sent = ?, collab = ?, reminded = ?, archived = ?, " +
                "multiple = CASE (SELECT count > 0 FROM nbResponses) " +
                "WHEN false THEN ? WHEN true THEN (SELECT multiple FROM " + Tables.FORM +" WHERE id = ?) END, " +
                "anonymous = CASE (SELECT count > 0 FROM nbResponses) " +
                "WHEN false THEN ? WHEN true THEN (SELECT anonymous FROM " + Tables.FORM +" WHERE id = ?) END, " +
                "response_notified = ?, editable = ?, rgpd = ?, rgpd_goal = ?, rgpd_lifetime = ?" +
                "WHERE id = ? RETURNING *;";

        JsonArray params = new JsonArray()
                .add(formId)
                .add(FINISHED)
                .add(form.getString("title", ""))
                .add(form.getString("description", ""))
                .add(form.getString("picture", ""))
                .add("NOW()")
                .add(form.getString("date_opening", "NOW()"))
                .add(form.getString("date_ending", null))
                .add(form.getBoolean("sent", false))
                .add(form.getBoolean("collab", false))
                .add(form.getBoolean("reminded", false))
                .add(form.getBoolean("archived", false))
                .add(form.getBoolean("multiple", false)).add(formId)
                .add(form.getBoolean("anonymous", false)).add(formId)
                .add(form.getBoolean("response_notified", false))
                .add(form.getBoolean("editable", false))
                .add(form.getBoolean("rgpd", false))
                .add(form.getString("rgpd_goal", ""))
                .add(form.getInteger("rgpd_lifetime", 12))
                .add(formId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(String formId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Tables.FORM + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getMyFormRights(String formId, List<String> groupsAndUserIds, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT action FROM " + Tables.FORM_SHARES +
                " WHERE resource_id = ? AND member_id IN " + Sql.listPrepared(groupsAndUserIds) + " AND action IN (?, ?, ?);";
        JsonArray params = new JsonArray()
                .add(formId)
                .addAll(new fr.wseduc.webutils.collections.JsonArray(groupsAndUserIds))
                .add(CONTRIB_RESOURCE_BEHAVIOUR)
                .add(MANAGER_RESOURCE_BEHAVIOUR)
                .add(RESPONDER_RESOURCE_BEHAVIOUR);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getAllMyFormRights(List<String> groupsAndUserIds, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT resource_id, action FROM " + Tables.FORM_SHARES +
                " WHERE member_id IN " + Sql.listPrepared(groupsAndUserIds) + " AND action IN (?, ?, ?);";
        JsonArray params = new JsonArray()
                .addAll(new fr.wseduc.webutils.collections.JsonArray(groupsAndUserIds))
                .add(CONTRIB_RESOURCE_BEHAVIOUR)
                .add(MANAGER_RESOURCE_BEHAVIOUR)
                .add(RESPONDER_RESOURCE_BEHAVIOUR);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void checkFormsRights(List<String> groupsAndUserIds, UserInfos user, String right, JsonArray formIds, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT COUNT(DISTINCT f.id) FROM " + Tables.FORM + " f " +
                "LEFT JOIN " + Tables.FORM_SHARES + " fs ON fs.resource_id = f.id " +
                "WHERE ((member_id IN " + Sql.listPrepared(groupsAndUserIds) + " AND action = ?) OR owner_id = ?) AND id IN " + Sql.listPrepared(formIds);
        JsonArray params = (new fr.wseduc.webutils.collections.JsonArray(groupsAndUserIds))
                .add(right)
                .add(user.getUserId())
                .addAll(formIds);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
