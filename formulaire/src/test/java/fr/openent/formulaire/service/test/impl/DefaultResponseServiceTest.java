package fr.openent.formulaire.service.test.impl;

import fr.openent.form.core.models.Response;
import fr.openent.formulaire.service.impl.DefaultResponseService;
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
public class DefaultResponseServiceTest {

    private Vertx vertx;
    private DefaultResponseService defaultResponseService;
    private Response response;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        defaultResponseService = new DefaultResponseService();
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);

        JsonObject responseJson = new JsonObject()
                .put(ID, 4)
                .put(QUESTION_ID, 1)
                .put(ANSWER, "coucou")
                .put(RESPONDER_ID, "50251834-1745-4fb9-a3ad-cc034438c688")
                .put(CHOICE_ID, 5)
                .put(DISTRIBUTION_ID, 53)
                .put(ORIGINAL_ID, (Long)null)
                .put(CHOICE_POSITION, 2)
                .put(CUSTOM_ANSWER, (String)null)
                .put(IMAGE, (String)null);
        response = new Response(responseJson);
    }

    @Test
    public void createResponse_Should_Return_Correct_RequestSQL_Default(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "INSERT INTO " + RESPONSE_TABLE + " (question_id, choice_id, answer, responder_id, " +
                "distribution_id, choice_position, custom_answer, image) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";

        JsonObject response = new JsonObject();
        String questionId = "1";
        UserInfos user = new UserInfos();

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            async.complete();
        });
        defaultResponseService.create(response, user, questionId, null);
    }

    @Test
    public void testListForm(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "SELECT r.* FROM " + RESPONSE_TABLE + " r " +
                "JOIN " + DISTRIBUTION_TABLE + " d ON d.id = r.distribution_id " +
                "WHERE form_id = ? ORDER BY question_id, choice_id;";
        JsonArray expectedParams = new JsonArray("[\"24\"]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultResponseService.listByForm("24")
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testListByIds_NoQuestionIds(TestContext ctx) {
        Async async = ctx.async();
        List<String> responseIds = new ArrayList<>();

        String expectedQuery = "SELECT * FROM " + RESPONSE_TABLE + " WHERE id IN " + Sql.listPrepared(responseIds);
        JsonArray expectedParams = new JsonArray();

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultResponseService.listByIds(responseIds)
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testListByIds_QuestionIds(TestContext ctx) {
        Async async = ctx.async();
        List<String> responseIds = Arrays.asList("1", "2", "5", "4");

        String expectedQuery = "SELECT * FROM " + RESPONSE_TABLE + " WHERE id IN " + Sql.listPrepared(responseIds);
        JsonArray expectedParams = new JsonArray("[\"1\", \"2\", \"5\", \"4\"]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultResponseService.listByIds(responseIds)
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testCreateMultiple_NoResponses(TestContext ctx) {
        Async async = ctx.async();
        UserInfos user = new UserInfos();
        user.setUserId("50251834-1745-4fb9-a3ad-cc034438c688");
        List<Response> responses = new ArrayList<>();

        String expectedQuery = "[{" +
                "\"action\":\"prepared\"," +
                "\"statement\":\"INSERT INTO " + RESPONSE_TABLE + " (question_id, choice_id, answer, responder_id, distribution_id, choice_position, custom_answer, image) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;\"," +
                "\"values\":[1,5,\"coucou\",\"50251834-1745-4fb9-a3ad-cc034438c688\",53,2,null,null]" +
                "}]";

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(TRANSACTION, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getJsonArray(STATEMENTS).toString());
            async.complete();
        });

        defaultResponseService.createMultiple(responses, user.getUserId())
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testCreateMultiple_Responses(TestContext ctx) {
        Async async = ctx.async();
        UserInfos user = new UserInfos();
        user.setUserId("50251834-1745-4fb9-a3ad-cc034438c688");
        List<Response> responses = new ArrayList<>();
        responses.add(response);

        String expectedQuery = "[{" +
                "\"action\":\"prepared\"," +
                "\"statement\":\"INSERT INTO " + RESPONSE_TABLE + " (question_id, choice_id, answer, responder_id, distribution_id, choice_position, custom_answer, image) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;\"," +
                "\"values\":[1,5,\"coucou\",\"50251834-1745-4fb9-a3ad-cc034438c688\",53,2,null,null]" +
                "}]";

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(TRANSACTION, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getJsonArray(STATEMENTS).toString());
            async.complete();
        });

        defaultResponseService.createMultiple(responses, user.getUserId())
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testUpdateMultiple_NoResponses(TestContext ctx) {
        Async async = ctx.async();
        UserInfos user = new UserInfos();
        user.setUserId("50251834-1745-4fb9-a3ad-cc034438c688");
        List<Response> responses = new ArrayList<>();
        responses.add(response);

        String expectedQuery = "[{" +
                "\"action\":\"prepared\"," +
                "\"statement\":\"INSERT INTO " + RESPONSE_TABLE + " (question_id, choice_id, answer, responder_id, distribution_id, choice_position, custom_answer, image) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;\"," +
                "\"values\":[1,5,\"coucou\",\"50251834-1745-4fb9-a3ad-cc034438c688\",53,2,null,null]" +
                "}]";

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(TRANSACTION, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getJsonArray(STATEMENTS).toString());
            async.complete();
        });

        defaultResponseService.createMultiple(responses, user.getUserId())
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testUpdateMultiple_Responses(TestContext ctx) {
        Async async = ctx.async();
        UserInfos user = new UserInfos();
        user.setUserId("50251834-1745-4fb9-a3ad-cc034438c688");
        List<Response> responses = new ArrayList<>();
        responses.add(response);

        String expectedQuery = "[{" +
                "\"action\":\"prepared\"," +
                "\"statement\":\"INSERT INTO " + RESPONSE_TABLE + " (question_id, choice_id, answer, responder_id, distribution_id, choice_position, custom_answer, image) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;\"," +
                "\"values\":[1,5,\"coucou\",\"50251834-1745-4fb9-a3ad-cc034438c688\",53,2,null,null]" +
                "}]";

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(TRANSACTION, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getJsonArray(STATEMENTS).toString());
            async.complete();
        });

        defaultResponseService.createMultiple(responses, user.getUserId())
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testDeleteByQuestionsAndDistribution_NoQuestionIds(TestContext ctx) {
        Async async = ctx.async();
        List<Long> questionIds = new ArrayList<>();
        String distributionId = "3";

        String expectedQuery = "DELETE FROM " + RESPONSE_TABLE + " " +
                "WHERE question_id IN " + Sql.listPrepared(questionIds) + " " +
                "AND distribution_id = ?;";
        JsonArray expectedParams = new JsonArray("[\"3\"]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultResponseService.deleteByQuestionsAndDistribution(questionIds, distributionId)
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testDeleteByQuestionsAndDistribution_QuestionIds(TestContext ctx) {
        Async async = ctx.async();
        List<Long> questionIds = new ArrayList<>(Arrays.asList(1L, 2L, 5L, 4L));
        String distributionId = "3";

        String expectedQuery = "DELETE FROM " + RESPONSE_TABLE + " " +
                "WHERE question_id IN " + Sql.listPrepared(questionIds) + " " +
                "AND distribution_id = ?;";
        JsonArray expectedParams = new JsonArray("[1,2,5,4,\"3\"]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultResponseService.deleteByQuestionsAndDistribution(questionIds, distributionId)
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }
}
