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
    public void listByResponder(UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE responder_id = ? AND active = ? " +
                "ORDER BY date_sending DESC;";
        JsonArray params = new JsonArray().add(user.getUserId()).add(true);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listByForm(String formId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE form_id = ? AND active = ? " +
                "ORDER BY date_sending DESC;";
        JsonArray params = new JsonArray().add(formId).add(true);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listByFormAndStatus(String formId, String status, String nbLines, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH distribs AS (SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE form_id = ? AND status = ? " +
                "ORDER BY date_response DESC) " +
                "SELECT * FROM distribs";
        JsonArray params = new JsonArray().add(formId).add(status);

        if (!nbLines.equals("null")) {
            query += " LIMIT ? OFFSET ?";
            params.add(Formulaire.NB_NEW_LINES).add(nbLines);
        }

        query += ";";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    public void listByFormAndResponder(String formId, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE +
                " WHERE form_id = ? AND responder_id = ? AND active = ?" +
                "ORDER BY date_sending DESC;";
        JsonArray params = new JsonArray().add(formId).add(user.getUserId()).add(true);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void count(String formId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT COUNT(*) FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE form_id = ? AND status = ?;";
        JsonArray params = new JsonArray().add(formId).add(Formulaire.FINISHED);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void get(String formId, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE +
                " WHERE form_id = ? AND responder_id = ? AND (status = ? OR status = ?);";
        JsonArray params = new JsonArray().add(formId).add(user.getUserId())
                .add(Formulaire.TO_DO).add(Formulaire.IN_PROGRESS);
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
                    "responder_id, responder_name, status, date_sending, active) " +
                    " VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";
            JsonArray params = new JsonArray()
                    .add(formId)
                    .add(user.getUserId())
                    .add(user.getUsername())
                    .add(responder.getString("id", ""))
                    .add(responder.getString("name", ""))
                    .add(Formulaire.TO_DO)
                    .add("NOW()")
                    .add(true);
            Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
        }
    }

    @Override
    public void add(JsonObject distribution, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT function_add_distrib(?, ?, ?, ?, ?) AS created;";
        JsonArray params = new JsonArray()
                .add(distribution.getInteger("form_id", null))
                .add(distribution.getString("sender_id", ""))
                .add(distribution.getString("sender_name", ""))
                .add(distribution.getString("responder_id", ""))
                .add(distribution.getString("responder_name", ""));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void update(String distributionId, JsonObject distribution, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Formulaire.DISTRIBUTION_TABLE + " SET status = ?, structure = ?";
        JsonArray params = new JsonArray().add(distribution.getString("status")).add(distribution.getString("structure"));

        if (distribution.getString("status").equals(Formulaire.FINISHED)) {
            query += ", date_response = ?";
            params.add("NOW()");
        }

        query += " WHERE id = ? RETURNING *;";
        params.add(distributionId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(String distributionId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(distributionId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }


    // Sync distributions functions (when form is sent/unsent/shared/unshared)

    @Override
    public void getDeactivated(String formId, JsonArray responders, Handler<Either<String, JsonArray>> handler) {
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
    public void createMultiple(String formId, UserInfos user, JsonArray responders, JsonArray duplicates, Handler<Either<String, JsonObject>> handler) {
        ArrayList<String> idsToFilter = new ArrayList<>();
        if (duplicates != null && !duplicates.isEmpty()) {
            for (int i = 0; i < duplicates.size(); i++) {
                idsToFilter.add(duplicates.getJsonObject(i).getString("responder_id"));
            }
        }

        ArrayList<JsonObject> respondersArray = new ArrayList<>();
        ArrayList<String> respondersArrayIds = new ArrayList<>();
        for (int i = 0; i < responders.size(); i++) {
            JsonArray infos = responders.getJsonObject(i).getJsonArray("users");
            if (infos != null && !infos.isEmpty()) {
                for (int j = 0; j < infos.size(); j++) {
                    JsonObject info = infos.getJsonObject(j);
                    if (!idsToFilter.contains(info.getString("id")) && !respondersArrayIds.contains(info.getString("id"))) {
                        respondersArray.add(info);
                        respondersArrayIds.add(info.getString("id"));
                    }
                }
            }
        }

        if (!respondersArray.isEmpty()) {
            JsonArray params = new JsonArray();
            String query = "INSERT INTO " + Formulaire.DISTRIBUTION_TABLE + " (form_id, sender_id, sender_name, responder_id, " +
                    "responder_name, status, date_sending, active) VALUES ";

            for (JsonObject responder : respondersArray) {
                query += "(?, ?, ?, ?, ?, ?, ?, ?), ";
                params.add(formId)
                        .add(user.getUserId())
                        .add(user.getUsername())
                        .add(responder.getString("id", ""))
                        .add(responder.getString("username", ""))
                        .add(Formulaire.TO_DO)
                        .add("NOW()")
                        .add(true);
            }

            query = query.substring(0, query.length() - 2) + ";";
            Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
        }
        else {
            handler.handle(new Either.Right<>(new JsonObject()));
        }
    }

    @Override
    public void setActiveValue(boolean active, String formId, JsonArray distributions, Handler<Either<String, JsonObject>> handler) {
        ArrayList<String> idsToUpdate = new ArrayList<>();
        if (distributions != null && !distributions.isEmpty()) {
            for (int i=0; i < distributions.size(); i++) {
                idsToUpdate.add(distributions.getJsonObject(i).getString("responder_id"));
            }
        }

        JsonArray params = new JsonArray().add(active).add(formId);
        String query = "UPDATE " + Formulaire.DISTRIBUTION_TABLE + " SET active = ? WHERE form_id = ? ";

        if (idsToUpdate.size() > 0) {
            query += "AND responder_id IN (";
            for (String id : idsToUpdate) {
                query += "?, ";
                params.add(id);
            }
            query = query.substring(0, query.length() - 2) + ");";
        }
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
