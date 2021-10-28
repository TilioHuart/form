package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.FolderService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class DefaultFolderService implements FolderService {
    @Override
    public void list(UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.FOLDER_TABLE + " WHERE user_id = ?;";
        JsonArray params = new JsonArray().add(user.getUserId());

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String folderId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.FOLDER_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(folderId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(JsonObject folder, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Formulaire.FOLDER_TABLE + " (parent_id, name, user_id) VALUES (?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(folder.getInteger("parent_id",1))
                .add(folder.getString("name"))
                .add(folder.getString("user_id"));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void update(String folderId, JsonObject folder, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Formulaire.FOLDER_TABLE + " SET parent_id = ?, name = ?, user_id = ? " +
                "WHERE id = ? RETURNING *;";
        JsonArray params = new JsonArray()
                .add(folder.getInteger("parent_id",1))
                .add(folder.getString("name"))
                .add(folder.getString("user_id"))
                .add(folderId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(JsonArray folderIds, Handler<Either<String, JsonArray>> handler) {
        String query = "DELETE FROM " + Formulaire.FOLDER_TABLE + " WHERE id IN " + Sql.listPrepared(folderIds) +
                " RETURNING parent_id";
        JsonArray params = new JsonArray().addAll(folderIds);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void move(JsonArray folderIds, String parentId, Handler<Either<String, JsonArray>> handler) {
        String query = "UPDATE " + Formulaire.FOLDER_TABLE + " SET parent_id = ? WHERE id IN " + Sql.listPrepared(folderIds);
        JsonArray params = new JsonArray().add(parentId).addAll(folderIds);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void syncNbChildren(UserInfos user, String newFolderId, Handler<Either<String, JsonObject>> handler) {
        String countFolder = "SELECT COUNT(*) FROM " + Formulaire.FOLDER_TABLE + " WHERE user_id = ? AND parent_id = ?";
        String countForms = "SELECT COUNT(*) FROM " + Formulaire.REL_FORM_FOLDER_TABLE + " WHERE user_id = ? AND folder_id = ?";

        String query = "UPDATE " + Formulaire.FOLDER_TABLE +
                " SET nb_folder_children = (" + countFolder + "), nb_form_children = (" + countForms + ") " +
                "WHERE id = ?;";
        JsonArray params = new JsonArray()
                .add(user.getUserId()).add(newFolderId)
                .add(user.getUserId()).add(newFolderId)
                .add(newFolderId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
