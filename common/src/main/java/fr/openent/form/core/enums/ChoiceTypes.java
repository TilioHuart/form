package fr.openent.form.core.enums;

import java.util.Arrays;

public enum ChoiceTypes {
  TXT("TXT"),
  IMAGE("IMAGE"),
  VIDEO("VIDEO");

  private final String name;

  ChoiceTypes(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public static ChoiceTypes getChoiceTypes(String value) {
     return Arrays.stream(ChoiceTypes.values())
             .filter(choiceTypes -> choiceTypes.getName().equals(value))
             .findFirst()
             .orElse(null);
  }
}