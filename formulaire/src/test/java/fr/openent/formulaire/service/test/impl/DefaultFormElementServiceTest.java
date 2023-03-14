package fr.openent.formulaire.service.test.impl;

import fr.openent.form.core.models.FormElement;
import fr.openent.form.core.models.Question;
import fr.openent.form.core.models.Section;
import fr.openent.formulaire.service.impl.DefaultFormElementService;
import io.vertx.core.Vertx;
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
import static fr.openent.form.core.constants.Fields.QUESTION_TYPE;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.*;

@RunWith(VertxUnitRunner.class)
public class DefaultFormElementServiceTest {
    private Vertx vertx;
    private DefaultFormElementService defaultFormElementService;

    @Before
    public void setUp(){
        vertx = Vertx.vertx();
        defaultFormElementService = new DefaultFormElementService();
        Sql.getInstance().init(vertx.eventBus(), FORMULAIRE_ADDRESS);
    }

    @Test
    public void testUpdate_NoSectionsOrNoQuestions_shouldFail(TestContext ctx) {
        Async async = ctx.async(2);

        // Test no sections
        List<FormElement> formElementsQuestions = Collections.singletonList(new Question());
        String expectedFailureMessageQuestions = "[Formulaire@DefaultFormElementService::update] formElements must contain at least one section. " +
                "You should use questionService.update() instead.";

        defaultFormElementService.update(formElementsQuestions, "1")
                .onFailure(err -> {
                    ctx.assertEquals(expectedFailureMessageQuestions, err.getMessage());
                    async.countDown();
                });

        // Test no questions
        List<FormElement> formElementsSections = Collections.singletonList(new Section());
        String expectedFailureMessageSections = "[Formulaire@DefaultFormElementService::update] formElements must contain at least one question. " +
                "You should use sectionService.update() instead.";

        defaultFormElementService.update(formElementsSections, "1")
                .onFailure(err -> {
                    ctx.assertEquals(expectedFailureMessageSections, err.getMessage());
                    async.countDown();
                });

        async.awaitSuccess(10000);
    }

    @Test
    public void testUpdate(TestContext ctx) {
        Async async = ctx.async();
        List<FormElement> formElements = new ArrayList<>();

        JsonObject section = new JsonObject()
                .put(ID, 2)
                .put(FORM_ID, 1)
                .put(TITLE, TITLE)
                .put(POSITION, 1)
                .put(DESCRIPTION, DESCRIPTION)
                .put(ORIGINAL_SECTION_ID, 1);

        JsonObject question = new JsonObject()
                .put(ID, 2)
                .put(FORM_ID, 1)
                .put(TITLE, TITLE)
                .put(POSITION, 2)
                .put(QUESTION_TYPE, 2)
                .put(STATEMENT, STATEMENT)
                .put(MANDATORY, false)
                .put(ORIGINAL_SECTION_ID, 1)
                .put(SECTION_ID, (Integer) null)
                .put(SECTION_POSITION, (Integer) null)
                .put(CONDITIONAL, false)
                .put(PLACEHOLDER, PLACEHOLDER)
                .put(MATRIX_ID, (Integer) null)
                .put(MATRIX_POSITION, (Integer) null);

        formElements.add(new Section(section));
        formElements.add(new Question(question));

        // Get ids
        List<Number> sectionIds = Collections.singletonList(2);
        List<Number> questionIds = Collections.singletonList(2);

        // Sections and questions nullifying queries
        String nullifyingSectionQuery = "UPDATE " + SECTION_TABLE + " SET position = NULL WHERE id IN " + Sql.listPrepared(sectionIds) + ";";
        String nullifyingQuestionQuery =
                "UPDATE " + QUESTION_TABLE + " " +
                "SET position = NULL, section_id = NULL, section_position = NULL, matrix_id = NULL, matrix_position = NULL " +
                "WHERE id IN " + Sql.listPrepared(questionIds) + ";";

        // Sections and questions queries
        String updatingSectionQuery = "UPDATE " + SECTION_TABLE + " SET title = ?, description = ?, position = ? WHERE id = ? RETURNING *;";
        String updatingQuestionQuery =
                "UPDATE " + QUESTION_TABLE + " SET title = ?, position = ?, question_type = ?, statement = ?, mandatory = ?, " +
                "section_id = ?, section_position = ?, conditional = ?, placeholder = ?, matrix_id = ?, matrix_position = ? " +
                "WHERE id = ? RETURNING *;";

        String expectedQuery = "[{\"action\":\"raw\",\"command\":\"BEGIN;\"}," +
                "{\"action\":\"prepared\",\"statement\":\"" + nullifyingSectionQuery + "\",\"values\":[2]}," +
                "{\"action\":\"prepared\",\"statement\":\"" + nullifyingQuestionQuery + "\",\"values\":[2]}," +
                "{\"action\":\"prepared\",\"statement\":\"" + updatingSectionQuery + "\",\"values\":[\"title\",\"description\",1,2]}," +
                "{\"action\":\"prepared\",\"statement\":\"" + updatingQuestionQuery + "\",\"values\":[\"title\",2,2,\"statement\",false,null,null,false,\"placeholder\",null,null,2]}," +
                "{\"action\":\"prepared\",\"statement\":\"UPDATE " + FORM_TABLE + " SET date_modification = ? WHERE id = ?; \",\"values\":[\"NOW()\",\"1\"]}," +
                "{\"action\":\"raw\",\"command\":\"COMMIT;\"}]";

        vertx.eventBus().consumer(FORMULAIRE_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(TRANSACTION, body.getString(ACTION));
            ctx.assertEquals(expectedQuery, body.getJsonArray(STATEMENTS).toString());
            async.complete();
        });

        defaultFormElementService.update(formElements, "1")
                .onSuccess(result -> async.complete());

        async.awaitSuccess(10000);
    }
}
