package fr.openent.form.core.models;

import io.vertx.core.json.JsonObject;
import static fr.openent.form.core.constants.Fields.*;

public class Response implements IModel<Response> {
    private Number id;
    private Number questionId;
    private String answer;
    private String responderId;
    private Number choiceId;
    private Number distributionId;
    private Number originalId;
    private String customAnswer;


    // Constructors

    public Response() {}

    public Response(JsonObject response) {
        this.id = response.getNumber(ID, null);
        this.questionId = response.getNumber(QUESTION_ID,null);
        this.answer = response.getString(ANSWER,null);
        this.responderId = response.getString(RESPONDER_ID, null);
        this.choiceId = response.getNumber(CHOICE_ID, null);
        this.distributionId = response.getNumber(DISTRIBUTION_ID, null);
        this.originalId = response.getNumber(ORIGINAL_ID, null);
        this.customAnswer = response.getString(CUSTOM_ANSWER, null);
    }


    // Getters

    public Number getId() {return id; }

    public Number getQuestionId() { return questionId; }

    public String getAnswer() { return answer; }

    public String getResponderId() { return responderId; }

    public Number getChoiceId() { return choiceId; }

    public Number getDistributionId() { return distributionId; }

    public Number getOriginalId() { return originalId; }

    public String getCustomAnswer() { return customAnswer; }


    // Setters

    public Response setId(Number id) {
        this.id = id;
        return this;
    }

    public Response setQuestionId(Number questionId) {
        this.questionId = questionId;
        return this;
    }

    public Response setAnswer(String answer) {
        this.answer = answer;
        return this;
    }

    public Response setResponderId(String responderId) {
        this.responderId = responderId;
        return this;
    }

    public Response setChoiceId(Number choiceId) {
        this.choiceId = choiceId;
        return this;
    }

    public Response setDistributionId(Number distributionId) {
        this.distributionId = distributionId;
        return this;
    }

    public Response setOriginalId(Number originalId) {
        this.originalId = originalId;
        return this;
    }

    public Response setCustomAnswer(String customAnswer) {
        this.customAnswer = customAnswer;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(QUESTION_ID, this.questionId)
                .put(ANSWER, this.answer)
                .put(RESPONDER_ID, this.responderId)
                .put(CHOICE_ID, this.choiceId)
                .put(DISTRIBUTION_ID, this.distributionId)
                .put(ORIGINAL_ID, this.originalId)
                .put(CUSTOM_ANSWER, this.customAnswer);
    }

    @Override
    public Response model(JsonObject response){
        return new Response(response);
    }
}

