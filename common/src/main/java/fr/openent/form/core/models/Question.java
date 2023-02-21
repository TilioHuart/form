package fr.openent.form.core.models;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import static fr.openent.form.core.constants.Fields.*;

import java.util.List;

public class Question extends FormElement implements Model<Question> {
    private Integer questionType;
    private String statement;
    private Boolean mandatory;
    private Number originalQuestionId;
    private Number sectionId;
    private Number sectionPosition;
    private Boolean conditional;
    private String placeholder;
    private Number matrixId;
    private Number matrixPosition;
    private List<QuestionChoice> questionChoices;
    private List<Question> children;


    // Constructors

    public Question() {
    }

    public Question(JsonObject question) {
        super(question);
        this.questionType = question.getInteger(QUESTION_TYPE, null);
        this.statement = question.getString(STATEMENT, null);
        this.mandatory = question.getBoolean(MANDATORY, null);
        this.originalQuestionId = question.getNumber(ORIGINAL_QUESTION_ID, null);
        this.sectionId = question.getNumber(SECTION_ID, null);
        this.sectionPosition = question.getNumber(SECTION_POSITION, null);
        this.conditional = question.getBoolean(CONDITIONAL, null);
        this.placeholder = question.getString(PLACEHOLDER, null);
        this.matrixId = question.getNumber(MATRIX_ID,null);
        this.matrixPosition = question.getNumber(MATRIX_POSITION,null);
        this.questionChoices = new QuestionChoice().toList(question.getJsonArray(QUESTION_CHOICES, null));
        this.children = new Question().toList(question.getJsonArray(CHILDREN, null));
    }


    // Getters

    public Integer getQuestionType() { return questionType; }

    public String getStatement() { return statement; }

    public Boolean getMandatory() { return mandatory; }

    public Number getOriginalQuestionId() { return originalQuestionId; }

    public Number getSectionId() { return sectionId; }

    public Number getSectionPosition() { return sectionPosition; }

    public Boolean getConditional() { return conditional; }

    public String getPlaceholder() { return placeholder; }

    public Number getMatrixId() { return matrixId; }

    public Number getMatrixPosition() { return matrixPosition; }

    public List<QuestionChoice> getQuestionChoices() { return questionChoices; }

    public List<Question> getChildren() { return children; }


    // Setters

    public Question setQuestionType(Integer questionType) {
        this.questionType = questionType;
        return this;
    }

    public Question setStatement(String statement) {
        this.statement = statement;
        return this;
    }

    public Question setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
        return this;
    }

    public Question setOriginalQuestionId(Number originalQuestionId) {
        this.originalQuestionId = originalQuestionId;
        return this;
    }

    public Question setSectionId(Number sectionId) {
        this.sectionId = sectionId;
        return this;
    }

    public Question setSectionPosition(Number sectionPosition) {
        this.sectionPosition = sectionPosition;
        return this;
    }

    public Question setConditional(Boolean conditional) {
        this.conditional = conditional;
        return this;
    }

    public Question setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    public Question setMatrixId(Number matrixId) {
        this.matrixId = matrixId;
        return this;
    }

    public Question setMatrixPosition(Number matrixPosition) {
        this.matrixPosition = matrixPosition;
        return this;
    }

    public Question setQuestionChoices(List<QuestionChoice> questionChoices) {
        this.questionChoices = questionChoices;
        return this;
    }

    public Question setChildren(List<Question> children) {
        this.children = children;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(FORM_ID, this.formId)
                .put(TITLE, this.title)
                .put(POSITION, this.position)
                .put(QUESTION_TYPE, this.questionType)
                .put(STATEMENT, this.statement)
                .put(MANDATORY, this.mandatory)
                .put(ORIGINAL_QUESTION_ID, this.originalQuestionId)
                .put(SECTION_ID, this.sectionId)
                .put(SECTION_POSITION, this.sectionPosition)
                .put(CONDITIONAL, this.conditional)
                .put(PLACEHOLDER, this.placeholder)
                .put(MATRIX_ID, this.matrixId)
                .put(MATRIX_POSITION, this.matrixPosition)
                .put(QUESTION_CHOICES, new QuestionChoice().toJsonArray(this.questionChoices))
                .put(CHILDREN, new Question().toJsonArray(this.children));
    }

    @Override
    public Question model(JsonObject question){
        return new Question(question);
    }
}

