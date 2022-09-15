package fr.openent.form.core.model;

import fr.openent.form.core.enums.ChoiceTypes;
import io.vertx.core.json.JsonObject;
import fr.openent.form.core.constants.Fields;

public class Question implements Model<Question> {
    private Integer questionType;
    private Number id;
    private Number questionId;
    private String value;
    private Number position;
    private Number nextSectionId;
    private Number nbResponses;
    private ChoiceTypes type;
    private Number code;
    private String name;
    private Number matrixId;
    private Number matrixPosition;

    public Question() {
    }

    public Question(JsonObject question) {
        this.questionType = question.getInteger(Fields.QUESTION_TYPE, null);
        this.id = question.getInteger(Fields.ID, null);
        this.name = question.getString(Fields.NAME, null);
        this.questionId = question.getNumber(Fields.QUESTION, null);
        this.value = question.getString(Fields.VALUE, null);
        this.position = question.getNumber(Fields.POSITION, null);
        this.nextSectionId = question.getNumber(Fields.NEXT_SECTION_ID, null);
        this.nbResponses = question.getNumber(Fields.NB_RESPONSES, null);
        this.code = question.getNumber(Fields.CODE, null);
        this.type = ChoiceTypes.getChoiceTypes(question.getString(Fields.TYPE, null));
        this.matrixId = question.getInteger(Fields.MATRIX_ID,null);
        this.matrixPosition = question.getInteger(Fields.MATRIX_POSITION,null);
    }

    public Integer getQuestionType() { return questionType; }

    public Number getId() { return id; }

    public Number getQuestionId() { return questionId; }

    public String getValue() { return value; }

    public Number getPosition() { return position; }

    public Number getNextSectionId() { return nextSectionId; }

    public Number getNbResponses() { return nbResponses; }

    public ChoiceTypes getType() { return type; }

    public Number getCode() { return code; }

    public String getName() { return name; }
    public Number getMatrixId() { return matrixId; }

    public Number getMatrixPosition() { return matrixPosition; }


    public Question setCode(Number code) {
        this.code = code;
        return this;
    }
    public Question setId(Number id) {
        this.id = id;
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
    public Question setName(String name) {
        this.name = name;
        return this;
    }
    public Question setNbResponses(Number nbResponses) {
        this.nbResponses = nbResponses;
        return this;
    }
    public Question setNextSectionId(Number nextSectionId) {
        this.nextSectionId = nextSectionId;
        return this;
    }
    public Question setPosition(Number position) {
        this.position = position;
        return this;
    }
    public Question setQuestionId(Number questionId) {
        this.questionId = questionId;
        return this;
    }
    public Question setQuestionType(Integer questionType) {
        this.questionType = questionType;
        return this;
    }
    public Question setType(ChoiceTypes type) {
        this.type = type;
        return this;
    }
    public Question setValue(String value) {
        this.value = value;
        return this;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put(Fields.QUESTION_TYPE, this.questionType )
                .put(Fields.ID, this.id)
                .put(Fields.QUESTION_ID, this.questionId)
                .put(Fields.VALUE, this.value)
                .put(Fields.POSITION, this.position)
                .put(Fields.NEXT_SECTION_ID, this.nextSectionId)
                .put(Fields.NB_RESPONSES, this.nbResponses)
                .put(Fields.TYPE, this.type)
                .put(Fields.CODE, this.code)
                .put(Fields.NAME, this.name)
                .put(Fields.MATRIX_ID, this.matrixId)
                .put(Fields.MATRIX_POSITION, this.matrixPosition);
    }
    @Override
    public Question model(JsonObject question){
        return new Question(question);
    }
}

