package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.service.FormSharesService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import static fr.openent.form.core.constants.Tables.FORM_SHARES_TABLE;

public class DefaultFormSharesService implements FormSharesService {
    @Override
    public void getSharedWithMe(String formId, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + FORM_SHARES_TABLE + " WHERE resource_id = ? AND member_id = ?;";
        JsonArray params = new JsonArray().add(formId).add(user.getUserId());
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
