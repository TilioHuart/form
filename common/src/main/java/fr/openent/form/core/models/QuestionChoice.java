package fr.openent.form.core.models;

import static fr.openent.form.core.constants.Fields.*;

import fr.openent.form.core.enums.FormElementTypes;
import io.vertx.core.json.JsonObject;

public class QuestionChoice implements Model<QuestionChoice> {
    private Long id;
    private Long questionId;
    private String value;
    private String type;
    private Long position;
    private Long nextFormElementId;
    private FormElementTypes nextFormElementType;
    private Boolean isNextFormElementDefault;
    private Boolean isCustom;
    private String image;


    // Constructors

    public QuestionChoice() {
    }

    public QuestionChoice(JsonObject questionChoice) {
        this.id = questionChoice.getLong(ID, null);
        this.questionId = questionChoice.getLong(QUESTION_ID, null);
        this.value = questionChoice.getString(VALUE, null);
        this.type = questionChoice.getString(TYPE, null);
        this.position = questionChoice.getLong(POSITION, null);
        this.nextFormElementId = questionChoice.getLong(NEXT_FORM_ELEMENT_ID, null);
        this.nextFormElementType = this.nextFormElementId == null ?
                null :
                FormElementTypes.getFormElementType(questionChoice.getString(NEXT_FORM_ELEMENT_TYPE, null));
        this.isNextFormElementDefault = questionChoice.getBoolean(IS_NEXT_FORM_ELEMENT_DEFAULT, false);
        this.isCustom = questionChoice.getBoolean(IS_CUSTOM, false);
        this.image = questionChoice.getString(IMAGE, null);
    }


    // Getters

    public Long getId() { return id; }

    public Long getQuestionId() { return questionId; }

    public String getValue() { return value; }

    public String getType() { return type; }

    public Long getPosition() { return position; }

    public Long getNextFormElementId() { return nextFormElementId; }

    public FormElementTypes getNextFormElementType() { return nextFormElementType; }

    public Boolean getIsNextFormElementDefault() { return isNextFormElementDefault; }

    public Boolean getIsCustom() { return isCustom; }

    public String getImage() { return image; }


    // Setters

    public QuestionChoice setId(Long id) {
        this.id = id;
        return this;
    }

    public QuestionChoice setQuestionId(Long questionId) {
        this.questionId = questionId;
        return this;
    }

    public QuestionChoice setValue(String value) {
        this.value = value;
        return this;
    }

    public QuestionChoice setType(String type) {
        this.type = type;
        return this;
    }

    public QuestionChoice setPosition(Long position) {
        this.position = position;
        return this;
    }

    public QuestionChoice setNextFormElementId(Long nextFormElementId) {
        this.nextFormElementId = nextFormElementId;
        return this;
    }

    public QuestionChoice setNextFormElementType(FormElementTypes nextFormElementType) {
        this.nextFormElementType = nextFormElementType;
        return this;
    }

    public QuestionChoice setIsNextFormElementDefault(Boolean isNextFormElementDefault) {
        this.isNextFormElementDefault = isNextFormElementDefault;
        return this;
    }

    public QuestionChoice setCustom(Boolean isCustom) {
        this.isCustom = isCustom;
        return this;
    }

    public QuestionChoice setImage(String image) {
        this.image = image;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(QUESTION_ID, this.questionId)
                .put(VALUE, this.value)
                .put(TYPE, this.type)
                .put(POSITION, this.position)
                .put(NEXT_FORM_ELEMENT_ID, this.nextFormElementId)
                .put(NEXT_FORM_ELEMENT_TYPE, this.nextFormElementType)
                .put(IS_NEXT_FORM_ELEMENT_DEFAULT, this.isNextFormElementDefault)
                .put(IS_CUSTOM, this.isCustom)
                .put(IMAGE, this.image);
    }

    @Override
    public QuestionChoice model(JsonObject questionChoice){
        return new QuestionChoice(questionChoice);
    }
}

