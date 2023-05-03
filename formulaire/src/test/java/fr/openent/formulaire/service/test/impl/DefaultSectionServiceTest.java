package fr.openent.formulaire.service.test.impl;

import fr.openent.form.core.models.Section;
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
    private Section section;

    @Before
    public void setUp(){
        vertx = Vertx.vertx();
        defaultSectionService = new DefaultSectionService();
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);

        JsonObject sectionJson = new JsonObject()
                .put(ID, 1)
                .put(FORM_ID, 9)
                .put(TITLE, TITLE)
                .put(POSITION, 2)
                .put(FORM_ELEMENT_TYPE, "SECTION")
                .put(DESCRIPTION, DESCRIPTION)
                .put(ORIGINAL_SECTION_ID, 1)
                .put(NEXT_FORM_ELEMENT_ID, 29)
                .put(NEXT_FORM_ELEMENT_TYPE, "QUESTION")
                .put(IS_NEXT_FORM_ELEMENT_DEFAULT, false);
        section = new Section(sectionJson);
    }

    @Test
    public void testList(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT * FROM " + SECTION_TABLE + " WHERE form_id = ? ORDER BY position;";
        JsonArray expectedParams = new JsonArray("[\"1\"]");

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
        JsonArray expectedParams = new JsonArray("[\"1\"]");

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

        String expectedQuery = "INSERT INTO " + SECTION_TABLE + " (form_id, title, description, position, next_form_element_id, next_form_element_type, is_next_form_element_default) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING *;";
        JsonArray expectedParams = new JsonArray("[\"9\",\"title\",\"description\",2,29,\"QUESTION\",false]");

        String expectedQueryResult = expectedQuery + getUpdateDateModifFormRequest();
        expectedParams.addAll(getParamsForUpdateDateModifFormRequest("9"));

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQueryResult, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultSectionService.create(section, "9")
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
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

        defaultSectionService.update("9", sections)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testUpdate_Sections(TestContext ctx) {
        Async async = ctx.async();

        JsonArray sections = new JsonArray("[" +
            "{\"id\":1,\"form_id\":\"9\",\"title\":\"title\",\"position\":3,\"form_element_type\":\"SECTION\",\"description\":\"description\",\"original_section_id\":0,\"next_form_element_id\":29,\"next_form_element_type\":\"QUESTION\",\"is_next_form_element_default\":false}," +
            "{\"id\":4,\"form_id\":\"9\",\"title\":\"title\",\"position\":5,\"form_element_type\":\"SECTION\",\"description\":\"description\",\"original_section_id\":3,\"next_form_element_id\":27,\"next_form_element_type\":\"QUESTION\",\"is_next_form_element_default\":false}" +
        "]");

        String expectedQuery = "[{\"action\":\"raw\",\"command\":\"BEGIN;\"}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + SECTION_TABLE + " SET position = NULL WHERE id IN (?,?);\",\"values\":[1,4]}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + SECTION_TABLE + " SET title = ?, description = ?, position = ?, next_form_element_id = ?, next_form_element_type = ?, is_next_form_element_default = ? WHERE id = ? RETURNING *;\",\"values\":[\"title\",\"description\",3,29,\"QUESTION\",false,1]}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + SECTION_TABLE + " SET title = ?, description = ?, position = ?, next_form_element_id = ?, next_form_element_type = ?, is_next_form_element_default = ? WHERE id = ? RETURNING *;\",\"values\":[\"title\",\"description\",5,27,\"QUESTION\",false,4]}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + FORM_TABLE + " SET date_modification = ? WHERE id = ?; \",\"values\":[\"NOW()\",\"9\"]}," +
                "{\"action\":\"raw\",\"command\":\"COMMIT;\"}]";


        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(TRANSACTION, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getJsonArray(STATEMENTS).toString());
            async.complete();
        });

        defaultSectionService.update("9", sections)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testDelete(TestContext ctx){
        Async async = ctx.async();
        String expectedQuery = "DELETE FROM " + SECTION_TABLE + " WHERE id = ?;";
        JsonArray expectedParams = new JsonArray("[\"1\"]");

        String expectedQueryResult =  expectedQuery + getUpdateDateModifFormRequest();
        expectedParams.addAll(getParamsForUpdateDateModifFormRequest("9"));

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQueryResult, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultSectionService.delete(section.toJson(), null);
    }

    @Test
    public void testIsSectionTargetValid(TestContext ctx) {
        Async async = ctx.async();
        String targetedTable = QUESTION_TABLE; // Because we defined "QUESTION" in the object section

        String expectedQuery =
                "SELECT COUNT(*) = 1 AS is_valid FROM (SELECT id, form_id, position FROM " + targetedTable + ") AS targets_infos " +
                "WHERE form_id = ? AND position IS NOT NULL AND position > ? AND id = ?;";
        JsonArray expectedParams = new JsonArray("[9,2,29]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultSectionService.isTargetValid(section)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }
}
