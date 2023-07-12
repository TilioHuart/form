package fr.openent.form.core.models;

import static fr.openent.form.core.constants.Fields.*;

import fr.openent.form.core.enums.FormElementTypes;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class Section extends FormElement implements IModel<Section> {
    private String description;
    private Long originalSectionId;
    private Long nextFormElementId;
    private FormElementTypes nextFormElementType;
    private Boolean isNextFormElementDefault;
    private List<Question> questions;


    // Constructors

    public Section() {
    }

    public Section(JsonObject section) {
        super(section);
        this.description = section.getString(DESCRIPTION, "");
        this.originalSectionId = section.getLong(ORIGINAL_SECTION_ID, null);
        this.nextFormElementId = section.getLong(NEXT_FORM_ELEMENT_ID, null);
        this.nextFormElementType = this.nextFormElementId == null ?
                null :
                FormElementTypes.getFormElementType(section.getString(NEXT_FORM_ELEMENT_TYPE, null));
        this.isNextFormElementDefault = section.getBoolean(IS_NEXT_FORM_ELEMENT_DEFAULT, false);

        if (section.getValue(QUESTIONS, null) instanceof JsonArray) {
            this.questions = new Question().toList(section.getJsonArray(QUESTIONS, null));
        }
        else if (section.getValue(QUESTIONS, null) instanceof JsonObject && section.getJsonObject(QUESTIONS, null).containsKey(ARR)) {
            this.questions = new Question().toList(section.getJsonObject(QUESTIONS, null).getJsonArray(ARR));
        }
        else if (section.getValue(QUESTIONS, null) instanceof JsonObject && section.getJsonObject(QUESTIONS, null).containsKey(ALL)) {
            this.questions = new Question().toList(section.getJsonObject(QUESTIONS, null).getJsonArray(ALL));
        }
    }


    // Getters

    public String getDescription() { return description; }

    public Long getOriginalSectionId() { return originalSectionId; }

    public Long getNextFormElementId() { return nextFormElementId; }

    public FormElementTypes getNextFormElementType() { return nextFormElementType; }

    public Boolean getIsNextFormElementDefault() { return isNextFormElementDefault; }

    public List<Question> getQuestions() { return questions; }


    // Setters

    public Section setDescription(String description) {
        this.description = description;
        return this;
    }

    public Section setOriginalSectionId(Long originalSectionId) {
        this.originalSectionId = originalSectionId;
        return this;
    }

    public Section setNextFormElementId(Long nextFormElementId) {
        this.nextFormElementId = nextFormElementId;
        return this;
    }

    public Section setNextFormElementType(FormElementTypes nextFormElementType) {
        this.nextFormElementType = nextFormElementType;
        return this;
    }

    public Section setIsNextFormElementDefault(Boolean isNextFormElementDefault) {
        this.isNextFormElementDefault = isNextFormElementDefault;
        return this;
    }

    public Section setQuestions(List<Question> questions) {
        this.questions = questions;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(FORM_ID, this.formId)
                .put(TITLE, this.title)
                .put(POSITION, this.position)
                .put(FORM_ELEMENT_TYPE, this.formElementType)
                .put(DESCRIPTION, this.description)
                .put(ORIGINAL_SECTION_ID, this.originalSectionId)
                .put(NEXT_FORM_ELEMENT_ID, this.nextFormElementId)
                .put(NEXT_FORM_ELEMENT_TYPE, this.nextFormElementType)
                .put(IS_NEXT_FORM_ELEMENT_DEFAULT, this.isNextFormElementDefault)
                .put(QUESTIONS, new Question().toJsonArray(questions));
    }

    @Override
    public Section model(JsonObject section){
        return new Section(section);
    }
}

