package fr.openent.form.core.models;

import io.vertx.core.json.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static fr.openent.form.core.constants.Dates.YYYY_MM_DD_T_HH_MM_SS_SSS;
import static fr.openent.form.core.constants.Fields.*;

public class ResponseFile implements Model<ResponseFile> {
    private String id;
    private Number responseId;
    private String filename;
    private String type;


    // Constructors

    public ResponseFile() {}

    public ResponseFile(JsonObject responseFile) {
        this.id = responseFile.getString(ID, null);
        this.responseId = responseFile.getNumber(RESPONSE_ID,null);
        this.filename = responseFile.getString(FILENAME,null);
        this.type = responseFile.getString(TYPE, null);
    }


    // Getters

    public String getId() { return id; }

    public Number getResponseId() { return responseId; }

    public String getFilename() { return filename; }

    public String getType() { return type; }


    // Setters


    public ResponseFile setId(String id) {
        this.id = id;
        return this;
    }

    public ResponseFile setResponseId(Number responseId) {
        this.responseId = responseId;
        return this;
    }

    public ResponseFile setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public ResponseFile setType(String type) {
        this.type = type;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(RESPONSE_ID, this.responseId)
                .put(FILENAME, this.filename)
                .put(TYPE, this.type);
    }

    @Override
    public ResponseFile model(JsonObject responseFile){
        return new ResponseFile(responseFile);
    }
}

