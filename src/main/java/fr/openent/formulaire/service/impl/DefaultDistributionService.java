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
                " WHERE form_id = ? AND responder_id = ? AND active = ? " +
                "ORDER BY date_sending DESC;";
        JsonArray params = new JsonArray().add(formId).add(user.getUserId()).add(true);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void countFinished(String formId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT COUNT(*) FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE form_id = ? AND status = ?;";
        JsonArray params = new JsonArray().add(formId).add(Formulaire.FINISHED);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void countMyToDo(String formId, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT COUNT(*) FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE form_id = ? AND responder_id = ? AND status = ?;";
        JsonArray params = new JsonArray().add(formId).add(user.getUserId()).add(Formulaire.TO_DO);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void get(String distributionId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(distributionId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getByFormResponderAndStatus(String formId, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.DISTRIBUTION_TABLE +
                " WHERE form_id = ? AND responder_id = ? AND status = ?;";
        JsonArray params = new JsonArray().add(formId).add(user.getUserId())
                .add(Formulaire.TO_DO);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void add(JsonObject distribution, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Formulaire.DISTRIBUTION_TABLE + " (form_id, sender_id, sender_name, " +
                "responder_id, responder_name, status, date_sending, active) " +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(distribution.getInteger("form_id", null))
                .add(distribution.getString("sender_id", ""))
                .add(distribution.getString("sender_name", ""))
                .add(distribution.getString("responder_id", ""))
                .add(distribution.getString("responder_name", ""))
                .add(Formulaire.TO_DO)
                .add("NOW()")
                .add(true);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void duplicateWithResponses(String distributionId, Handler<Either<String, JsonObject>> handler) {
        String query =
                "WITH newDistrib AS (" +
                    "INSERT INTO " + Formulaire.DISTRIBUTION_TABLE + " (form_id, sender_id, sender_name, " +
                    "responder_id, responder_name, status, date_sending, date_response, active, original_id) " +
                    "SELECT form_id, sender_id, sender_name, responder_id, responder_name, ?, " +
                    "date_sending, date_response, active, id FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE id = ? RETURNING *" +
                "), " +
                "newResponses AS (" +
                    "INSERT INTO " + Formulaire.RESPONSE_TABLE + " (question_id, answer, responder_id, choice_id, distribution_id, original_id) " +
                    "SELECT question_id, answer, responder_id, choice_id, (SELECT id FROM newDistrib), id " +
                    "FROM " + Formulaire.RESPONSE_TABLE + " WHERE distribution_id = ? RETURNING *" +
                ")," +
                "newResponseFiles AS (" +
                    "INSERT INTO " + Formulaire.RESPONSE_FILE_TABLE + " (id, response_id, filename, type) " +
                    "SELECT id, (SELECT id FROM newResponses WHERE original_id = response_id), filename, type " +
                    "FROM " + Formulaire.RESPONSE_FILE_TABLE + " WHERE response_id IN (SELECT original_id FROM newResponses)" +
                ")" +
                "SELECT * FROM newDistrib;";

        JsonArray params = new JsonArray().add(Formulaire.ON_CHANGE).add(distributionId).add(distributionId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void update(String distributionId, JsonObject distribution, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Formulaire.DISTRIBUTION_TABLE + " SET status = ?, structure = ?";
        JsonArray params = new JsonArray().add(distribution.getString("status")).add(distribution.getString("structure"));

        if (distribution.getString("date_response") != null) {
            query += ", date_response = ?";
            params.add(distribution.getString("date_response"));
        }
        else if (distribution.getString("status").equals(Formulaire.FINISHED)) {
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
        JsonArray respondersIds = getIds(responders);

        JsonArray params = new JsonArray().add(formId);
        String query = "SELECT responder_id FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE form_id = ? ";

        if (respondersIds.size() > 0) {
            query += "AND responder_id NOT IN " + Sql.listPrepared(respondersIds) + ";";
            params.addAll(respondersIds);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getDuplicates(String formId, JsonArray responders, Handler<Either<String, JsonArray>> handler) {
        JsonArray respondersIds = getIds(responders);

        JsonArray params = new JsonArray().add(formId);
        String query = "SELECT responder_id FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE form_id = ? ";

        if (respondersIds.size() > 0) {
            query += "AND responder_id IN " + Sql.listPrepared(respondersIds) + ";";
            params.addAll(respondersIds);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void createMultiple(String formId, UserInfos user, JsonArray responders, JsonArray duplicates, Handler<Either<String, JsonObject>> handler) {
        JsonArray idsToFilter = new JsonArray();
        if (duplicates != null && !duplicates.isEmpty()) {
            for (int i = 0; i < duplicates.size(); i++) {
                idsToFilter.add(duplicates.getJsonObject(i).getString("responder_id"));
            }
        }

        ArrayList<JsonObject> respondersArray = new ArrayList<>();
        ArrayList<String> respondersArrayIds = new ArrayList<>();
        for (int j = 0; j < responders.size(); j++) {
            if (responders.getJsonObject(j) != null && !responders.getJsonObject(j).isEmpty()) {
                JsonObject responder = responders.getJsonObject(j);
                if (!idsToFilter.contains(responder.getString("id")) && !respondersArrayIds.contains(responder.getString("id"))) {
                    respondersArray.add(responder);
                    respondersArrayIds.add(responder.getString("id"));
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
        JsonArray idsToUpdate = new JsonArray();
        if (distributions != null && !distributions.isEmpty()) {
            for (int i = 0; i < distributions.size(); i++) {
                idsToUpdate.add(distributions.getJsonObject(i).getString("responder_id"));
            }
        }

        JsonArray params = new JsonArray().add(active).add(formId);
        String query = "UPDATE " + Formulaire.DISTRIBUTION_TABLE + " SET active = ? WHERE form_id = ? ";

        if (idsToUpdate.size() > 0) {
            query += "AND responder_id IN " + Sql.listPrepared(idsToUpdate) + ";";
            params.addAll(idsToUpdate);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private JsonArray getIds(JsonArray responders) {
        JsonArray respondersIds = new JsonArray();
        for (int j = 0; j < responders.size(); j++) {
            respondersIds.add(responders.getJsonObject(j).getString("id"));
        }
        return respondersIds;
    }
}
