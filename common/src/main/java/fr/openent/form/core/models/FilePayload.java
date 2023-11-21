package fr.openent.form.core.models;

import fr.openent.form.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

import static fr.openent.form.core.constants.Fields.*;

public class FilePayload implements IModel<FilePayload> {
    private String file;
    private Integer responseId;


    // Constructors

    public FilePayload() {}

    public FilePayload(JsonObject responseFile) {
        this.file = responseFile.getString(FILE, null);
        this.responseId = responseFile.getInteger(RESPONSE_ID,null);
    }


    // Getters

    public String getFile() { return file; }

    public Integer getResponseId() { return responseId; }


    // Setters


    public FilePayload setId(String file) {
        this.file = file;
        return this;
    }

    public FilePayload setResponseId(Integer responseId) {
        this.responseId = responseId;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    @Override
    public FilePayload model(JsonObject filePayload){
        return new FilePayload(filePayload);
    }
}

