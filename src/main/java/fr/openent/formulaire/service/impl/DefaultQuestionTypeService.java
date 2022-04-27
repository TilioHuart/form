package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.QuestionTypeService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultQuestionTypeService implements QuestionTypeService {

    @Override
    public void list(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Formulaire.QUESTION_TYPE_TABLE + " ORDER BY code;";
        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }
}
