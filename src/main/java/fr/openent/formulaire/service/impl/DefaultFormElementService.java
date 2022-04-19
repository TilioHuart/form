package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.service.FormElementService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultFormElementService implements FormElementService {

    @Override
    public void countFormElements(String formId, Handler<Either<String, JsonObject>> handler) {
        String countQuestions = "SELECT COUNT(*) FROM " + Formulaire.QUESTION_TABLE + " WHERE form_id = ? AND section_id IS NULL";
        String countSections = "SELECT COUNT(*) FROM " + Formulaire.SECTION_TABLE + " WHERE form_id = ?";
        String query = "SELECT ((" + countQuestions + ") + (" + countSections + ")) AS count;";
        JsonArray params = new JsonArray().add(formId).add(formId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getTypeAndIdByPosition(String formId, String position, Handler<Either<String, JsonObject>> handler) {
        String getQuestions = "SELECT id, 'question' AS element_type FROM " + Formulaire.QUESTION_TABLE + " WHERE form_id = ? AND position = ?";
        String getSections = "SELECT id, 'section' AS element_type FROM " + Formulaire.SECTION_TABLE + " WHERE form_id = ? AND position = ?";
        String query = getQuestions + " UNION " + getSections + ";";
        JsonArray params = new JsonArray().add(formId).add(position).add(formId).add(position);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getByTypeAndId(String elementId, String elementType, Handler<Either<String, JsonObject>> handler) {
        String table = elementType.equals("question") ? Formulaire.QUESTION_TABLE : Formulaire.SECTION_TABLE;
        String query = "SELECT * FROM " + table + " WHERE id = ?;";
        JsonArray params = new JsonArray().add(elementId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
