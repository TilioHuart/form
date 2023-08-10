package fr.openent.formulaire.service.test.impl;

import fr.openent.form.core.models.ResponseFile;
import fr.openent.formulaire.service.impl.DefaultResponseFileService;
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

import java.util.ArrayList;
import java.util.List;

import static fr.openent.form.core.constants.EbFields.FORMULAIRE_ADDRESS;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.*;

@RunWith(VertxUnitRunner.class)
public class DefaultResponseFileServiceTest {

    private Vertx vertx;
    private DefaultResponseFileService defaultResponseFileService;
    private ResponseFile responseFile;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        defaultResponseFileService = new DefaultResponseFileService();
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);

        JsonObject responseFileJson = new JsonObject()
                .put(ID, "a4954345-92eb-438a-b2b5-77daeec4724e")
                .put(RESPONSE_ID, 633)
                .put(FILENAME, "myPicture.jpeg")
                .put(TYPE, "image/jpeg");
        responseFile = new ResponseFile(responseFileJson);
    }

    @Test
    public void testDeleteAllByResponse_NoResponseIds(TestContext ctx) {
        Async async = ctx.async();
        List<String> responseIds = new ArrayList<>();

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            async.complete();
        });

        defaultResponseFileService.deleteAllByResponse(responseIds)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testDeleteAllByResponse_ResponseIds(TestContext ctx) {
        Async async = ctx.async();
        List<String> responseIds = new ArrayList<>();
        responseIds.add("1");

        String expectedQuery = "DELETE FROM " + RESPONSE_FILE_TABLE + " " +
                "WHERE response_id IN " + Sql.listPrepared(responseIds) + " " +
                "RETURNING *;";
        JsonArray expectedParams = new JsonArray("[\"1\"]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultResponseFileService.deleteAllByResponse(responseIds)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }
}
