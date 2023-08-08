package fr.openent.formulaire.service.test.impl;

import fr.openent.form.core.enums.QuestionTypes;
import fr.openent.form.core.models.Question;
import fr.openent.form.core.models.QuestionSpecificFields;
import fr.openent.formulaire.service.impl.DefaultQuestionSpecificFieldsService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.Async;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static fr.openent.form.core.constants.EbFields.FORMULAIRE_ADDRESS;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Fields.QUESTION_TYPE;
import static fr.openent.form.core.constants.Tables.*;

@RunWith(VertxUnitRunner.class)
public class DefaultQuestionSpecificServiceTest {
    private Vertx vertx;
    private DefaultQuestionSpecificFieldsService defaultQuestionSpecificFieldsService;
    private QuestionSpecificFields questionSpecificFields;

    @Before
    public void setUp(){
        vertx = Vertx.vertx();
        defaultQuestionSpecificFieldsService = new DefaultQuestionSpecificFieldsService();
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);

        JsonObject questionSpecificFieldsJson = new JsonObject()
                .put(CURSOR_MIN_VAL, 2)
                .put(CURSOR_MAX_VAL, 9)
                .put(CURSOR_STEP, 1)
                .put(CURSOR_MIN_LABEL, "min")
                .put(CURSOR_MAX_LABEL, "max");
        questionSpecificFields = new QuestionSpecificFields(questionSpecificFieldsJson);
    }

    @Test
    public void testListSpecificField(TestContext ctx) {
        Async async = ctx.async();
        List<Long> questionIds = new ArrayList<>();
        questionIds.add(1L);
        questionIds.add(2L);
        questionIds.add(3L);

        String expectedQuery = "SELECT * FROM " + QUESTION_SPECIFIC_FIELDS_TABLE + " WHERE question_id IN " + Sql.listPrepared(questionIds);
        JsonArray expectedParams = new JsonArray("[1, 2, 3]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultQuestionSpecificFieldsService.listByIds(questionIds)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void create(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "INSERT INTO " + QUESTION_SPECIFIC_FIELDS_TABLE + " (question_id, cursor_min_val, cursor_max_val, " +
                "cursor_step, cursor_min_label, cursor_max_label) VALUES (?, ?, ?, ?, ?, ?) RETURNING *;";
        JsonArray expectedParams = new JsonArray("[1, 2, 9, 1, \"min\", \"max\"]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultQuestionSpecificFieldsService.create(this.questionSpecificFields, 1L)
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void update(TestContext ctx) {
        Async async = ctx.async();

        JsonObject questionSpecificFieldsJson1 = new JsonObject()
                .put(ID, 11)
                .put(QUESTION_TYPE, 11)
                .put(CURSOR_MIN_VAL, 1)
                .put(CURSOR_MAX_VAL, 10)
                .put(CURSOR_STEP, 1)
                .put(CURSOR_MIN_LABEL, "label_min_val")
                .put(CURSOR_MAX_LABEL, "label_max_val");
        Question question1 = new Question(new JsonObject()
                .put(ID, 1)
                .put(QUESTION_TYPE, QuestionTypes.CURSOR.getCode())
                .put(SPECIFIC_FIELDS, questionSpecificFieldsJson1));

        JsonObject questionSpecificFieldsJson2 = new JsonObject()
                .put(ID, 12)
                .put(QUESTION_TYPE, 11)
                .put(CURSOR_MIN_VAL, 2)
                .put(CURSOR_MAX_VAL, 12)
                .put(CURSOR_STEP, 2)
                .put(CURSOR_MIN_LABEL, "label_mined_val")
                .put(CURSOR_MAX_LABEL, "label_maxed_val");
        Question question2 = new Question(new JsonObject()
                .put(ID, 2)
                .put(QUESTION_TYPE, QuestionTypes.CURSOR.getCode())
                .put(SPECIFIC_FIELDS, questionSpecificFieldsJson2));

        List<Question> questions = new ArrayList<>();
        questions.add(question1);
        questions.add(question2);

        String expectedQuery = "[" +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + QUESTION_SPECIFIC_FIELDS_TABLE + " SET cursor_min_val = ?, cursor_max_val = ?, cursor_step = ?, cursor_min_label = ?, cursor_max_label = ? WHERE question_id = ? RETURNING *;\",\"values\":[1,10,1,\"label_min_val\",\"label_max_val\",1]}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + QUESTION_SPECIFIC_FIELDS_TABLE + " SET cursor_min_val = ?, cursor_max_val = ?, cursor_step = ?, cursor_min_label = ?, cursor_max_label = ? WHERE question_id = ? RETURNING *;\",\"values\":[2,12,2,\"label_mined_val\",\"label_maxed_val\",2]}" +
            "]";

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(TRANSACTION, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getJsonArray(STATEMENTS).toString());
            async.complete();
        });
        defaultQuestionSpecificFieldsService.update(questions)
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }
}
