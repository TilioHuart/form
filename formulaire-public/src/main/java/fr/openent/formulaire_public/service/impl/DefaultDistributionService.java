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

import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.CAPTCHA_TABLE;
import static fr.openent.form.core.constants.Tables.DISTRIBUTION_TABLE;

public class DefaultDistributionService implements DistributionService {

    @Override
    public void getDistributionByKey(String distributionKey, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + DISTRIBUTION_TABLE + " WHERE public_key = ?;";
        JsonArray params = new JsonArray().add(distributionKey);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void createDistribution(JsonObject form, Handler<Either<String, JsonObject>> handler) {
        String query = "WITH newCaptchaId AS (SELECT id FROM " + CAPTCHA_TABLE + " ORDER BY RANDOM() LIMIT 1) " +
                "INSERT INTO " + DISTRIBUTION_TABLE + " (form_id, sender_id, sender_name, responder_id, responder_name, " +
                "status, date_sending, active, public_key, captcha_id) " +
                "VALUES (?, ?, ?, '', '', ?, ?, ?, ?, (SELECT id FROM newCaptchaId)) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(form.getInteger(ID, null))
                .add(form.getString(OWNER_ID, ""))
                .add(form.getString(OWNER_NAME, ""))
                .add(DistributionStatus.TO_DO)
                .add("NOW()")
                .add(true)
                .add(UUID.randomUUID().toString());
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void updateCaptchaDistribution(String distributionKey, Handler<Either<String, JsonObject>> handler) {
        String query = "WITH oldCaptchaId AS (SELECT captcha_id FROM " + DISTRIBUTION_TABLE + " WHERE public_key = ?), " +
                "newCaptchaId AS (SELECT id FROM " + CAPTCHA_TABLE + " WHERE id != (SELECT captcha_id FROM oldCaptchaId) ORDER BY RANDOM() LIMIT 1) " +
                "UPDATE " + DISTRIBUTION_TABLE + " SET captcha_id = (SELECT id FROM newCaptchaId) WHERE public_key = ? RETURNING *;";
        JsonArray params = new JsonArray().add(distributionKey).add(distributionKey);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void finishDistribution(String distributionKey, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + DISTRIBUTION_TABLE + " SET status = ?, date_response = ?";
        JsonArray params = new JsonArray().add(DistributionStatus.FINISHED).add("NOW()");

        query += " WHERE public_key = ? RETURNING *;";
        params.add(distributionKey);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
