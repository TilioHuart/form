package fr.openent.form.core.models;

import static fr.openent.form.core.constants.Fields.*;
import io.vertx.core.json.JsonObject;

public abstract class FormElement {
    protected Number id;
    protected Number formId;
    protected String title;
    protected Number position;


    // Constructors

    public FormElement() {}

    public FormElement(JsonObject question) {
        this.id = question.getNumber(ID, null);
        this.formId = question.getNumber(FORM_ID, null);
        this.title = question.getString(TITLE, null);
        this.position = question.getNumber(POSITION, null);
    }


    // Getters

    public Number getId() { return id; }

    public Number getFormId() { return formId; }

    public String getTitle() { return title; }

    public Number getPosition() { return position; }


    // Setters

    public FormElement setId(Number id) {
        this.id = id;
        return this;
    }

    public FormElement setFormId(Number formId) {
        this.formId = formId;
        return this;
    }

    public FormElement setTitle(String title) {
        this.title = title;
        return this;
    }

    public FormElement setPosition(Number position) {
        this.position = position;
        return this;
    }

    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(FORM_ID, this.formId)
                .put(TITLE, this.title)
                .put(POSITION, this.position);
    }
}

