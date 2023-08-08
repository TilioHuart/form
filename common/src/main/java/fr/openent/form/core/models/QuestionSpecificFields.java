package fr.openent.form.core.models;

import fr.openent.form.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

import static fr.openent.form.core.constants.Fields.*;

public class QuestionSpecificFields implements IModel<QuestionSpecificFields> {
    private Long id;
    private Long questionId;
    private Long cursorMinVal;
    private Long cursorMaxVal;
    private Long cursorStep;
    private String cursorMinLabel;
    private String cursorMaxLabel;


    // Constructors

    public QuestionSpecificFields() {
    }

    public QuestionSpecificFields(JsonObject questionSpecificFields) {
        this.id = questionSpecificFields.getLong(ID, null);
        this.questionId = questionSpecificFields.getLong(QUESTION_ID, null);
        this.cursorMinVal = questionSpecificFields.getLong(CURSOR_MIN_VAL,1L);
        this.cursorMaxVal = questionSpecificFields.getLong(CURSOR_MAX_VAL,10L);
        this.cursorStep = questionSpecificFields.getLong(CURSOR_STEP, 1L);
        this.cursorMinLabel = questionSpecificFields.getString(CURSOR_MIN_LABEL,"");
        this.cursorMaxLabel = questionSpecificFields.getString(CURSOR_MAX_LABEL,"");
    }


    // Getters

    public Long getId() { return id; }

    public Long getQuestionId() { return questionId; }

    public Long getCursorMinVal() { return cursorMinVal; }

    public Long getCursorMaxVal() { return cursorMaxVal; }

    public Long getCursorStep() { return cursorStep; }

    public String getCursorMinLabel() { return cursorMinLabel; }

    public String getCursorMaxLabel() { return cursorMaxLabel; }


    // Setters

    public QuestionSpecificFields setId(Long id) {
        this.id = id;
        return this;
    }

    public QuestionSpecificFields setQuestionId(Long questionId) {
        this.questionId = questionId;
        return this;
    }

    public QuestionSpecificFields setCursorMinVal(Long cursorMinVal) {
        this.cursorMinVal = cursorMinVal;
        return this;
    }

    public QuestionSpecificFields setCursorMaxVal(Long cursorMaxVal) {
        this.cursorMaxVal = cursorMaxVal;
        return this;
    }

    public QuestionSpecificFields setCursorStep(Long cursorStep) {
        this.cursorStep = cursorStep;
        return this;
    }

    public QuestionSpecificFields setCursorMinLabel(String cursorMinLabel) {
        this.cursorMinLabel = cursorMinLabel;
        return this;
    }

    public QuestionSpecificFields setCursorMaxLabel(String cursorMaxLabel) {
        this.cursorMaxLabel = cursorMaxLabel;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    /**
     * @deprecated Should instead use IModelHelper directly
     */
    @Deprecated
    @Override
    public QuestionSpecificFields model(JsonObject questionSpecificFields){
        return IModelHelper.toModel(questionSpecificFields, QuestionSpecificFields.class).orElse(null);
    }
}

