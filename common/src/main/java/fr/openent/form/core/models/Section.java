package fr.openent.form.core.models;

import static fr.openent.form.core.constants.Fields.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class Section extends FormElement implements Model<Section> {
    private String description;
    private Number originalSectionId;
    private List<Question> questions;


    // Constructors

    public Section() {
    }

    public Section(JsonObject section) {
        super(section);
        this.description = section.getString(DESCRIPTION, null);
        this.originalSectionId = section.getNumber(ORIGINAL_SECTION_ID, null);
        this.questions = new Question().toList(section.getJsonArray(QUESTIONS, null));
    }


    // Getters

    public String getDescription() { return description; }

    public Number getOriginalSectionId() { return originalSectionId; }

    public List<Question> getQuestions() { return questions; }


    // Setters

    public Section setDescription(String description) {
        this.description = description;
        return this;
    }

    public Section setOriginalSectionId(Number originalSectionId) {
        this.originalSectionId = originalSectionId;
        return this;
    }

    public Section setQuestions(List<Question> questions) {
        this.questions = questions;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(FORM_ID, this.formId)
                .put(TITLE, this.title)
                .put(POSITION, this.position)
                .put(DESCRIPTION, this.description)
                .put(ORIGINAL_SECTION_ID, this.originalSectionId)
                .put(QUESTIONS, new Question().toJsonArray(questions));
    }

    @Override
    public Section model(JsonObject section){
        return new Section(section);
    }
}

