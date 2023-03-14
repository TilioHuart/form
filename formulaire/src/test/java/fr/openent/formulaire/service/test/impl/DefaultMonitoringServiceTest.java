package fr.openent.formulaire.service.test.impl;

import fr.openent.formulaire.service.impl.DefaultMonitoringService;
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

@RunWith(VertxUnitRunner.class)
public class DefaultMonitoringServiceTest {
    private Vertx vertx;
    private DefaultMonitoringService defaultMonitoringService;

    @Before
    public void setUp(){
        vertx = Vertx.vertx();
        defaultMonitoringService = new DefaultMonitoringService();
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);
    }

    @Test
    public void testGetFormIdsWithPositionDuplicates(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery =
                "SELECT form_id FROM (" +
                    "SELECT form_id, position FROM (" +
                        "SELECT form_id, position FROM " + SECTION_TABLE + " " +
                        "UNION ALL " +
                        "SELECT form_id, position FROM " + QUESTION_TABLE + " " +
                    ") AS qs " +
                    "GROUP BY form_id, position " +
                    "HAVING position IS NOT NULL AND COUNT(*) > 1" +
                ") AS form_ids;";
        JsonArray expectedParams = new JsonArray();

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultMonitoringService.getFormIdsWithPositionDuplicates()
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testGetPositionDuplicates_NoFormIds(TestContext ctx) {
        Async async = ctx.async();
        JsonArray formIds = new JsonArray();

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            async.complete();
        });

        defaultMonitoringService.getPositionDuplicates(formIds)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testGetPositionDuplicates_FormIds(TestContext ctx) {
        Async async = ctx.async();
        JsonArray formIds = new JsonArray().add("1").add("2");
        String expectedQuery =
                "SELECT * FROM ( " +
                    "SELECT id, form_id, title, position, true AS is_section FROM " + SECTION_TABLE + " " +
                    "UNION " +
                    "SELECT id, form_id, title, position, false AS is_section FROM " + QUESTION_TABLE + " " +
                ") AS qs " +
                "WHERE form_id IN " + Sql.listPrepared(formIds) + " " +
                "ORDER BY form_id, position, id;";
        JsonArray expectedParams = new JsonArray().addAll(formIds);

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultMonitoringService.getPositionDuplicates(formIds)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testCleanPositionDuplicates_NoFormIds(TestContext ctx) {
        Async async = ctx.async();
        JsonArray formIds = new JsonArray();

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(TRANSACTION, body.getString(ACTION));
            async.complete();
        });

        defaultMonitoringService.cleanPositionDuplicates(formIds)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testCleanPositionDuplicates_FormIds(TestContext ctx) {
        Async async = ctx.async();
        JsonArray formIds = new JsonArray().add(1).add(2);

        String nullifyingQuery =
                "WITH nullyfied_sections AS ( " +
                    "UPDATE " + SECTION_TABLE + " s " +
                    "SET position = null " +
                    "WHERE position IS NOT NULL AND form_id IN " + Sql.listPrepared(formIds) + " " +
                    "RETURNING id, true AS is_section " +
                "), " +
                "nullyfied_questions AS (" +
                    "UPDATE " + QUESTION_TABLE + " q " +
                    "SET position = null " +
                    "WHERE position IS NOT NULL AND form_id IN " + Sql.listPrepared(formIds) + " " +
                    "RETURNING id, false AS is_section " +
                ") " +
                "SELECT * FROM nullyfied_sections UNION SELECT * FROM nullyfied_questions " +
                "ORDER BY id, is_section;";

        String updatingQuery =
                "WITH ranking_position AS ( " +
                    "SELECT *, RANK() OVER (PARTITION BY form_id ORDER BY position, id) AS rank " +
                    "FROM ( " +
                        "SELECT id, form_id, position, true AS is_section FROM " + SECTION_TABLE + " " +
                        "UNION " +
                        "SELECT id, form_id, position, false AS is_section FROM " + QUESTION_TABLE + " " +
                    ") AS elements " +
                "), " +
                "updated_sections AS ( " +
                    "UPDATE " + SECTION_TABLE + " s " +
                    "SET position = ( " +
                        "SELECT rank FROM ranking_position r " +
                        "WHERE s.id = r.id AND r.is_section = true " +
                    ") " +
                    "WHERE id IN (SELECT id FROM " + SECTION_TABLE + " WHERE form_id IN " + Sql.listPrepared(formIds) + ") " +
                    "RETURNING id, form_id, title, position, true AS is_section " +
                "), " +
                "updated_questions AS ( " +
                    "UPDATE " + QUESTION_TABLE + " q " +
                    "SET position = (" +
                        "SELECT rank FROM ranking_position r " +
                        "WHERE q.id = r.id AND r.is_section = false " +
                    ")" +
                    "WHERE id IN (SELECT id FROM " + QUESTION_TABLE + " WHERE form_id IN " + Sql.listPrepared(formIds) + ") " +
                    "AND (section_id IS NULL AND section_position IS NULL AND matrix_id IS NULL AND matrix_position IS NULL)" +
                    "RETURNING id, form_id, title, position, false AS is_section " +
                ") " +
                "SELECT * FROM updated_sections " +
                "UNION " +
                "SELECT * FROM updated_questions " +
                "ORDER BY form_id, position, id;";

        String expectedQuery = "[{\"action\":\"raw\",\"command\":\"BEGIN;\"}," +
                "{\"action\":\"prepared\",\"statement\":\"" + nullifyingQuery + "\",\"values\":[1,2,1,2]}," +
                "{\"action\":\"prepared\",\"statement\":\"" + updatingQuery + "\",\"values\":[1,2,1,2]}," +
                "{\"action\":\"raw\",\"command\":\"COMMIT;\"}]";


        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(TRANSACTION, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getJsonArray(STATEMENTS).toString());
            async.complete();
        });

        defaultMonitoringService.cleanPositionDuplicates(formIds)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }
}
