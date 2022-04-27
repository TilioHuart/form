package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.POCService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.entcore.common.user.UserInfos;

import java.text.Normalizer;
import java.util.ArrayList;

public class DefaultPOCService implements POCService {
    @Override
    public void getFormByKey(String formKey, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.FORM_TABLE + " WHERE id = ?;"; // TODO 'key' instead of 'id'
        JsonArray params = new JsonArray().add(formKey);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void createDistribution(JsonObject form, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Formulaire.DISTRIBUTION_TABLE + " (form_id, sender_id, sender_name, " +
                "responder_id, responder_name, status, date_sending, active) " +
                " VALUES (?, ?, ?, '', '', ?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(form.getInteger("id", null))
                .add(form.getString("owner_id", ""))
                .add(form.getString("owner_name", ""))
                .add(Formulaire.TO_DO)
                .add("NOW()")
                .add(true);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getDistributionByKey(String distributionKey, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE id = ?;"; // TODO 'key' instead of 'id'
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

        query += " WHERE id = ? RETURNING *;"; // TODO 'key' instead of 'id'
        params.add(distributionKey);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
