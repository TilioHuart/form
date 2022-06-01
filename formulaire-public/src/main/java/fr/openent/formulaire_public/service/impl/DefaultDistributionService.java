package fr.openent.formulaire_public.service.impl;

import fr.openent.form.core.constants.DistributionStatus;
import fr.openent.form.core.constants.Tables;
import fr.openent.formulaire_public.service.DistributionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.UUID;

public class DefaultDistributionService implements DistributionService {

    @Override
    public void getDistributionByKey(String distributionKey, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Tables.DISTRIBUTION + " WHERE public_key = ?;";
        JsonArray params = new JsonArray().add(distributionKey);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void createDistribution(JsonObject form, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Tables.DISTRIBUTION + " (form_id, sender_id, sender_name, " +
                "responder_id, responder_name, status, date_sending, active, public_key) " +
                " VALUES (?, ?, ?, '', '', ?, ?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(form.getInteger("id", null))
                .add(form.getString("owner_id", ""))
                .add(form.getString("owner_name", ""))
                .add(DistributionStatus.TO_DO)
                .add("NOW()")
                .add(true)
                .add(UUID.randomUUID().toString());
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void finishDistribution(String distributionKey, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Tables.DISTRIBUTION + " SET status = ?, date_response = ?";
        JsonArray params = new JsonArray().add(DistributionStatus.FINISHED).add("NOW()");

        query += " WHERE public_key = ? RETURNING *;";
        params.add(distributionKey);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
