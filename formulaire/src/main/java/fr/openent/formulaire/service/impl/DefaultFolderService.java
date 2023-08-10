package fr.openent.formulaire.service.impl;

import fr.openent.form.core.models.Folder;
import fr.openent.form.helpers.IModelHelper;
import fr.openent.formulaire.service.FolderService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.Optional;

import static fr.openent.form.core.constants.Fields.NAME;
import static fr.openent.form.core.constants.Fields.PARENT_ID;
import static fr.openent.form.core.constants.Tables.FOLDER_TABLE;
import static fr.openent.form.core.constants.Tables.REL_FORM_FOLDER_TABLE;

public class DefaultFolderService implements FolderService {
    @Override
    public void list(UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + FOLDER_TABLE + " WHERE user_id = ?;";
        JsonArray params = new JsonArray().add(user.getUserId());

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listByIds(JsonArray folderIds, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + FOLDER_TABLE + " WHERE id IN " + Sql.listPrepared(folderIds);
        JsonArray params = new JsonArray().addAll(folderIds);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Deprecated
    @Override
    public void get(String folderId, Handler<Either<String, JsonObject>> handler) {
        this.get(folderId)
            .onSuccess(res -> handler.handle(new Either.Right<>(res.get().toJson())))
            .onFailure(error -> handler.handle(new Either.Left<>(error.getMessage())));
    }

    @Override
    public Future<Optional<Folder>> get(String folderId) {
        Promise<Optional<Folder>> promise = Promise.promise();

        String query = "SELECT * FROM " + FOLDER_TABLE + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(folderId);

        String errorMessage = "[Formulaire@DefaultFolderService::get] Fail to get folder with id " + folderId;
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(IModelHelper.sqlUniqueResultToIModel(promise, Folder.class, errorMessage)));

        return promise.future();
    }

    @Override
    public void create(JsonObject folder, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + FOLDER_TABLE + " (parent_id, name, user_id) VALUES (?, ?, ?) RETURNING *;";
        JsonArray params = new JsonArray()
                .add(folder.getInteger(PARENT_ID,1))
                .add(folder.getString(NAME))
                .add(user.getUserId());

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void update(String folderId, JsonObject folder, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + FOLDER_TABLE + " SET parent_id = ?, name = ? WHERE id = ? RETURNING *;";
        JsonArray params = new JsonArray()
                .add(folder.getInteger(PARENT_ID,1))
                .add(folder.getString(NAME))
                .add(folderId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(JsonArray folderIds, Handler<Either<String, JsonArray>> handler) {
        String query = "DELETE FROM " + FOLDER_TABLE + " WHERE id IN " + Sql.listPrepared(folderIds) +
                " RETURNING parent_id";
        JsonArray params = new JsonArray().addAll(folderIds);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void move(JsonArray folderIds, String parentId, Handler<Either<String, JsonArray>> handler) {
        String query = "UPDATE " + FOLDER_TABLE + " SET parent_id = ? WHERE id IN " + Sql.listPrepared(folderIds);
        JsonArray params = new JsonArray().add(parentId).addAll(folderIds);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void syncNbChildren(UserInfos user, JsonArray newFolderIds, Handler<Either<String, JsonArray>> handler) {
        String query =
                "WITH updated_ids AS (" +
                    "UPDATE " + FOLDER_TABLE + " folder " +
                    "SET nb_folder_children = CASE WHEN counts.nb_folders IS NULL THEN 0 ELSE counts.nb_folders END, " +
                    "nb_form_children = CASE WHEN counts.nb_forms IS NULL THEN 0 ELSE counts.nb_forms END " +
                    "FROM ( " +
                        "SELECT * FROM ( " +
                            "SELECT COUNT(*) AS nb_folders, parent_id FROM " + FOLDER_TABLE +
                            " WHERE user_id = ? GROUP BY parent_id " +
                        ") AS f " +
                        "FULL JOIN ( " +
                            "SELECT COUNT(*) AS nb_forms, folder_id FROM " + REL_FORM_FOLDER_TABLE +
                            " WHERE user_id = ? GROUP BY folder_id " +
                        ") AS rff ON rff.folder_id = f.parent_id " +
                    ") AS counts " +
                    "WHERE folder.id IN " + Sql.listPrepared(newFolderIds) +
                    " AND (folder.id = counts.parent_id OR folder.id = counts.folder_id)" +
                    "RETURNING id, folder.parent_id, name, user_id, nb_folder_children, nb_form_children" +
                ")," +
                "other_updated_ids AS (" +
                    "UPDATE " + FOLDER_TABLE +
                    " SET nb_folder_children = 0, nb_form_children = 0 " +
                    "WHERE folder.id IN " + Sql.listPrepared(newFolderIds) +
                    " AND folder.id NOT IN (SELECT id FROM updated_ids) " +
                    "RETURNING *" +
                ")" +
                "SELECT * FROM updated_ids UNION SELECT * FROM other_updated_ids";
        JsonArray params = new JsonArray().add(user.getUserId()).add(user.getUserId()).addAll(newFolderIds).addAll(newFolderIds);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
