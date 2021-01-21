package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.DistributionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class DefaultDistributionService implements DistributionService {

    @Override
    public void list(UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE + " ORDER BY date_sending DESC;";
        JsonArray params = new JsonArray().add(user.getUserId());
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String id, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(id);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(String formId, UserInfos user, Handler<Either<String, JsonObject>> handler) {
//        String query = "INSERT INTO " + Formulaire.DISTRIBUTION_TABLE + " (form_id, sender_id, sender_name, " +
//                "respondent_id, respondent_name, status, date_sending) " +
//                " VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING *;";
//        JsonArray params = new JsonArray()
//                .add(formId)
//                .add(user.getUserId())
//                .add(user.getUsername())
//                .add(respondents[i].getString("id", ""))
//                .add(respondents[i].getString("name", ""))
//                .add(Formulaire.TO_DO)
//                .add("NOW()");
        String query = "";
        JsonArray params = new JsonArray();

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void update(String id, JsonObject distribution, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Formulaire.DISTRIBUTION_TABLE + " SET status = " + distribution.getString("status");
        if (distribution.getString("status") == Formulaire.FINISHED) { query += ", date_response = NOW() "; }
        query += "WHERE id = ?;";

        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(String id, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(id);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
