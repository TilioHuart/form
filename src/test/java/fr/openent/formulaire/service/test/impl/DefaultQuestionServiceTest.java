package fr.openent.formulaire.service.test.impl;
import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.helpers.SqlHelper;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.Async;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.junit.Assert.fail;

@RunWith(VertxUnitRunner.class)
public class DefaultQuestionServiceTest {
    private Vertx vertx;
    private DefaultQuestionService defaultQuestionService;


    @Before
    public void setUp(){
        vertx = Vertx.vertx();
        defaultQuestionService = new DefaultQuestionService();
        Sql.getInstance().init(vertx.eventBus(), "fr.openent.formulaire");
        try {
            setFinalStatic(Formulaire.class.getDeclaredField("QUESTION_TABLE"), "formulaire.question");
            setFinalStatic(Formulaire.class.getDeclaredField("SECTION_TABLE"), "formulaire.section");
            setFinalStatic(Formulaire.class.getDeclaredField("FORM_TABLE"), "formulaire.form");
        }
        catch (Exception e) {fail();}
    }

    @Test
    public void testListForForm(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT * FROM formulaire.question WHERE form_id = ? AND section_id IS NULL ORDER BY position;";
        JsonArray expectedParams = new JsonArray().add("form_id");

        vertx.eventBus().consumer("fr.openent.formulaire", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        defaultQuestionService.listForForm("form_id", null);
    }

    @Test
    public void testListForSection(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT * FROM formulaire.question WHERE section_id = ? ORDER BY section_position;";
        JsonArray expectedParams = new JsonArray().add("section_id");

        vertx.eventBus().consumer("fr.openent.formulaire", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        defaultQuestionService.listForSection("section_id", null);
    }

    @Test
    public void testListForFormAndSection(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT * FROM formulaire.question WHERE form_id = ? ORDER BY position, section_id, section_position;";
        JsonArray expectedParams = new JsonArray().add("form_id");

        vertx.eventBus().consumer("fr.openent.formulaire", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        defaultQuestionService.listForFormAndSection("form_id", null);
    }

    @Test
    public void testExport(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT q.*, (CASE WHEN q.position ISNULL THEN s.position WHEN s.position ISNULL THEN q.position END) AS element_position " +
                "FROM formulaire.question q LEFT JOIN formulaire.section s ON q.section_id = s.id " +
                "WHERE q.form_id = ? " +
                "ORDER BY element_position, q.section_position;";

        JsonArray expectedParams = new JsonArray().add("form_id");

        vertx.eventBus().consumer("fr.openent.formulaire", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        defaultQuestionService.export("form_id",true, null);
    }

    @Test
    public void get(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT * FROM formulaire.question WHERE id = ?;";
        JsonArray expectedParams = new JsonArray().add("form_id");

        vertx.eventBus().consumer("fr.openent.formulaire", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        defaultQuestionService.get("form_id", null);
    }

    @Test
    public void create(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "INSERT INTO formulaire.question " +
                "(form_id, title, position, question_type, statement, mandatory, section_id, section_position, conditional) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";

        JsonObject question = new JsonObject();
        JsonArray expectedParams = new JsonArray().add("form_id")
                .add(question.getString("title", ""))
                .add(question.getInteger("section_position", null) != null ? null : question.getInteger("position", null))
                .add(question.getInteger("question_type", 1))
                .add(question.getString("statement", ""))
                .add(question.getBoolean("conditional", false) || question.getBoolean("mandatory", false))
                .add(question.getInteger("section_id", null))
                .add(question.getInteger("section_position", null))
                .add(question.getBoolean("conditional", false));

        String expectedQueryResult = expectedQuery + SqlHelper.getUpdateDateModifFormRequest();
        expectedParams.addAll(SqlHelper.getParamsForUpdateDateModifFormRequest("form_id"));

        vertx.eventBus().consumer("fr.openent.formulaire", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQueryResult, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        defaultQuestionService.create(question, "form_id", null);
    }

    @Test
    public void createMultiple(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "INSERT INTO formulaire.question (form_id, title, position, question_type, statement, mandatory, section_id, section_position, conditional) VALUES ";
        JsonArray questions = new JsonArray();
        List<JsonObject> allQuestions = questions.getList();
        JsonArray expectedParams = new JsonArray();
        for (JsonObject question : allQuestions){
            expectedQuery += "(?, ?, ?, ?, ?, ?, ?, ?, ?), ";
            expectedParams.add("form_id")
                    .add(question.getString("title", ""))
                    .add(question.getInteger("section_position", null) != null ? null : question.getInteger("position", null))
                    .add(question.getInteger("question_type", 1))
                    .add(question.getString("statement", ""))
                    .add(question.getBoolean("mandatory", false))
                    .add(question.getInteger("section_id", null))
                    .add(question.getInteger("section_position", null))
                    .add(question.getBoolean("conditional", false));
        }
        expectedQuery = expectedQuery.substring(0, expectedQuery.length() - 2) + " RETURNING *;";
        String expectedQueryResult = expectedQuery + SqlHelper.getUpdateDateModifFormRequest();
        expectedParams.addAll(SqlHelper.getParamsForUpdateDateModifFormRequest("form_id"));

        vertx.eventBus().consumer("fr.openent.formulaire", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQueryResult, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        defaultQuestionService.createMultiple(questions,"form_id",null);
    }

    @Test
    public void update(TestContext ctx) {
        Async async = ctx.async();
        JsonObject tabQuestion = new JsonObject();
        tabQuestion.put("title", "title")
                .put("position", 1)
                .put("question_type", 1)
                .put("statement", "statement")
                .put("mandatory", false)
                .put("section_id", 1)
                .put("section_position",1)
                .put("conditional", false)
                .put("id", 1);
        JsonObject tabQuestionNew = new JsonObject();
        tabQuestionNew.put("title", "titled")
                .put("position", 2)
                .put("question_type", 2)
                .put("statement", "statemented")
                .put("mandatory", true)
                .put("section_id", 2)
                .put("section_position", 2)
                .put("conditional", true)
                .put("id", 2);
        JsonArray questions = new JsonArray();
        questions.add(tabQuestion)
                .add(tabQuestionNew);

        String expectedQuery = "[{\"action\":\"raw\",\"command\":\"BEGIN;\"}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE formulaire.question SET title = ?, position = ?, question_type = ?, statement = ?, mandatory = ?, section_id = ?, section_position = ?, conditional = ?  WHERE id = ? RETURNING *;\",\"values\":[\"title\",null,1,\"statement\",false,1,1,false,1]}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE formulaire.question SET title = ?, position = ?, question_type = ?, statement = ?, mandatory = ?, section_id = ?, section_position = ?, conditional = ?  WHERE id = ? RETURNING *;\",\"values\":[\"titled\",null,2,\"statemented\",true,2,2,true,2]}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE formulaire.form SET date_modification = ? WHERE id = ?; \",\"values\":[\"NOW()\",\"form_id\"]}," +
                "{\"action\":\"raw\",\"command\":\"COMMIT;\"}]";


        vertx.eventBus().consumer("fr.openent.formulaire", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("transaction", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getJsonArray("statements").toString());
            async.complete();
        });
        defaultQuestionService.update("form_id", questions,null);
    }

    @Test
    public void delete(TestContext ctx){
        Async async = ctx.async();
        JsonObject tabQuestion = new JsonObject();
        tabQuestion.put("title", "title")
                .put("position", 1)
                .put("question_type", 1)
                .put("statement", "statement")
                .put("mandatory", false)
                .put("section_id", 1)
                .put("section_position",1)
                .put("conditional", false)
                .put("form_id", 1);
        String expectedQuery = "DELETE FROM formulaire.question WHERE id = ?;";

        JsonArray expectedParams = new JsonArray().add(tabQuestion.getInteger("id"));

        String expectedQueryResult =  expectedQuery + SqlHelper.getUpdateDateModifFormRequest();
        expectedParams.addAll(SqlHelper.getParamsForUpdateDateModifFormRequest(tabQuestion.getInteger("form_id").toString()));
        vertx.eventBus().consumer("fr.openent.formulaire", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQueryResult, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        defaultQuestionService.delete(tabQuestion, null);
    }

    static void setFinalStatic(Field field, String newValue) throws Exception {
        field.setAccessible(true);
        field.set(null, newValue);
    }
}
