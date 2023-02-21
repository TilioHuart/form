package fr.openent.form.core.models;

import static fr.openent.form.core.constants.Fields.*;
import io.vertx.core.json.JsonObject;

public class FormShare implements Model<FormShare> {
    private String memberId;
    private Number resourceId;
    private String action;


    // Constructors

    public FormShare() {}

    public FormShare(JsonObject formShare) {
        this.memberId = formShare.getString(MEMBER_ID, null);
        this.resourceId = formShare.getNumber(RESOURCE_ID, null);
        this.action = formShare.getString(ACTION, null);;
    }


    // Getters

    public String getMemberId() { return memberId; }

    public Number getResourceId() { return resourceId; }

    public String getAction() { return action; }


    // Setters

    public FormShare setMemberId(String memberId) {
        this.memberId = memberId;
        return this;
    }

    public FormShare setResourceId(Number resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    public FormShare setAction(String action) {
        this.action = action;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(MEMBER_ID, this.memberId)
                .put(RESOURCE_ID, this.resourceId)
                .put(ACTION, this.action);
    }

    @Override
    public FormShare model(JsonObject formShare){
        return new FormShare(formShare);
    }
}

