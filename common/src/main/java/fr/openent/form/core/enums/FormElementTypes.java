package fr.openent.form.core.enums;

import java.util.Arrays;

public enum FormElementTypes {
  SECTION("SECTION"),
  QUESTION("QUESTION");

  private final String name;

  FormElementTypes(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public static FormElementTypes getFormElementType(String name) {
    return Arrays.stream(FormElementTypes.values())
            .filter(formElementType -> formElementType.getName().equals(name))
            .findFirst()
            .orElse(null);
  }
}