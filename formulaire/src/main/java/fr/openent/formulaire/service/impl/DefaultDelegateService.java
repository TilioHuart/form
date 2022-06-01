package fr.openent.formulaire.service.impl;

import fr.openent.form.core.constants.Tables;
import fr.openent.formulaire.service.DelegateService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultDelegateService implements DelegateService {

    @Override
    public void list(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Tables.DELEGATE + ";";
        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }
}
