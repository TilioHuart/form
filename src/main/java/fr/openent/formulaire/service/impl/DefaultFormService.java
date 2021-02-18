package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.FormService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultFormService extends SqlCrudService implements FormService {

    public DefaultFormService(String schema, String table, String shareTable) {
        super(schema, table, shareTable);
    }

    @Override
    public void list(UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.FORM_TABLE + " WHERE owner_id = ? " +
                "ORDER BY date_modification DESC;";
        JsonArray params = new JsonArray().add(user.getUserId());
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listSentForms(UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT f.id, title, description, picture, owner_id, owner_name," +
                "date_creation, date_modification, date_opening, date_ending, form_id, status, date_sending, date_response " +
                "FROM " + Formulaire.FORM_TABLE + " f " +
                "INNER JOIN " + Formulaire.DISTRIBUTION_TABLE + " d ON f.id = d.form_id " +
                "WHERE d.responder_id = ? AND NOW() BETWEEN f.date_opening AND f.date_ending " +
                "ORDER BY d.date_sending DESC;";
        JsonArray params = new JsonArray().add(user.getUserId());
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String id, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.FORM_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(id);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(JsonObject form, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Formulaire.FORM_TABLE + " (owner_id, owner_name, title, description, " +
                "picture, date_creation, date_modification, date_opening, date_ending) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(user.getUserId())
                .add(user.getUsername())
                .add(form.getString("title", ""))
                .add(form.getString("description", ""))
                .add(form.getString("picture", ""))
                .add("NOW()").add("NOW()")
                .add(form.getString("date_opening", "NOW()"))
                .add(form.getString("date_ending", "9999-12-31 23:59:59"));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void update(String formId, JsonObject form, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Formulaire.FORM_TABLE + " SET title = ?, description = ?, picture = ?, " +
                "date_modification = ?, date_opening = ?, date_ending = ?, sent = ?, collab = ?, archived = ? WHERE id = ? RETURNING *;";
        JsonArray params = new JsonArray()
                .add(form.getString("title", ""))
                .add(form.getString("description", ""))
                .add(form.getString("picture", ""))
                .add("NOW()")
                .add(form.getString("date_opening", "NOW()"))
                .add(form.getString("date_ending", "9999-12-31 23:59:59"))
                .add(form.getBoolean("sent", false))
                .add(form.getBoolean("collab", false))
                .add(form.getBoolean("archived", false))
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
    public void getImage(EventBus eb, String idImage, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject().put("action", "getDocument").put("id", idImage);
        String WORKSPACE_BUS_ADDRESS = "org.entcore.workspace";
        eb.send(WORKSPACE_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            if (idImage.equals("")) {
                handler.handle(new Either.Left<>("[DefaultDocumentService@get] An error id image"));
            } else {
                handler.handle(new Either.Right<>(message.body().getJsonObject("result")));
            }
        }));
    }
}
