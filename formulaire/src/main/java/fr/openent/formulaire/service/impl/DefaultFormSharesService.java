package fr.openent.formulaire.service.impl;

import fr.openent.form.core.models.FormShare;
import fr.openent.form.helpers.IModelHelper;
import fr.openent.formulaire.service.FormSharesService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

import static fr.openent.form.core.constants.Tables.FORM_SHARES_TABLE;

public class DefaultFormSharesService implements FormSharesService {
    @Override
    public void getSharedWithMe(String formId, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + FORM_SHARES_TABLE + " WHERE resource_id = ? AND member_id = ?;";
        JsonArray params = new JsonArray().add(formId).add(user.getUserId());
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public Future<List<FormShare>> deleteForFormAndRight(Number formId, List<String> rightMethods) {
        Promise<List<FormShare>> promise = Promise.promise();

        if (rightMethods == null || rightMethods.isEmpty()) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        String query = "DELETE FROM " + FORM_SHARES_TABLE + " WHERE action IN " + Sql.listPrepared(rightMethods) + " AND resource_id = ?;";
        JsonArray params = new JsonArray().addAll(new JsonArray(rightMethods)).add(formId);

        String errorMessage = "[Formulaire@DefaultFormSharesService::deleteForFormAndRight] Failed to delete sharing methods for form with id " + formId;
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, FormShare.class, errorMessage)));

        return promise.future();
    }
}
