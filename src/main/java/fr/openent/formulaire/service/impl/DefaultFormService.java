package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.FormService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import java.util.List;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultFormService implements FormService {

    @Override
    public void list(List<String> groupsAndUserIds, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray params = new JsonArray();

        query.append("SELECT f.*, d.nb_responses, e.nb_elements, rff.folder_id ")
                .append("FROM ").append(Formulaire.FORM_TABLE).append(" f ")
                .append("LEFT JOIN ").append(Formulaire.FORM_SHARES_TABLE).append(" fs ON f.id = fs.resource_id ")
                .append("LEFT JOIN ").append(Formulaire.MEMBERS_TABLE).append(" m ON (fs.member_id = m.id AND m.group_id IS NOT NULL) ")
                .append("LEFT JOIN (SELECT form_id, COUNT(form_id) AS nb_responses FROM ").append(Formulaire.DISTRIBUTION_TABLE)
                .append(" WHERE status = ? GROUP BY form_id) d ON d.form_id = f.id ")
                .append("LEFT JOIN (SELECT form_id, COUNT(form_id) AS nb_elements FROM (SELECT id, form_id FROM ")
                .append(Formulaire.QUESTION_TABLE).append(" UNION SELECT id, form_id FROM ").append(Formulaire.SECTION_TABLE)
                .append(") AS e GROUP BY form_id) e ON e.form_id = f.id ")
                .append("LEFT JOIN ").append(Formulaire.REL_FORM_FOLDER_TABLE).append(" rff ON rff.form_id = f.id AND rff.user_id = f.owner_id");
        params.add(Formulaire.FINISHED);

        query.append(" WHERE (fs.member_id IN ").append(Sql.listPrepared(groupsAndUserIds.toArray()));
        for (String groupOrUser : groupsAndUserIds) {
            params.add(groupOrUser);
        }

        query.append(" AND (fs.action = ? OR fs.action = ?)) OR f.owner_id = ? ")
                .append("GROUP BY f.id, d.nb_responses, e.nb_elements, rff.folder_id ")
                .append("ORDER BY f.date_modification DESC;");
        params.add(Formulaire.MANAGER_RESOURCE_BEHAVIOUR).add(Formulaire.CONTRIB_RESOURCE_BEHAVIOUR).add(user.getUserId());

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listSentForms(UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT f.* FROM " + Formulaire.FORM_TABLE + " f " +
                "LEFT JOIN " + Formulaire.DISTRIBUTION_TABLE + " d ON f.id = d.form_id " +
                "WHERE d.responder_id = ? AND NOW() BETWEEN date_opening AND COALESCE(date_ending, NOW() + interval '1 year') " +
                "AND active = ? " +
                "GROUP BY f.id " +
                "ORDER BY title;";
        JsonArray params = new JsonArray().add(user.getUserId()).add(true);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listForLinker(List<String> groupsAndUserIds, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT f.* FROM " + Formulaire.FORM_TABLE + " f " +
                "LEFT JOIN " + Formulaire.FORM_SHARES_TABLE + " fs ON f.id = fs.resource_id " +
                "LEFT JOIN " + Formulaire.MEMBERS_TABLE + " m ON (fs.member_id = m.id AND m.group_id IS NOT NULL) " +
                "WHERE (fs.member_id IN " + Sql.listPrepared(groupsAndUserIds.toArray()) +
                " AND fs.action = ?) OR f.owner_id = ? " +
                "GROUP BY f.id " +
                "ORDER BY f.date_modification DESC;";

        JsonArray params = new JsonArray();
        for (String groupOrUser : groupsAndUserIds) {
            params.add(groupOrUser);
        }
        params.add(Formulaire.MANAGER_RESOURCE_BEHAVIOUR).add(user.getUserId());

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listContributors(String formId, Handler<Either<String, JsonArray>> handler) {
        listUsersByRights(formId, Formulaire.CONTRIB_RESOURCE_BEHAVIOUR, handler);
    }

    @Override
    public void listManagers(String formId, Handler<Either<String, JsonArray>> handler) {
        listUsersByRights(formId, Formulaire.MANAGER_RESOURCE_BEHAVIOUR, handler);
    }

    private void listUsersByRights(String formId, String right, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT fs.member_id AS id FROM " + Formulaire.FORM_TABLE + " f " +
                "JOIN " + Formulaire.FORM_SHARES_TABLE + " fs ON fs.resource_id = f.id " +
                "WHERE f.id = ? AND fs.action = ? " +
                "UNION " +
                "SELECT owner_id FROM "  + Formulaire.FORM_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(formId).add(right).add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String formId, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query =
                "WITH folder_id AS ( " +
                    "SELECT folder_id FROM " + Formulaire.REL_FORM_FOLDER_TABLE + " " +
                    "WHERE form_id = ? AND user_id = ? " +
                ") " +
                "SELECT *, (SELECT * FROM folder_id) " +
                "FROM " + Formulaire.FORM_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(formId).add(user.getUserId()).add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(JsonObject form, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Formulaire.FORM_TABLE + " (owner_id, owner_name, title, description, picture, " +
                "date_creation, date_modification, date_opening, date_ending, multiple, anonymous, response_notified, " +
                "editable, rgpd, rgpd_goal, rgpd_lifetime) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";
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
                .add(form.getInteger("rgpd_lifetime", 12));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void createMultiple(JsonArray forms, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "";
        JsonArray params = new JsonArray();

        List<JsonObject> allForms = forms.getList();
        for (JsonObject form : allForms) {
            query += "INSERT INTO " + Formulaire.FORM_TABLE + " (owner_id, owner_name, title, description, picture, " +
                    "date_creation, date_modification, date_opening, date_ending, multiple, anonymous, response_notified, " +
                    "editable, rgpd, rgpd_goal, rgpd_lifetime) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ";
            params.add(user.getUserId())
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
                    .add(form.getInteger("rgpd_lifetime", 12));
        }

        query += "RETURNING *;";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void duplicate(int formId, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query =
                "WITH new_form_id AS (" +
                    "INSERT INTO  " + Formulaire.FORM_TABLE + " (owner_id, owner_name, title, description, picture, " +
                    "date_opening, date_ending, multiple, anonymous, response_notified, editable, rgpd, rgpd_goal, rgpd_lifetime) " +
                    "SELECT ?, ?, concat(title, ' - Copie'), description, picture, date_opening, date_ending, multiple, " +
                    "anonymous, response_notified, editable, rgpd, rgpd_goal, rgpd_lifetime " +
                    "FROM " + Formulaire.FORM_TABLE + " WHERE id = ? RETURNING id" +
                "), " +
                "new_sections AS (" +
                    "INSERT INTO " + Formulaire.SECTION_TABLE + " (form_id, title, description, position, original_section_id) " +
                    "SELECT (SELECT id from new_form_id), title, description, position, id " +
                    "FROM " + Formulaire.SECTION_TABLE + " WHERE form_id = ? " +
                    "RETURNING id, form_id, original_section_id" +
                "), " +
                "new_sections_linked AS (" +
                    "SELECT ns.id, ns.original_section_id, q.id AS question_id, q.section_position FROM new_sections ns " +
                    "JOIN " + Formulaire.QUESTION_TABLE + " q ON ns.original_section_id = q.section_id" +
                "), " +
                "rows AS (" +
                    "INSERT INTO " + Formulaire.QUESTION_TABLE + " (form_id, title, position, question_type, statement, " +
                    "mandatory, original_question_id, section_id, section_position, conditional) " +
                    "SELECT (SELECT id from new_form_id), title, position, question_type, statement, mandatory, id, " +
                    "(SELECT id FROM new_sections_linked WHERE original_section_id = q.section_id LIMIT 1), " +
                    "(SELECT section_position FROM new_sections_linked WHERE question_id = q.id), conditional " +
                    "FROM " + Formulaire.QUESTION_TABLE + " q WHERE form_id = ? " +
                    "ORDER BY q.id " +
                    "RETURNING id, form_id, original_question_id, question_type" +
                ") " +
                "SELECT * FROM rows " +
                "UNION ALL " +
                "SELECT (SELECT id FROM new_form_id), null, null, null " +
                "WHERE NOT EXISTS (SELECT * FROM rows)" +
                "ORDER BY id;";
        JsonArray params = new JsonArray().add(user.getUserId()).add(user.getUsername()).add(formId).add(formId).add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void update(String formId, JsonObject form, Handler<Either<String, JsonObject>> handler) {
        String query = "WITH nbResponses AS (SELECT COUNT(*) FROM " + Formulaire.DISTRIBUTION_TABLE +
                " WHERE form_id = ? AND status = ?) " +
                "UPDATE " + Formulaire.FORM_TABLE + " SET title = ?, description = ?, picture = ?, date_modification = ?, " +
                "date_opening = ?, date_ending = ?, sent = ?, collab = ?, reminded = ?, archived = ?, " +
                "multiple = CASE (SELECT count > 0 FROM nbResponses) " +
                "WHEN false THEN ? WHEN true THEN (SELECT multiple FROM " + Formulaire.FORM_TABLE +" WHERE id = ?) END, " +
                "anonymous = CASE (SELECT count > 0 FROM nbResponses) " +
                "WHEN false THEN ? WHEN true THEN (SELECT anonymous FROM " + Formulaire.FORM_TABLE +" WHERE id = ?) END, " +
                "response_notified = ?, editable = ?, rgpd = ?, rgpd_goal = ?, rgpd_lifetime = ?" +
                "WHERE id = ? RETURNING *;";

        JsonArray params = new JsonArray()
                .add(formId)
                .add(Formulaire.FINISHED)
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
        String query = "DELETE FROM " + Formulaire.FORM_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getMyFormRights(String formId, List<String> groupsAndUserIds, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT action FROM " + Formulaire.FORM_SHARES_TABLE +
                " WHERE resource_id = ? AND member_id IN " + Sql.listPrepared(groupsAndUserIds) + " AND action IN (?, ?, ?);";
        JsonArray params = new JsonArray()
                .add(formId)
                .addAll(new fr.wseduc.webutils.collections.JsonArray(groupsAndUserIds))
                .add(Formulaire.CONTRIB_RESOURCE_BEHAVIOUR)
                .add(Formulaire.MANAGER_RESOURCE_BEHAVIOUR)
                .add(Formulaire.RESPONDER_RESOURCE_BEHAVIOUR);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getAllMyFormRights(List<String> groupsAndUserIds, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT resource_id, action FROM " + Formulaire.FORM_SHARES_TABLE +
                " WHERE member_id IN " + Sql.listPrepared(groupsAndUserIds) + " AND action IN (?, ?, ?);";
        JsonArray params = new JsonArray()
                .addAll(new fr.wseduc.webutils.collections.JsonArray(groupsAndUserIds))
                .add(Formulaire.CONTRIB_RESOURCE_BEHAVIOUR)
                .add(Formulaire.MANAGER_RESOURCE_BEHAVIOUR)
                .add(Formulaire.RESPONDER_RESOURCE_BEHAVIOUR);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getImage(EventBus eb, String idImage, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject().put("action", "getDocument").put("id", idImage);
        String WORKSPACE_BUS_ADDRESS = "org.entcore.workspace";
        eb.send(WORKSPACE_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            if (idImage.equals("")) {
                handler.handle(new Either.Left<>("[DefaultFormService@getImage] An error id image"));
            } else {
                handler.handle(new Either.Right<>(message.body().getJsonObject("result")));
            }
        }));
    }
}
