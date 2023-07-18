package fr.openent.formulaire.service.test.impl;

import fr.openent.form.core.models.Folder;
import fr.openent.formulaire.service.impl.DefaultFolderService;
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
import static fr.openent.form.core.constants.Fields.FOLDER;
import static fr.openent.form.core.constants.Tables.*;

@RunWith(VertxUnitRunner.class)
public class DefaultFolderServiceTest {
    private Vertx vertx;
    private DefaultFolderService defaultFolderService;
    private Folder folder;

    @Before
    public void setUp(){
        vertx = Vertx.vertx();
        defaultFolderService = new DefaultFolderService();
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);

        JsonObject folderJson = new JsonObject()
                .put(ID, 5)
                .put(PARENT_ID, 1)
                .put(NAME, FOLDER)
                .put(USER_ID, USER_ID)
                .put(NB_FOLDER_CHILDREN, 3)
                .put(NB_FORM_CHILDREN, 2);
        folder = new Folder(folderJson);
    }

    @Test
    public void testGet(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "SELECT * FROM " + FOLDER_TABLE + " WHERE id = ?;";
        JsonArray expectedParams = new JsonArray("[\"1\"]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultFolderService.get("1")
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }
}
