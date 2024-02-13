package fr.openent.formulaire.service.test.impl;

import fr.openent.form.core.models.FormShare;
import fr.openent.formulaire.service.impl.DefaultFormSharesService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fr.openent.form.core.constants.EbFields.FORMULAIRE_ADDRESS;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.*;

@RunWith(VertxUnitRunner.class)
public class DefaultFormSharesServiceTest {
    private Vertx vertx;
    private DefaultFormSharesService defaultFormSharesService;
    private FormShare formShare;

    @Before
    public void setUp(){
        vertx = Vertx.vertx();
        defaultFormSharesService = new DefaultFormSharesService();
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);

        JsonObject formShareJson = new JsonObject()
                .put(MEMBER_ID, "4265605f-3352-4f42-8cef-18e150bbf6bf")
                .put(RESOURCE_ID, 9)
                .put(ACTION, "fr-openent-formulaire-controllers-SharingController|shareJson");
        formShare = new FormShare(formShareJson);
    }

    @Test
    public void testGetSharedWithMe(TestContext ctx) {
        Async async = ctx.async();
        UserInfos user = new UserInfos();
        user.setUserId("4265605f-3352-4f42-8cef-18e150bbf6bf");

        String expectedQuery = "SELECT * FROM " + FORM_SHARES_TABLE + " WHERE resource_id = ? AND member_id = ?;";
        JsonArray expectedParams = new JsonArray("[\"24\",\"4265605f-3352-4f42-8cef-18e150bbf6bf\"]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultFormSharesService.getSharedWithMe("24", user, null);
    }

    @Test
    public void testDeleteForFormAndRight_emptyMethods(TestContext ctx) {
        Async async = ctx.async();
        List<String> rightMethods = new ArrayList<>();


        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            async.complete();
        });

        defaultFormSharesService.deleteForFormAndRight(24, rightMethods)
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testDeleteForFormAndRight_withMethods(TestContext ctx) {
        Async async = ctx.async();
        List<String> rightMethods = Arrays.asList("fr-openent-formulaire-controllers-SharingController|shareJson",
                "fr-openent-formulaire-controllers-SharingController|shareJson",
                "fr-openent-formulaire-controllers-FormController|restore");

        String expectedQuery = "DELETE FROM " + FORM_SHARES_TABLE + " WHERE action IN " + Sql.listPrepared(rightMethods) + " AND resource_id = ?;";
        JsonArray expectedParams = new JsonArray("[\"fr-openent-formulaire-controllers-SharingController|shareJson\"," +
                "\"fr-openent-formulaire-controllers-SharingController|shareJson\"," +
                "\"fr-openent-formulaire-controllers-FormController|restore\"," +
                "24]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultFormSharesService.deleteForFormAndRight(24, rightMethods)
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }
}
