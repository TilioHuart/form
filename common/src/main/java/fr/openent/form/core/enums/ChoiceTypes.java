package fr.openent.form.core.enums;

import java.util.Arrays;

public enum ChoiceTypes {
  TXT("TXT"),
  IMAGE("IMAGE"),
  VIDEO("VIDEO");

  private final String value;

  ChoiceTypes(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }

  public static ChoiceTypes getChoiceTypes(String value) {
     return Arrays.stream(ChoiceTypes.values())
             .filter(choiceTypes -> choiceTypes.getValue().equals(value))
             .findFirst()
             .orElse(null);
  }
}