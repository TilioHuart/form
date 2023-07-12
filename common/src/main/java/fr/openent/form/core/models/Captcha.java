package fr.openent.form.core.models;

import static fr.openent.form.core.constants.Fields.*;
import io.vertx.core.json.JsonObject;

public class Captcha implements IModel<Captcha> {
    private Number id;
    private String question;
    private String answer;


    // Constructors

    public Captcha() {}

    public Captcha(JsonObject captcha) {
        this.id = captcha.getNumber(ID, null);
        this.question = captcha.getString(QUESTION, null);
        this.answer = captcha.getString(ANSWER, null);
    }


    // Getters

    public Number getId() { return id; }

    public String getQuestion() { return question; }

    public String getAnswer() { return answer; }


    // Setters

    public Captcha setId(Number id) {
        this.id = id;
        return this;
    }

    public Captcha setQuestion(String question) {
        this.question = question;
        return this;
    }

    public Captcha setAnswer(String answer) {
        this.answer = answer;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(QUESTION, this.question)
                .put(ANSWER, this.answer);
    }

    @Override
    public Captcha model(JsonObject captcha){
        return new Captcha(captcha);
    }
}

