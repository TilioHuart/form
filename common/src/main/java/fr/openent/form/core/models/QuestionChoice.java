package fr.openent.form.core.models;

import static fr.openent.form.core.constants.Fields.*;
import io.vertx.core.json.JsonObject;

public class QuestionChoice implements Model<QuestionChoice> {
    private Number id;
    private Number questionId;
    private String value;
    private String type;
    private Number position;
    private Number nextSectionId;
    private Boolean isCustom;


    // Constructors

    public QuestionChoice() {
    }

    public QuestionChoice(JsonObject questionChoice) {
        this.id = questionChoice.getNumber(ID, null);
        this.questionId = questionChoice.getNumber(QUESTION_ID, null);
        this.value = questionChoice.getString(VALUE, null);
        this.type = questionChoice.getString(TYPE, null);
        this.position = questionChoice.getNumber(POSITION, null);
        this.nextSectionId = questionChoice.getNumber(NEXT_SECTION_ID, null);
        this.isCustom = questionChoice.getBoolean(IS_CUSTOM, null);
    }


    // Getters

    public Number getId() { return id; }

    public Number getQuestionId() { return questionId; }

    public String getValue() { return value; }

    public String getType() { return type; }

    public Number getPosition() { return position; }

    public Number getNextSectionId() { return nextSectionId; }

    public Boolean getIsCustom() { return isCustom; }


    // Setters

    public QuestionChoice setId(Number id) {
        this.id = id;
        return this;
    }

    public QuestionChoice setQuestionId(Number questionId) {
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

    public QuestionChoice setPosition(Number position) {
        this.position = position;
        return this;
    }

    public QuestionChoice setNextSectionId(Number nextSectionId) {
        this.nextSectionId = nextSectionId;
        return this;
    }

    public QuestionChoice setCustom(Boolean isCustom) {
        this.isCustom = isCustom;
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
                .put(NEXT_SECTION_ID, this.nextSectionId)
                .put(IS_CUSTOM, this.isCustom);
    }

    @Override
    public QuestionChoice model(JsonObject questionChoice){
        return new QuestionChoice(questionChoice);
    }
}

