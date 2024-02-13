package fr.openent.formulaire.service.test.impl;

import fr.openent.form.core.models.Question;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
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
import java.util.Arrays;
import java.util.List;

import static fr.openent.form.core.constants.Constants.*;
import static fr.openent.form.core.constants.EbFields.FORMULAIRE_ADDRESS;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Fields.QUESTION_TYPE;
import static fr.openent.form.core.constants.Tables.*;
import static fr.openent.form.helpers.SqlHelper.getParamsForUpdateDateModifFormRequest;
import static fr.openent.form.helpers.SqlHelper.getUpdateDateModifFormRequest;

@RunWith(VertxUnitRunner.class)
public class DefaultQuestionServiceTest {
    private Vertx vertx;
    private DefaultQuestionService defaultQuestionService;
    private Question question;

    @Before
    public void setUp(){
        vertx = Vertx.vertx();
        defaultQuestionService = new DefaultQuestionService();
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);

        JsonObject questionJson = new JsonObject()
                .put(ID, 102)
                .put(FORM_ID, 9)
                .put(TITLE, TITLE)
                .put(POSITION, 2)
                .put(FORM_ELEMENT_TYPE, "QUESTION")
                .put(QUESTION_TYPE, 8)
                .put(STATEMENT, "Freetext blabla")
                .put(MANDATORY, false)
                .put(ORIGINAL_QUESTION_ID, 1)
                .put(SECTION_ID, 4)
                .put(SECTION_POSITION, 3)
                .put(CONDITIONAL, false)
                .put(PLACEHOLDER, PLACEHOLDER)
                .put(MATRIX_ID, (Long)null)
                .put(MATRIX_POSITION, (Long)null);
        question = new Question(questionJson);
    }

    @Test
    public void testListForForm(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT * FROM " + QUESTION_TABLE + " WHERE form_id = ? AND section_id IS NULL AND matrix_id IS NULL ORDER BY position;";
        JsonArray expectedParams = new JsonArray().add(FORM_ID);

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultQuestionService.listForForm(FORM_ID, null);
    }

    @Test
    public void testListForSection(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT * FROM " + QUESTION_TABLE + " WHERE section_id = ? AND matrix_id IS NULL " +
                "ORDER BY section_position;";
        JsonArray expectedParams = new JsonArray().add(SECTION_ID);

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultQuestionService.listForSection(SECTION_ID, null);
    }

    @Test
    public void testListForFormAndSection(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT * FROM " + QUESTION_TABLE + " WHERE form_id = ? AND matrix_id IS NULL " +
                "ORDER BY position, section_id, section_position;";
        JsonArray expectedParams = new JsonArray().add(FORM_ID);

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultQuestionService.listForFormAndSection(FORM_ID)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testListChildren(TestContext ctx) {
        Async async = ctx.async();
        JsonArray questionIds = new JsonArray().add("1").add("2").add("3");
        String expectedQuery = "SELECT * FROM " + QUESTION_TABLE + " WHERE matrix_id IN " + Sql.listPrepared(questionIds);
        JsonArray expectedParams = new JsonArray().addAll(questionIds);

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultQuestionService.listChildren(questionIds, null);
    }

    @Test
    public void testListByIds_NoQuestionIds(TestContext ctx) {
        Async async = ctx.async();
        List<Long> questionIds = new ArrayList<>();

        String expectedQuery = "SELECT * FROM " + QUESTION_TABLE + " WHERE id IN " + Sql.listPrepared(questionIds);
        JsonArray expectedParams = new JsonArray();

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultQuestionService.listByIds(questionIds)
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testListByIds_QuestionIds(TestContext ctx) {
        Async async = ctx.async();
        List<Long> questionIds = Arrays.asList(1L,2L,5L,4L);

        String expectedQuery = "SELECT * FROM " + QUESTION_TABLE + " WHERE id IN " + Sql.listPrepared(questionIds);
        JsonArray expectedParams = new JsonArray("[1, 2, 5, 4]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultQuestionService.listByIds(questionIds)
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testExportCSV(TestContext ctx) {
        Async async = ctx.async();
        String getElementPosition =
                "CASE " +
                    "WHEN q.position ISNULL AND s.position IS NOT NULL THEN s.position " +
                    "WHEN s.position ISNULL AND q.position IS NOT NULL THEN q.position " +
                    "WHEN s.position ISNULL AND q.position ISNULL THEN parent.position " +
                "END AS element_position";

        String getSectionPosition =
                "CASE " +
                    "WHEN parent.section_position ISNULL AND q.section_position IS NOT NULL THEN q.section_position " +
                    "WHEN q.section_position ISNULL AND parent.section_position IS NOT NULL THEN parent.section_position " +
                "END AS section_position";

        String expectedQuery = "SELECT q.id, q.title, q.position, q.question_type, q.statement, q.mandatory, q.section_id, " +
                getElementPosition + ", " + getSectionPosition + ", q.matrix_position, q.conditional " +
                "FROM " + QUESTION_TABLE + " q " +
                "LEFT JOIN " + QUESTION_TABLE + " parent ON parent.id = q.matrix_id " +
                "LEFT JOIN " + SECTION_TABLE + " s ON q.section_id = s.id OR parent.section_id = s.id " +
                "WHERE q.form_id = ? AND q.question_type NOT IN " + Sql.listPrepared(QUESTIONS_WITHOUT_RESPONSES) +
                "ORDER BY element_position, section_position, q.matrix_position, q.id;";

        JsonArray expectedParams = new JsonArray().add(FORM_ID).addAll(new JsonArray(QUESTIONS_WITHOUT_RESPONSES));

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultQuestionService.getExportInfos(FORM_ID,false, null);
    }

    @Test
    public void testExportPDF(TestContext ctx) {
        Async async = ctx.async();
        String getElementPosition =
                "CASE " +
                    "WHEN q.position ISNULL AND s.position IS NOT NULL THEN s.position " +
                    "WHEN s.position ISNULL AND q.position IS NOT NULL THEN q.position " +
                    "WHEN s.position ISNULL AND q.position ISNULL THEN parent.position " +
                "END AS element_position";

        String getSectionPosition =
                "CASE " +
                    "WHEN parent.section_position ISNULL AND q.section_position IS NOT NULL THEN q.section_position " +
                    "WHEN q.section_position ISNULL AND parent.section_position IS NOT NULL THEN parent.section_position " +
                "END AS section_position";

        String expectedQuery = "SELECT q.id, q.title, q.position, q.question_type, q.statement, q.mandatory, q.section_id, " +
                getElementPosition + ", " + getSectionPosition + ", q.matrix_position, q.conditional " +
                "FROM " + QUESTION_TABLE + " q " +
                "LEFT JOIN " + QUESTION_TABLE + " parent ON parent.id = q.matrix_id " +
                "LEFT JOIN " + SECTION_TABLE + " s ON q.section_id = s.id OR parent.section_id = s.id " +
                "WHERE q.form_id = ? AND q.matrix_id IS NULL " +
                "ORDER BY element_position, section_position, q.matrix_position, q.id;";

        JsonArray expectedParams = new JsonArray().add(FORM_ID);

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultQuestionService.getExportInfos(FORM_ID,true, null);
    }

    @Test
    public void testCreate(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "INSERT INTO " + QUESTION_TABLE + " (form_id, title, position, question_type, statement, " +
                "mandatory, section_id, section_position, conditional, placeholder, matrix_id, matrix_position) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;";
        JsonArray expectedParams = new JsonArray("[\"9\", \"title\", null, 8, \"Freetext blabla\", false, 4, 3, false, \"placeholder\", null, null]");

        String expectedQueryResult = expectedQuery + getUpdateDateModifFormRequest();
        expectedParams.addAll(getParamsForUpdateDateModifFormRequest("9"));

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQueryResult, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultQuestionService.create(this.question, "9")
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);;
    }

    @Test
    public void testUpdate(TestContext ctx) {
        Async async = ctx.async();
        JsonObject tabQuestion = new JsonObject();
        tabQuestion.put(TITLE, TITLE)
                .put(POSITION, 1)
                .put(QUESTION_TYPE, 9)
                .put(STATEMENT, STATEMENT)
                .put(MANDATORY, false)
                .put(SECTION_ID, 1)
                .put(SECTION_POSITION,1)
                .put(CONDITIONAL, false)
                .put(PLACEHOLDER, PLACEHOLDER)
                .put(MATRIX_ID, 1)
                .put(MATRIX_POSITION, 1)
                .put(ID, 1);
        JsonObject tabQuestionNew = new JsonObject();
        tabQuestionNew.put(TITLE, "titled")
                .put(POSITION, 2)
                .put(QUESTION_TYPE, 5)
                .put(STATEMENT, "statemented")
                .put(MANDATORY, true)
                .put(SECTION_ID, 2)
                .put(SECTION_POSITION, 2)
                .put(CONDITIONAL, true)
                .put(PLACEHOLDER, "placeholdered")
                .put(MATRIX_ID, 2)
                .put(MATRIX_POSITION, 2)
                .put(ID, 2);
        JsonArray questions = new JsonArray();
        questions.add(tabQuestion)
                .add(tabQuestionNew);



        String expectedQuery = "[{\"action\":\"raw\",\"command\":\"BEGIN;\"}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + QUESTION_TABLE + " SET position = NULL, section_id = NULL, section_position = NULL, matrix_id = NULL, matrix_position = NULL WHERE id IN (?,?);\",\"values\":[1,2]}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + QUESTION_TABLE + " SET title = ?, position = ?, question_type = ?, statement = ?, mandatory = ?, section_id = ?, section_position = ?, conditional = ?, placeholder = ?, matrix_id = ?, matrix_position = ? WHERE id = ? RETURNING *;\",\"values\":[\"title\",null,9,\"statement\",false,1,1,false,\"placeholder\",1,1,1]}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + QUESTION_TABLE + " SET title = ?, position = ?, question_type = ?, statement = ?, mandatory = ?, section_id = ?, section_position = ?, conditional = ?, placeholder = ?, matrix_id = ?, matrix_position = ? WHERE id = ? RETURNING *;\",\"values\":[\"titled\",null,5,\"statemented\",true,2,2,true,\"placeholdered\",2,2,2]}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + FORM_TABLE + " SET date_modification = ? WHERE id = ?; \",\"values\":[\"NOW()\",\"form_id\"]}," +
                "{\"action\":\"raw\",\"command\":\"COMMIT;\"}]";


        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(TRANSACTION, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getJsonArray(STATEMENTS).toString());
            async.complete();
        });
        defaultQuestionService.update(FORM_ID, questions,null);
    }

    @Test
    public void testDelete(TestContext ctx){
        Async async = ctx.async();
        JsonObject tabQuestion = new JsonObject();
        tabQuestion.put(TITLE, TITLE)
                .put(POSITION, 1)
                .put(QUESTION_TYPE, 1)
                .put(STATEMENT, STATEMENT)
                .put(MANDATORY, false)
                .put(SECTION_ID, 1)
                .put(SECTION_POSITION,1)
                .put(CONDITIONAL, false)
                .put(MATRIX_ID, 1)
                .put(MATRIX_POSITION, 1)
                .put(FORM_ID, 1)
                .put(ID, 1);
        String expectedQuery = "DELETE FROM " + QUESTION_TABLE + " WHERE id = ?;";

        JsonArray expectedParams = new JsonArray().add(tabQuestion.getInteger(ID));

        String expectedQueryResult =  expectedQuery + getUpdateDateModifFormRequest();
        expectedParams.addAll(getParamsForUpdateDateModifFormRequest(tabQuestion.getInteger(FORM_ID).toString()));
        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQueryResult, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });
        defaultQuestionService.delete(tabQuestion, null);
    }

    @Test
    public void testDeleteFileQuestionsForForm(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "DELETE FROM " + QUESTION_TABLE + " WHERE form_id = ? AND question_type = ? RETURNING *;";
        JsonArray expectedParams = new JsonArray("[24,8]");

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getString(STATEMENT));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray(VALUES).toString());
            async.complete();
        });

        defaultQuestionService.deleteFileQuestionsForForm(24)
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testReorderQuestionsAfterDeletion_emptyQuestions(TestContext ctx) {
        Async async = ctx.async();
        List<Question> deletedQuestions = new ArrayList<>();

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(PREPARED, body.getString(ACTION));
            async.complete();
        });

        defaultQuestionService.reorderQuestionsAfterDeletion(24, deletedQuestions)
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }

    @Test
    public void testReorderQuestionsAfterDeletion_withQuestions(TestContext ctx) {
        Async async = ctx.async();
        Question question3 = new Question(question.toJson()).setSectionId(5L);
        List<Question> deletedQuestions = Arrays.asList(question, question, question3);

        String querySection =
                "WITH ranking_position AS ( " +
                        "SELECT *, RANK() OVER (PARTITION BY form_id ORDER BY section_position) AS rank " +
                        "FROM " + QUESTION_TABLE + " WHERE section_id = ? " +
                "), " +
                "nullify_questions AS ( " +
                    "UPDATE " + QUESTION_TABLE + " SET section_position = NULL, section_id = NULL WHERE section_id = ? " +
                ") " +
                "UPDATE " + QUESTION_TABLE + " q " +
                "SET section_position = (SELECT rank FROM ranking_position WHERE q.id = ranking_position.id), section_id = ? " +
                "WHERE section_id = ? " +
                "RETURNING *";

        // Reorder form elements in form
        String queryForm =
                "WITH ranking_position AS ( " +
                    "SELECT *, RANK() OVER (PARTITION BY form_id ORDER BY position, id_section, id_question) AS rank " +
                    "FROM ( " +
                        "SELECT form_id, id AS id_section, null AS id_question, position FROM " + SECTION_TABLE +
                        " WHERE form_id = ? " +
                        "UNION " +
                        "SELECT form_id, null AS id_section, id AS id_question, position FROM " + QUESTION_TABLE +
                        " WHERE form_id = ? AND position IS NOT NULL " +
                    ") AS elements " +
                "), " +
                "nullify_sections AS ( " +
                    "UPDATE " + SECTION_TABLE + " SET position = NULL WHERE form_id = ? " +
                "), " +
                "nullify_questions AS ( " +
                    "UPDATE " + QUESTION_TABLE + " SET position = NULL WHERE form_id = ? AND position IS NOT NULL " +
                ")," +
                "updated_sections AS ( " +
                    "UPDATE " + SECTION_TABLE + " s " +
                    "SET position = ( " +
                        "SELECT rank FROM ranking_position " +
                        "WHERE ranking_position.id_section IS NOT NULL AND s.id = ranking_position.id_section " +
                    ") " +
                    "WHERE form_id = ? " +
                    "RETURNING id, form_id, title, position, true AS is_section " +
                "), " +
                "updated_questions AS ( " +
                    "UPDATE " + QUESTION_TABLE + " q " +
                    "SET position = ( " +
                        "SELECT rank FROM ranking_position " +
                        "WHERE ranking_position.id_question IS NOT NULL AND q.id = ranking_position.id_question " +
                    ") " +
                    "WHERE form_id = ? " +
                    "RETURNING id, form_id, title, position, false AS is_section " +
                ") " +
                "SELECT * FROM updated_sections " +
                "UNION " +
                "SELECT * FROM updated_questions " +
                "ORDER BY form_id, position, id";

        String expectedQuery = "[" +
                "{" +
                    "\"action\":\"prepared\"," +
                    "\"statement\":\"" + querySection + "\"," +
                    "\"values\":[4,4,4,4]" +
                "}," +
                "{" +
                    "\"action\":\"prepared\"," +
                    "\"statement\":\"" + querySection + "\"," +
                    "\"values\":[5,5,5,5]" +
                "}," +
                "{\"action\":\"prepared\"," +
                    "\"statement\":\"" + queryForm + "\"," +
                    "\"values\":[24,24,24,24,24,24]" +
                "}" +
            "]";

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(TRANSACTION, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getJsonArray(STATEMENTS).toString());
            async.complete();
        });

        defaultQuestionService.reorderQuestionsAfterDeletion(24, deletedQuestions)
            .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }
}
