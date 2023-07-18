package fr.openent.formulaire.service.test.impl;

import fr.openent.formulaire.service.impl.DefaultResponseService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static fr.openent.form.core.constants.EbFields.FORMULAIRE_ADDRESS;
import static fr.openent.form.core.constants.Fields.*;

import static fr.openent.form.core.constants.Tables.*;

@RunWith(VertxUnitRunner.class)
public class DefaultResponseServiceTest {

    private Vertx vertx;
    private DefaultResponseService defaultResponseService;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        defaultResponseService = new DefaultResponseService();
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);
    }

    @Test
    public void createResponse_Should_Return_Correct_RequestSQL_Default(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "INSERT INTO " + RESPONSE_TABLE + " (question_id, choice_id, answer, responder_id, " +
                "distribution_id, choice_position, custom_answer, image) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";

        JsonObject response = new JsonObject();
        String questionId = "1";
        UserInfos user = new UserInfos();

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            async.complete();
        });
        defaultResponseService.create(response, user, questionId, null);
    }
}
