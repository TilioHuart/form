package fr.openent.formulaire.service.test.impl;

import fr.openent.form.core.constants.Tables;
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

@RunWith(VertxUnitRunner.class)
public class DefaultDistributionServiceTest {
    private Vertx vertx;
    private DefaultDistributionService defaultDistributionService;

    @Before
    public void setUp(){
        vertx = Vertx.vertx();
        defaultDistributionService = new DefaultDistributionService();
        Sql.getInstance().init(vertx.eventBus(), "fr.openent.formulaire");
    }

    @Test
    public void testListByFormAndStatusAndQuestion_NbLinesNull(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT d.* FROM " + Tables.DISTRIBUTION + " d " +
                "JOIN " + Tables.RESPONSE + " r ON r.distribution_id = d.id " +
                "WHERE form_id = ? AND status = ? AND question_id = ? " +
                "ORDER BY date_response DESC;";
        JsonArray expectedParams = new JsonArray().add("form_id").add("status").add("question_id");

        vertx.eventBus().consumer("fr.openent.formulaire", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        defaultDistributionService.listByFormAndStatusAndQuestion("form_id","status","question_id",null,null);
    }

    @Test
    public void testListByFormAndStatusAndQuestion_NbLinesNotNull(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT d.* FROM " + Tables.DISTRIBUTION + " d " +
                "JOIN " + Tables.RESPONSE + " r ON r.distribution_id = d.id " +
                "WHERE form_id = ? AND status = ? AND question_id = ? " +
                "ORDER BY date_response DESC " +
                "LIMIT ? OFFSET ?;";
        JsonArray expectedParams = new JsonArray().add("form_id").add("status").add("question_id").add(NB_NEW_LINES).add("nb_lines");

        vertx.eventBus().consumer("fr.openent.formulaire", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        defaultDistributionService.listByFormAndStatusAndQuestion("form_id","status","question_id","nb_lines",null);
    }
}
