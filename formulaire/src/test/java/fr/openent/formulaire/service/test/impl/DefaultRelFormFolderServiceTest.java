package fr.openent.formulaire.service.test.impl;

import fr.openent.formulaire.service.impl.DefaultRelFormFolderService;
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

import static fr.openent.form.core.constants.EbFields.FORMULAIRE_ADDRESS;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.ShareRights.CONTRIB_RESOURCE_BEHAVIOUR;
import static fr.openent.form.core.constants.Tables.*;

@RunWith(VertxUnitRunner.class)
public class DefaultRelFormFolderServiceTest {
    private Vertx vertx;
    private DefaultRelFormFolderService defaultRelFormFolderService;

    @Before
    public void setUp(){
        vertx = Vertx.vertx();
        defaultRelFormFolderService = new DefaultRelFormFolderService();
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);
    }

    @Test
    public void testUpdateForRestoration(TestContext ctx) {
        Async async = ctx.async();
        JsonArray formIds = new JsonArray().add("1").add("2").add("3");
        String expectedQuery = "UPDATE " + REL_FORM_FOLDER_TABLE + " rff SET folder_id = folder_target " +
                "FROM (" +
                    "SELECT owner_id AS member_id, id AS form_id, 1 AS folder_target FROM " + FORM_TABLE + " " +
                    "WHERE id IN " + Sql.listPrepared(formIds) +
                    "UNION " +
                    "SELECT DISTINCT fs.member_id, fs.resource_id, 2 AS folder_target FROM " + FORM_TABLE + " f " +
                    "JOIN " + FORM_SHARES_TABLE + " fs ON fs.resource_id = f.id " +
                    "WHERE f.id IN " + Sql.listPrepared(formIds) + " AND fs.action = ? " +
                ") AS contributors " +
                "WHERE rff.user_id = contributors.member_id AND rff.form_id = contributors.form_id " +
                "RETURNING *;";
        JsonArray expectedParams = new JsonArray().addAll(formIds).addAll(formIds).add(CONTRIB_RESOURCE_BEHAVIOUR);

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultRelFormFolderService.updateForRestoration(formIds, null);
    }
}
