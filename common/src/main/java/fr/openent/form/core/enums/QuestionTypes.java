package fr.openent.form.core.enums;

import java.util.ArrayList;
import java.util.List;

public enum QuestionTypes {
  FREETEXT(1),
  SHORTANSWER(2),
  LONGANSWER(3),
  SINGLEANSWER(4),
  MULTIPLEANSWER(5),
  DATE(6),
  TIME(7),
  FILE(8),
  SINGLEANSWERRADIO(9),
  MATRIX(10);

  private final int code;

  QuestionTypes(int code) {
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }

  public static List<Integer> getGraphQuestions() {
    List<Integer> graphQuestions = new ArrayList<>();
    graphQuestions.add(QuestionTypes.SINGLEANSWER.getCode());
    graphQuestions.add(QuestionTypes.MULTIPLEANSWER.getCode());
    graphQuestions.add(QuestionTypes.SINGLEANSWERRADIO.getCode());
    graphQuestions.add(QuestionTypes.MATRIX.getCode());
    return graphQuestions;
  }

  public static List<Integer> getChoicesTypeQuestions() {
    return getGraphQuestions();
  }

  public static List<Integer> getConditionalQuestions() {
    List<Integer> conditionalQuestions = new ArrayList<>();
    conditionalQuestions.add(QuestionTypes.SINGLEANSWER.getCode());
    conditionalQuestions.add(QuestionTypes.MULTIPLEANSWER.getCode());
    conditionalQuestions.add(QuestionTypes.SINGLEANSWERRADIO.getCode());
    return conditionalQuestions;
  }

  public static List<Integer> getQuestionsWithoutResponses() {
    List<Integer> questionsWithoutResponses = new ArrayList<>();
    questionsWithoutResponses.add(QuestionTypes.FREETEXT.getCode());
    questionsWithoutResponses.add(QuestionTypes.MATRIX.getCode());
    return questionsWithoutResponses;
  }

  public static List<Integer> getMatrixChildQuestions() {
    List<Integer> questionsWithoutResponses = new ArrayList<>();
    questionsWithoutResponses.add(QuestionTypes.SINGLEANSWER.getCode());
    questionsWithoutResponses.add(QuestionTypes.SINGLEANSWERRADIO.getCode());
    return questionsWithoutResponses;
  }
}