package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.RelFormFolderService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class DefaultRelFormFolderService implements RelFormFolderService {
    @Override
    public void listAll(UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.REL_FORM_FOLDER_TABLE + " WHERE user_id = ?;";
        JsonArray params = new JsonArray().add(user.getUserId());

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listByFolder(UserInfos user, String folderId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.REL_FORM_FOLDER_TABLE + " WHERE user_id = ? AND folder_id = ?;";
        JsonArray params = new JsonArray().add(user.getUserId()).add(folderId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listFormChildrenRecursively(JsonArray folderIds, Handler<Either<String, JsonArray>> handler) {
        String query =
                "WITH RECURSIVE descendant AS (" +
                    "SELECT id, parent_id FROM " + Formulaire.FOLDER_TABLE + " WHERE id IN " + Sql.listPrepared(folderIds) +
                    "UNION ALL " +
                    "SELECT f.id, f.parent_id FROM " + Formulaire.FOLDER_TABLE + " f " +
                    "JOIN descendant d ON f.parent_id = d.id" +
                ") " +
                "SELECT f.* FROM " + Formulaire.FORM_TABLE + " f " +
                "JOIN " + Formulaire.REL_FORM_FOLDER_TABLE + " rff ON f.id = rff.form_id " +
                "JOIN descendant d ON rff.folder_id = d.id " +
                "JOIN " + Formulaire.FOLDER_TABLE + " a ON d.parent_id = a.id;";
        JsonArray params = new JsonArray().addAll(folderIds);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(UserInfos user, String formId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Formulaire.REL_FORM_FOLDER_TABLE + " WHERE user_id = ? AND form_id = ?;";
        JsonArray params = new JsonArray().add(user.getUserId()).add(formId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void create(UserInfos user, JsonArray formIds, String folderId, Handler<Either<String, JsonArray>> handler) {
        String query = "INSERT INTO " + Formulaire.REL_FORM_FOLDER_TABLE + " (user_id, form_id, folder_id) VALUES ";
        JsonArray params = new JsonArray();

        for (Object formId : formIds) {
            query += "(?, ?, ?), ";
            params.add(user.getUserId()).add(formId).add(folderId);
        }

        query = query.substring(0, query.length() - 2) + " RETURNING *;";

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void update(UserInfos user, JsonArray formIds, String newFolderId, Handler<Either<String, JsonArray>> handler) {
        String query = "UPDATE " + Formulaire.REL_FORM_FOLDER_TABLE + " SET folder_id = ? " +
                "WHERE user_id = ? AND form_id IN " + Sql.listPrepared(formIds) + " RETURNING *;";
        JsonArray params = new JsonArray().add(newFolderId).add(user.getUserId()).addAll(formIds);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
