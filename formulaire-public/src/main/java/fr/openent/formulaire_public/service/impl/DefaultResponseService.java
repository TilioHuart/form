package fr.openent.formulaire_public.service.impl;

import fr.openent.formulaire_public.service.ResponseService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;

import java.util.ArrayList;

import static fr.openent.form.core.constants.Constants.TRANSACTION_BEGIN_QUERY;
import static fr.openent.form.core.constants.Constants.TRANSACTION_COMMIT_QUERY;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.RESPONSE_TABLE;

public class DefaultResponseService implements ResponseService {

    @Override
    public void createResponses(JsonArray responses, JsonObject distribution, Handler<Either<String, JsonArray>> handler) {
        ArrayList<JsonObject> responsesList = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            if (responses.getJsonObject(i) != null) {
                JsonObject response = responses.getJsonObject(i);
                responsesList.add(response);
            }
        }

        if (!responsesList.isEmpty()) {
            SqlStatementsBuilder s = new SqlStatementsBuilder();
            String query = "INSERT INTO " + RESPONSE_TABLE + " (question_id, choice_id, answer, responder_id, distribution_id) " +
                    "VALUES (?, ?, ?, ?, ?);";

            s.raw(TRANSACTION_BEGIN_QUERY);
            for (JsonObject response : responsesList) {
                JsonArray params = new JsonArray()
                        .add(response.getInteger(QUESTION_ID, null))
                        .add(response.getInteger(CHOICE_ID, null))
                        .add(response.getValue(ANSWER, ""))
                        .add("")
                        .add(distribution.getInteger(ID, null));
                s.prepared(query, params);
            }
            s.raw(TRANSACTION_COMMIT_QUERY);

            Sql.getInstance().transaction(s.build(), SqlResult.validResultHandler(handler));
        }
        else {
            handler.handle(new Either.Right<>(new JsonArray()));
        }
    }
}
