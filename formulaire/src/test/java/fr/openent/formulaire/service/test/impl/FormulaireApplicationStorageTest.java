package fr.openent.formulaire.service.test.impl;

import fr.openent.formulaire.service.impl.FormulaireApplicationStorage;
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
import org.powermock.reflect.Whitebox;

import static fr.openent.form.core.constants.EbFields.FORMULAIRE_ADDRESS;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.*;
import static fr.openent.form.core.constants.Tables.DISTRIBUTION_TABLE;

@RunWith(VertxUnitRunner.class)
public class FormulaireApplicationStorageTest {
    private Vertx vertx;
    private FormulaireApplicationStorage formulaireApplicationStorage;

    @Before
    public void setUp(){
        vertx = Vertx.vertx();
        formulaireApplicationStorage = new FormulaireApplicationStorage(null, null);
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);
    }

    @Test
    public void testGetInfectedFileInfos(TestContext ctx) throws Exception {
        Async async = ctx.async();

        String expectedQuery = "SELECT rf.filename, d.responder_id, d.responder_name, d.form_id AS form_id " +
                "FROM " + RESPONSE_FILE_TABLE + " rf " +
                "INNER JOIN " + RESPONSE_TABLE + " r ON r.id = rf.response_id " +
                "INNER JOIN " + DISTRIBUTION_TABLE + " d ON d.id = r.distribution_id " +
                "WHERE rf.id = ?;";
        JsonArray expectedParams = new JsonArray("[\"0162bd36-0093-42b6-b19b-cd1f0e6272cb\"]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        Whitebox.invokeMethod(this.formulaireApplicationStorage, "getInfectedFileInfos", "0162bd36-0093-42b6-b19b-cd1f0e6272cb");

        async.await(10000);
    }
}
