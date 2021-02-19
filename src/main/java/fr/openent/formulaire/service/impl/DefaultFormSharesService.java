package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.DistributionService;
import fr.openent.formulaire.service.FormSharesService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;

public class DefaultFormSharesService implements FormSharesService {
    @Override
    public void getSharedWithMe(String formId, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.FORM_SHARES_TABLE + " WHERE resource_id = ? AND member_id = ?;";
        JsonArray params = new JsonArray().add(formId).add(user.getUserId());
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
