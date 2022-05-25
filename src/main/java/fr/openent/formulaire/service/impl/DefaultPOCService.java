package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.POCService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;

import java.util.ArrayList;
import java.util.UUID;

public class DefaultPOCService implements POCService {
    private final TimelineHelper timelineHelper;

    public DefaultPOCService(TimelineHelper timelineHelper){
        this.timelineHelper = timelineHelper;
    }

    @Override
    public void getFormByKey(String formKey, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.FORM_TABLE + " WHERE public_key = ?;";
        JsonArray params = new JsonArray().add(formKey);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void createDistribution(JsonObject form, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Formulaire.DISTRIBUTION_TABLE + " (form_id, sender_id, sender_name, " +
                "responder_id, responder_name, status, date_sending, active, public_key) " +
                " VALUES (?, ?, ?, '', '', ?, ?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(form.getInteger("id", null))
                .add(form.getString("owner_id", ""))
                .add(form.getString("owner_name", ""))
                .add(Formulaire.TO_DO)
                .add("NOW()")
                .add(true)
                .add(UUID.randomUUID().toString());
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getDistributionByKey(String distributionKey, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE public_key = ?;";
        JsonArray params = new JsonArray().add(distributionKey);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void createResponses(JsonArray responses, JsonObject distribution, Handler<Either<String, JsonArray>> handler) {
        ArrayList<JsonObject> responsesList = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            if (responses.getJsonObject(i) != null) {
                JsonObject response = responses.getJsonObject(i);
                responsesList.add(response);
            }
        }

        if (!responsesList.isEmpty()) {
            SqlStatementsBuilder s = new SqlStatementsBuilder();
            String query = "INSERT INTO " + Formulaire.RESPONSE_TABLE + " (question_id, choice_id, answer, responder_id, distribution_id) " +
                    "VALUES (?, ?, ?, ?, ?);";

            s.raw("BEGIN;");
            for (JsonObject response : responsesList) {
                JsonArray params = new JsonArray()
                        .add(response.getInteger("question_id", null))
                        .add(response.getInteger("choice_id", null))
                        .add(response.getString("answer", ""))
                        .add("")
                        .add(distribution.getInteger("id", null));
                s.prepared(query, params);
            }
            s.raw("COMMIT;");

            Sql.getInstance().transaction(s.build(), SqlResult.validResultHandler(handler));
        }
        else {
            handler.handle(new Either.Right<>(new JsonArray()));
        }
    }

    @Override
    public void finishDistribution(String distributionKey, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Formulaire.DISTRIBUTION_TABLE + " SET status = ?, date_response = ?";
        JsonArray params = new JsonArray().add(Formulaire.FINISHED).add("NOW()");

        query += " WHERE public_key = ? RETURNING *;";
        params.add(distributionKey);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void listManagers(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT fs.member_id AS id FROM " + Formulaire.FORM_TABLE + " f " +
                "JOIN " + Formulaire.FORM_SHARES_TABLE + " fs ON fs.resource_id = f.id " +
                "WHERE f.id = ? AND fs.action = ? " +
                "UNION " +
                "SELECT owner_id FROM "  + Formulaire.FORM_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(formId).add(Formulaire.MANAGER_RESOURCE_BEHAVIOUR).add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void notifyResponse(HttpServerRequest request, JsonObject form, JsonArray managers) {
        JsonObject params = new JsonObject()
                .put("anonymous", form.getBoolean("anonymous"))
                .put("formUri", "/formulaire#/form/" + form.getInteger("id") + "/edit")
                .put("formName", form.getString("title"))
                .put("formResultsUri", "/formulaire#/form/" + form.getInteger("id") + "/results/1")
                .put("pushNotif", new JsonObject().put("title", "push.notif.formulaire.response").put("body", ""));

        timelineHelper.notifyTimeline(request, "formulaire.response_notification", null, managers.getList(), params);
    }
}
