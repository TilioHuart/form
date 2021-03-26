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

import java.util.ArrayList;

public class DefaultDistributionService implements DistributionService {

    @Override
    public void listBySender(UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE sender_id = ? " +
                "ORDER BY date_sending DESC;";
        JsonArray params = new JsonArray().add(user.getUserId());
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listByForm(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE form_id = ? " +
                "ORDER BY date_sending DESC;";
        JsonArray params = new JsonArray().add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String formId, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE form_id = ? AND responder_id = ?;";
        JsonArray params = new JsonArray().add(formId).add(user.getUserId());
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void count(String formId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT COUNT(*) FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE form_id = ? AND status = ?;";
        JsonArray params = new JsonArray().add(formId).add(Formulaire.FINISHED);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(String formId, UserInfos user, JsonArray responders, Handler<Either<String, JsonObject>> handler) {
        ArrayList<JsonObject> respondersArray = new ArrayList<>();
        if (responders != null) {
            for (int i=0; i < responders.size(); i++){
                respondersArray.add(responders.getJsonObject(i));
            }
        }
        for (JsonObject responder : respondersArray) {
            String query = "INSERT INTO " + Formulaire.DISTRIBUTION_TABLE + " (form_id, sender_id, sender_name, " +
                    "responder_id, responder_name, status, date_sending) " +
                    " VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING *;";
            JsonArray params = new JsonArray()
                    .add(formId)
                    .add(user.getUserId())
                    .add(user.getUsername())
                    .add(responder.getString("id", ""))
                    .add(responder.getString("name", ""))
                    .add(Formulaire.TO_DO)
                    .add("NOW()");
            Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
        }
    }

    @Override
    public void update(String distributionId, JsonObject distribution, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Formulaire.DISTRIBUTION_TABLE + " SET status = '"
                + distribution.getString("status") + "' ";
        if (distribution.getString("status").equals(Formulaire.FINISHED)) { query += ", date_response = NOW() "; }
        query += "WHERE id = " + distributionId + " RETURNING *;";

        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(String distributionId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(distributionId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getDuplicates(String formId, JsonArray responders, Handler<Either<String, JsonArray>> handler) {
        ArrayList<String> respondersIdsArray = new ArrayList<>();
        for (int i = 0; i < responders.size(); i++) {
            JsonArray users = responders.getJsonObject(i).getJsonArray("users");
            for (int j = 0; j < users.size(); j++) {
                respondersIdsArray.add(users.getJsonObject(j).getString("id"));
            }
        }

        JsonArray params = new JsonArray().add(formId);
        String query = "SELECT responder_id FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE form_id = ? ";

        if (respondersIdsArray.size() > 0) {
            query += "AND responder_id IN (";
            for (String id : respondersIdsArray) {
                query += "?, ";
                params.add(id);
            }
            query = query.substring(0, query.length() - 2) + ");";
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getRemoved(String formId, JsonArray responders, Handler<Either<String, JsonArray>> handler) {
        ArrayList<String> respondersIdsArray = new ArrayList<>();
        for (int i = 0; i < responders.size(); i++) {
            JsonArray users = responders.getJsonObject(i).getJsonArray("users");
            for (int j = 0; j < users.size(); j++) {
                respondersIdsArray.add(users.getJsonObject(j).getString("id"));
            }
        }

        JsonArray params = new JsonArray().add(formId);
        String query = "SELECT responder_id FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE form_id = ? ";

        if (respondersIdsArray.size() > 0) {
            query += "AND responder_id NOT IN (";
            for (String id : respondersIdsArray) {
                query += "?, ";
                params.add(id);
            }
            query = query.substring(0, query.length() - 2) + ");";
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void createMultiple(String formId, UserInfos user, JsonArray responders, JsonArray duplicates, Handler<Either<String, JsonObject>> handler) {
        ArrayList<String> idsToFilter = new ArrayList<>();
        if (duplicates != null && !duplicates.isEmpty()) {
            for (int i = 0; i < duplicates.size(); i++) {
                idsToFilter.add(duplicates.getJsonObject(i).getString("responder_id"));
            }
        }

        ArrayList<JsonObject> respondersArray = new ArrayList<>();
        for (int i = 0; i < responders.size(); i++) {
            JsonArray infos = responders.getJsonObject(i).getJsonArray("users");
            if (infos != null && !infos.isEmpty()) {
                for (int j = 0; j < infos.size(); j++) {
                    JsonObject info = infos.getJsonObject(j);
                    if (!idsToFilter.contains(info.getString("id"))) {
                        respondersArray.add(info);
                    }
                }
            }
        }

        if (!respondersArray.isEmpty()) {
            JsonArray params = new JsonArray();
            String query = "INSERT INTO " + Formulaire.DISTRIBUTION_TABLE + " (form_id, sender_id, sender_name, responder_id, " +
                    "responder_name, status, date_sending) VALUES ";

            for (JsonObject responder : respondersArray) {
                query += "(?, ?, ?, ?, ?, ?, ?), ";
                params.add(formId)
                        .add(user.getUserId())
                        .add(user.getUsername())
                        .add(responder.getString("id", ""))
                        .add(responder.getString("username", ""))
                        .add(Formulaire.TO_DO)
                        .add("NOW()");
            }

            query = query.substring(0, query.length() - 2) + ";";
            Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
        }
        else {
            handler.handle(new Either.Right<>(new JsonObject()));
        }
    }

    @Override
    public void removeMultiple(String formId, JsonArray removed, Handler<Either<String, JsonObject>> handler) {
        ArrayList<String> idsToRemove = new ArrayList<>();
        if (removed != null && !removed.isEmpty()) {
            for (int i=0; i < removed.size(); i++) {
                idsToRemove.add(removed.getJsonObject(i).getString("responder_id"));
            }
        }

        JsonArray params = new JsonArray().add(formId);
        String query = "DELETE FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE form_id = ? ";

        if (idsToRemove.size() > 0) {
            query += "AND responder_id IN (";
            for (String id : idsToRemove) {
                query += "?, ";
                params.add(id);
            }
            query = query.substring(0, query.length() - 2) + ");";
        }
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
