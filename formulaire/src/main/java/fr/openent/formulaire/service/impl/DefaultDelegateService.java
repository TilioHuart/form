package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.service.DelegateService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import static fr.openent.form.core.constants.Tables.DELEGATE_TABLE;

public class DefaultDelegateService implements DelegateService {

    @Override
    public void list(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + DELEGATE_TABLE + ";";
        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }
}
