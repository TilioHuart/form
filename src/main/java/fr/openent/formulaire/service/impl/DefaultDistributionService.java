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
    public void list(UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE sender_id = ? " +
                "ORDER BY date_sending DESC;";
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
    public void update(String id, JsonObject distribution, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Formulaire.DISTRIBUTION_TABLE + " SET status = '"
                + distribution.getString("status") + "' ";
        if (distribution.getString("status").equals(Formulaire.FINISHED)) { query += ", date_response = NOW() "; }
        query += "WHERE id = " + id + " ;";

        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(String id, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(id);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getDuplicates(String formId, JsonArray responders, Handler<Either<String, JsonArray>> handler) {
        ArrayList<String> respondersIdsArray = new ArrayList<>();
        if (responders != null) {
            for (int i=0; i < responders.size(); i++){
                respondersIdsArray.add(responders.getJsonObject(i).getJsonArray("users").getJsonObject(0).getString("id"));
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
        if (responders != null) {
            for (int i=0; i < responders.size(); i++){
                respondersIdsArray.add(responders.getJsonObject(i).getJsonArray("users").getJsonObject(0).getString("id"));
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
        if (duplicates != null) {
            for (int i=0; i < duplicates.size(); i++) {
                idsToFilter.add(duplicates.getJsonObject(i).getString("responder_id"));
            }
        }

        ArrayList<JsonObject> respondersArray = new ArrayList<>();
        if (responders != null) {
            for (int i=0; i < responders.size(); i++) {
                JsonObject info = responders.getJsonObject(i).getJsonArray("users").getJsonObject(0);
                if (!idsToFilter.contains(info.getString("id"))) {
                    respondersArray.add(info);
                }
            }
        }

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

    @Override
    public void removeMultiple(String formId, JsonArray removed, Handler<Either<String, JsonObject>> handler) {
        ArrayList<String> idsToRemove = new ArrayList<>();
        if (removed != null) {
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
