package fr.openent.formulaire.service.test.impl;

import fr.openent.form.core.models.FormElement;
import fr.openent.form.core.models.Question;
import fr.openent.form.core.models.QuestionChoice;
import fr.openent.form.core.models.Section;
import fr.openent.formulaire.service.impl.DefaultQuestionChoiceService;
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
import java.util.Collections;
import java.util.List;

import static fr.openent.form.core.constants.EbFields.FORMULAIRE_ADDRESS;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.*;

@RunWith(VertxUnitRunner.class)
public class DefaultQuestionChoiceServiceTest {
    private Vertx vertx;
    private DefaultQuestionChoiceService defaultQuestionChoiceService;
    private QuestionChoice choice;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        defaultQuestionChoiceService = new DefaultQuestionChoiceService();
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);

        JsonObject questionChoice = new JsonObject()
                .put(ID, 1)
                .put(QUESTION_ID, 23)
                .put(VALUE, "C")
                .put(TYPE, "TXT")
                .put(POSITION, 2)
                .put(NEXT_FORM_ELEMENT_ID, 29)
                .put(NEXT_FORM_ELEMENT_TYPE, "SECTION")
                .put(IS_NEXT_FORM_ELEMENT_DEFAULT, false)
                .put(IS_CUSTOM, true)
                .put(IMAGE, "/workspace/document/25389fc4-dbd8-4952-b4bd-bce9fb30b559");
        choice = new QuestionChoice(questionChoice);
    }

    @Test
    public void testCreate(TestContext ctx) {
        Async async = ctx.async();
        String questionId = "1";
        String locale = "fr";

        String expectedQuery = "INSERT INTO " + QUESTION_CHOICE_TABLE + " (question_id, value, position, type, next_form_element_id, " +
                "next_form_element_type, is_next_form_element_default, is_custom, image) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";
        JsonArray expectedParams = new JsonArray("[\"1\",\"formulaire.other\",2,\"TXT\",29,\"SECTION\",false,true, " +
                "\"/workspace/document/25389fc4-dbd8-4952-b4bd-bce9fb30b559\"]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultQuestionChoiceService.create(questionId, choice, locale)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testUpdate_oneChoice(TestContext ctx) {
        Async async = ctx.async();
        String locale = "fr";

        String expectedQuery = "UPDATE " + QUESTION_CHOICE_TABLE + " SET value = ?, position = ?, type = ?, " +
                "next_form_element_id = ?, next_form_element_type = ?, is_next_form_element_default = ?, is_custom = ?, " +
                "image = ? WHERE id = ? RETURNING *;";
        JsonArray expectedParams = new JsonArray("[\"formulaire.other\",2,\"TXT\",29,\"SECTION\",false,true, " +
                "\"/workspace/document/25389fc4-dbd8-4952-b4bd-bce9fb30b559\", 1]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultQuestionChoiceService.update(choice, locale)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testDelete(TestContext ctx) {
        Async async = ctx.async();
        String choiceId = "1";

        String expectedQuery = "DELETE FROM " + QUESTION_CHOICE_TABLE + " WHERE id = ?;";
        JsonArray expectedParams = new JsonArray("[\"1\"]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultQuestionChoiceService.delete(choiceId)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testIsChoiceTargetValid(TestContext ctx) {
        Async async = ctx.async();
        String targetedTable = SECTION_TABLE; // Because we defined "SECTION" in the object choice

        String expectedQuery =
                "WITH question AS (SELECT * FROM " + QUESTION_TABLE + " WHERE id = ?), " +
                "element_position AS (SELECT CASE " +
                    "WHEN (SELECT section_id FROM question) IS NULL THEN (SELECT position FROM question) " +
                    "ELSE (SELECT position FROM " + SECTION_TABLE + " WHERE id = (SELECT section_id FROM question))" +
                "END) " +
                "SELECT COUNT(*) = 1 AS count FROM (SELECT id, form_id, position FROM " + targetedTable + ") AS targets_infos " +
                "WHERE form_id = (SELECT form_id FROM " + QUESTION_TABLE + " WHERE id = ?) " +
                "AND position IS NOT NULL " +
                "AND position > (SELECT position FROM element_position) " +
                "AND id = ?;";
        JsonArray expectedParams = new JsonArray("[23,23,29]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultQuestionChoiceService.isTargetValid(choice)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }
}