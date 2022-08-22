package fr.openent.formulaire.service.test.impl;

import fr.openent.formulaire.service.impl.DefaultDistributionService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static fr.openent.form.core.constants.Constants.NB_NEW_LINES;
import static fr.openent.form.core.constants.EbFields.FORMULAIRE_ADDRESS;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.DISTRIBUTION_TABLE;
import static fr.openent.form.core.constants.Tables.RESPONSE_TABLE;

@RunWith(VertxUnitRunner.class)
public class DefaultDistributionServiceTest {
    private Vertx vertx;
    private DefaultDistributionService defaultDistributionService;

    @Before
    public void setUp(){
        vertx = Vertx.vertx();
        defaultDistributionService = new DefaultDistributionService();
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);
    }

    @Test
    public void testListByFormAndStatusAndQuestion_NbLinesNull(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT d.* FROM " + DISTRIBUTION_TABLE + " d " +
                "JOIN " + RESPONSE_TABLE + " r ON r.distribution_id = d.id " +
                "WHERE form_id = ? AND status = ? AND question_id = ? " +
                "ORDER BY date_response DESC;";
        JsonArray expectedParams = new JsonArray().add(FORM_ID).add(STATUS).add(QUESTION_ID);

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultDistributionService.listByFormAndStatusAndQuestion(FORM_ID, STATUS, QUESTION_ID,null,null);
    }

    @Test
    public void testListByFormAndStatusAndQuestion_NbLinesNotNull(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT d.* FROM " + DISTRIBUTION_TABLE + " d " +
                "JOIN " + RESPONSE_TABLE + " r ON r.distribution_id = d.id " +
                "WHERE form_id = ? AND status = ? AND question_id = ? " +
                "ORDER BY date_response DESC " +
                "LIMIT ? OFFSET ?;";
        JsonArray expectedParams = new JsonArray().add(FORM_ID).add(STATUS).add(QUESTION_ID).add(NB_NEW_LINES).add(NB_LINES);

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultDistributionService.listByFormAndStatusAndQuestion(FORM_ID, STATUS, QUESTION_ID,NB_LINES,null);
    }
}
