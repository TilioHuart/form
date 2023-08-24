package fr.openent.formulaire.helpers;

import fr.openent.form.core.models.Question;
import fr.openent.form.core.models.QuestionSpecificFields;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.enums.FormElementTypes.SECTION;

public class ApiVersionHelper {
    private static final Logger log = LoggerFactory.getLogger(ApiVersionHelper.class);

    private ApiVersionHelper() {}

    // Version 2

    public static JsonArray formatQuestions(JsonArray questions) {
        return questions.stream()
            .map(Question.class::cast)
            .map(ApiVersionHelper::formatQuestion)
            .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
    }

    public static JsonObject formatQuestion(Question question) {
        JsonObject jsonQuestion = question.toJson();
        QuestionSpecificFields specifics = question.getSpecificFields();
        jsonQuestion.put(CURSOR_MIN_VAL, specifics != null ? specifics.getCursorMinVal() : null);
        jsonQuestion.put(CURSOR_MAX_VAL, specifics != null ? specifics.getCursorMaxVal() : null);
        jsonQuestion.put(CURSOR_STEP, specifics != null ? specifics.getCursorStep() : null);
        jsonQuestion.put(CURSOR_MIN_LABEL, specifics != null ? specifics.getCursorMinLabel() : null);
        jsonQuestion.put(CURSOR_MAX_LABEL, specifics != null ? specifics.getCursorMaxLabel() : null);
        jsonQuestion.remove(SPECIFIC_FIELDS);
        return jsonQuestion;
    }

    // Version 1.9

    public static void convertToNextSectionId(JsonObject questionChoice) {
        questionChoice.put(NEXT_SECTION_ID, QUESTION.equals(questionChoice.getString(NEXT_FORM_ELEMENT_TYPE)) ?
                null : questionChoice.getInteger(NEXT_FORM_ELEMENT_ID));
        questionChoice.remove(NEXT_FORM_ELEMENT_ID);
        questionChoice.remove(NEXT_FORM_ELEMENT_TYPE);
    }

    public static void convertToNextSectionId(JsonArray questionChoices) {
        for (int i = 0; i < questionChoices.size(); i++) {
            JsonObject questionChoice = questionChoices.getJsonObject(i);
            convertToNextSectionId(questionChoice);
        }
    }

    public static void convertToNextFormElementId(JsonObject questionChoice) {
        questionChoice.put(NEXT_FORM_ELEMENT_ID, questionChoice.getInteger(NEXT_SECTION_ID));
        questionChoice.put(NEXT_FORM_ELEMENT_TYPE, SECTION);
        questionChoice.remove(NEXT_SECTION_ID);
    }

    public static void convertToNextFormElementId(JsonArray questionChoices) {
        for (int i = 0; i < questionChoices.size(); i++) {
            JsonObject questionChoice = questionChoices.getJsonObject(i);
            convertToNextFormElementId(questionChoice);
        }
    }
}
