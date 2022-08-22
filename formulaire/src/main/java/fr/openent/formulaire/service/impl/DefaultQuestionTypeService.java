package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.service.QuestionTypeService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import static fr.openent.form.core.constants.Tables.QUESTION_TYPE_TABLE;

public class DefaultQuestionTypeService implements QuestionTypeService {

    @Override
    public void list(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + QUESTION_TYPE_TABLE + " ORDER BY code;";
        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }
}
