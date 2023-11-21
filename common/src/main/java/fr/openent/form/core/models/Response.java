package fr.openent.form.core.models;

import io.vertx.core.json.JsonObject;
import static fr.openent.form.core.constants.Fields.*;

public class Response implements IModel<Response> {
    private Long id;
    private Long questionId;
    private String answer;
    private String responderId;
    private Long choiceId;
    private Long distributionId;
    private Long originalId;
    private String customAnswer;
    private Long choicePosition;
    private String image;


    // Constructors

    public Response() {}

    public Response(JsonObject response) {
        this.id = response.getLong(ID, null);
        this.questionId = response.getLong(QUESTION_ID,null);
        this.answer = response.getString(ANSWER,null);
        this.responderId = response.getString(RESPONDER_ID, null);
        this.choiceId = response.getLong(CHOICE_ID, null);
        this.distributionId = response.getLong(DISTRIBUTION_ID, null);
        this.originalId = response.getLong(ORIGINAL_ID, null);
        this.customAnswer = response.getString(CUSTOM_ANSWER, null);
        this.choicePosition = response.getLong(CHOICE_POSITION, null);
        this.image = response.getString(IMAGE, null);
    }


    // Getters

    public Long getId() {return id; }

    public Long getQuestionId() { return questionId; }

    public String getAnswer() { return answer; }

    public String getResponderId() { return responderId; }

    public Long getChoiceId() { return choiceId; }

    public Long getDistributionId() { return distributionId; }

    public Long getOriginalId() { return originalId; }

    public String getCustomAnswer() { return customAnswer; }

    public Long getChoicePosition() { return choicePosition; }

    public String getImage() { return image; }


    // Setters

    public Response setId(Long id) {
        this.id = id;
        return this;
    }

    public Response setQuestionId(Long questionId) {
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

    public Response setChoiceId(Long choiceId) {
        this.choiceId = choiceId;
        return this;
    }

    public Response setDistributionId(Long distributionId) {
        this.distributionId = distributionId;
        return this;
    }

    public Response setOriginalId(Long originalId) {
        this.originalId = originalId;
        return this;
    }

    public Response setCustomAnswer(String customAnswer) {
        this.customAnswer = customAnswer;
        return this;
    }

    public Response setChoicePosition(Long choicePosition) {
        this.choicePosition = choicePosition;
        return this;
    }

    public Response setImage(String image) {
        this.image = image;
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
                .put(CUSTOM_ANSWER, this.customAnswer)
                .put(CHOICE_POSITION, this.choicePosition)
                .put(IMAGE, this.image);
    }

    @Override
    public Response model(JsonObject response){
        return new Response(response);
    }
}

