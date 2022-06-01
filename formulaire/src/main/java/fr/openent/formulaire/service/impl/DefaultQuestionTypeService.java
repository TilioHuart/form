package fr.openent.formulaire.service.impl;

import fr.openent.form.core.constants.Tables;
import fr.openent.formulaire.service.QuestionTypeService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultQuestionTypeService implements QuestionTypeService {

    @Override
    public void list(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Tables.QUESTION_TYPE + " ORDER BY code;";
        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }
}
