package fr.openent.formulaire.helpers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.enums.FormElementTypes.SECTION;

public class ApiVersionHelper {
    private static final Logger log = LoggerFactory.getLogger(ApiVersionHelper.class);

    private ApiVersionHelper() {}

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
