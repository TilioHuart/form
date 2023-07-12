package fr.openent.form.core.models;

import io.vertx.core.json.JsonObject;

import static fr.openent.form.core.constants.Fields.*;

public class QuestionType implements IModel<QuestionType> {
    private Number id;
    private Number code;
    private String name;


    // Constructors

    public QuestionType() {}

    public QuestionType(JsonObject questionType) {
        this.id = questionType.getNumber(ID, null);
        this.code = questionType.getNumber(CODE, null);
        this.name = questionType.getString(NAME, null);;
    }


    // Getters

    public Number getId() { return id; }

    public Number getCode() { return code; }

    public String getName() { return name; }


    // Setters

    public QuestionType setMemberId(Number id) {
        this.id = id;
        return this;
    }

    public QuestionType setResourceId(Number code) {
        this.code = code;
        return this;
    }

    public QuestionType setAction(String name) {
        this.name = name;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(CODE, this.code)
                .put(NAME, this.name);
    }

    @Override
    public QuestionType model(JsonObject questionType){
        return new QuestionType(questionType);
    }
}

