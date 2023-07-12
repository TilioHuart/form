package fr.openent.form.core.models;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import static fr.openent.form.core.constants.Fields.*;

import java.util.List;

public class Question extends FormElement implements IModel<Question> {
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
    private List<QuestionChoice> choices;
    private List<Question> children;


    // Constructors

    public Question() {
    }

    public Question(JsonObject question) {
        super(question);
        this.questionType = question.getInteger(QUESTION_TYPE, null);
        this.statement = question.getString(STATEMENT, "");
        this.mandatory = question.getBoolean(MANDATORY, false);
        this.originalQuestionId = question.getNumber(ORIGINAL_QUESTION_ID, null);
        this.sectionId = question.getNumber(SECTION_ID, null);
        this.sectionPosition = question.getNumber(SECTION_POSITION, null);
        this.conditional = question.getBoolean(CONDITIONAL, false);
        this.placeholder = question.getString(PLACEHOLDER, "");
        this.matrixId = question.getNumber(MATRIX_ID,null);
        this.matrixPosition = question.getNumber(MATRIX_POSITION,null);

        if (question.getValue(CHOICES, null) instanceof JsonArray) {
            this.choices = new QuestionChoice().toList(question.getJsonArray(CHOICES, null));
        }
        else if (question.getValue(CHOICES, null) instanceof JsonObject && question.getJsonObject(CHOICES, null).containsKey(ARR)) {
            this.choices = new QuestionChoice().toList(question.getJsonObject(CHOICES, null).getJsonArray(ARR));
        }
        else if (question.getValue(CHOICES, null) instanceof JsonObject && question.getJsonObject(CHOICES, null).containsKey(ALL)) {
            this.choices = new QuestionChoice().toList(question.getJsonObject(CHOICES, null).getJsonArray(ALL));
        }

        if (question.getValue(CHILDREN, null) instanceof JsonArray) {
            this.children = new Question().toList(question.getJsonArray(CHILDREN, null));
        }
        else if (question.getValue(CHILDREN, null) instanceof JsonObject && question.getJsonObject(CHILDREN, null).containsKey(ARR)) {
            this.children = new Question().toList(question.getJsonObject(CHILDREN, null).getJsonArray(ARR));
        }
        else if (question.getValue(CHILDREN, null) instanceof JsonObject && question.getJsonObject(CHILDREN, null).containsKey(ALL)) {
            this.children = new Question().toList(question.getJsonObject(CHILDREN, null).getJsonArray(ALL));
        }
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

    public List<QuestionChoice> getChoices() { return choices; }

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

    public Question setChoices(List<QuestionChoice> choices) {
        this.choices = choices;
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
                .put(FORM_ELEMENT_TYPE, this.formElementType)
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
                .put(CHOICES, new QuestionChoice().toJsonArray(this.choices))
                .put(CHILDREN, new Question().toJsonArray(this.children));
    }

    @Override
    public Question model(JsonObject question){
        return new Question(question);
    }
}

