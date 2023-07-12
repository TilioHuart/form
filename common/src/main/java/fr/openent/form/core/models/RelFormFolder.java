package fr.openent.form.core.models;

import io.vertx.core.json.JsonObject;

import static fr.openent.form.core.constants.Fields.*;

public class RelFormFolder implements IModel<RelFormFolder> {
    private String userId;
    private Number formId;
    private Number folderId;


    // Constructors

    public RelFormFolder() {}

    public RelFormFolder(JsonObject relFormFolder) {
        this.userId = relFormFolder.getString(USER_ID, null);
        this.formId = relFormFolder.getNumber(FORM_ID, null);
        this.folderId = relFormFolder.getNumber(FOLDER_ID, null);
    }


    // Getters

    public String getUserId() { return userId; }

    public Number getFormId() { return formId; }

    public Number getFolderId() { return folderId; }


    // Setters

    public RelFormFolder setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public RelFormFolder setFormId(Number formId) {
        this.formId = formId;
        return this;
    }

    public RelFormFolder setFolderId(Number folderId) {
        this.folderId = folderId;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(USER_ID, this.userId)
                .put(FORM_ID, this.formId)
                .put(FOLDER_ID, this.folderId);
    }

    @Override
    public RelFormFolder model(JsonObject relFormFolder){
        return new RelFormFolder(relFormFolder);
    }
}

