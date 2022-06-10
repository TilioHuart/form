package fr.openent.formulaire_public.service.impl;

import fr.openent.form.core.constants.Tables;
import fr.openent.formulaire_public.service.CaptchaService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultCaptchaService implements CaptchaService {

    @Override
    public void get(String captchaId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Tables.CAPTCHA + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(captchaId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
