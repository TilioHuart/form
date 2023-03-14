package fr.openent.formulaire.service.test.impl;

import fr.openent.formulaire.service.impl.DefaultSectionService;
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
import static fr.openent.form.core.constants.Tables.*;
import static fr.openent.form.helpers.SqlHelper.getParamsForUpdateDateModifFormRequest;
import static fr.openent.form.helpers.SqlHelper.getUpdateDateModifFormRequest;

@RunWith(VertxUnitRunner.class)
public class DefaultSectionServiceTest {
    private Vertx vertx;
    private DefaultSectionService defaultSectionService;

    @Before
    public void setUp(){
        vertx = Vertx.vertx();
        defaultSectionService = new DefaultSectionService();
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);
    }

    @Test
    public void testList(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT * FROM " + SECTION_TABLE + " WHERE form_id = ? ORDER BY position;";
        JsonArray expectedParams = new JsonArray().add("1");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultSectionService.list("1", null);
    }

    @Test
    public void testGet(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT * FROM " + SECTION_TABLE + " WHERE id = ?;";
        JsonArray expectedParams = new JsonArray().add("1");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultSectionService.get("1", null);
    }

    @Test
    public void testCreate(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "INSERT INTO " + SECTION_TABLE + " (form_id, title, description, position) " +
                "VALUES (?, ?, ?, ?) RETURNING *;";

        JsonObject section = new JsonObject();
        section.put(ID, 2)
                .put(FORM_ID, 1)
                .put(TITLE, TITLE)
                .put(POSITION, 1)
                .put(DESCRIPTION, DESCRIPTION)
                .put(ORIGINAL_SECTION_ID, 1);

        JsonArray expectedParams = new JsonArray()
                .add(section.getInteger(FORM_ID, null).toString())
                .add(section.getString(TITLE, ""))
                .add(section.getString(DESCRIPTION, ""))
                .add(section.getInteger(POSITION, null));

        String expectedQueryResult = expectedQuery + getUpdateDateModifFormRequest();
        expectedParams.addAll(getParamsForUpdateDateModifFormRequest(section.getInteger(FORM_ID, null).toString()));

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQueryResult, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultSectionService.create(section, "1", null);
    }

    @Test
    public void testUpdate_NoSections(TestContext ctx) {
        Async async = ctx.async();
        JsonArray sections = new JsonArray();

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            async.complete();
        });

        defaultSectionService.update("1", sections)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testUpdate_Sections(TestContext ctx) {
        Async async = ctx.async();

        JsonObject section = new JsonObject();
        section.put(ID, 2)
                .put(FORM_ID, 1)
                .put(TITLE, TITLE)
                .put(POSITION, 1)
                .put(DESCRIPTION, DESCRIPTION)
                .put(ORIGINAL_SECTION_ID, 1);

        JsonObject updatedSection = new JsonObject();
        updatedSection.put(ID, 4)
                .put(FORM_ID, 1)
                .put(TITLE, TITLE)
                .put(POSITION, 2)
                .put(DESCRIPTION, DESCRIPTION)
                .put(ORIGINAL_SECTION_ID, 2);

        JsonArray sections = new JsonArray();
        sections.add(section)
                .add(updatedSection);

        String expectedQuery = "[{\"action\":\"raw\",\"command\":\"BEGIN;\"}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + SECTION_TABLE + " SET position = NULL WHERE id IN (?,?);\",\"values\":[2,4]}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + SECTION_TABLE + " SET title = ?, description = ?, position = ? WHERE id = ? RETURNING *;\",\"values\":[\"title\",\"description\",1,2]}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + SECTION_TABLE + " SET title = ?, description = ?, position = ? WHERE id = ? RETURNING *;\",\"values\":[\"title\",\"description\",2,4]}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + FORM_TABLE + " SET date_modification = ? WHERE id = ?; \",\"values\":[\"NOW()\",\"1\"]}," +
                "{\"action\":\"raw\",\"command\":\"COMMIT;\"}]";


        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(TRANSACTION, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getJsonArray(STATEMENTS).toString());
            async.complete();
        });

        defaultSectionService.update("1", sections)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testDelete(TestContext ctx){
        Async async = ctx.async();
        String expectedQuery = "DELETE FROM " + SECTION_TABLE + " WHERE id = ?;";

        JsonObject section = new JsonObject();
        section.put(ID, 2)
                .put(FORM_ID, 1)
                .put(TITLE, TITLE)
                .put(POSITION, 1)
                .put(DESCRIPTION, DESCRIPTION)
                .put(ORIGINAL_SECTION_ID, 1);

        JsonArray expectedParams = new JsonArray().add(section.getInteger(ID, null).toString());

        String expectedQueryResult =  expectedQuery + getUpdateDateModifFormRequest();
        expectedParams.addAll(getParamsForUpdateDateModifFormRequest(section.getInteger(FORM_ID).toString()));

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQueryResult, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultSectionService.delete(section, null);
    }
}
