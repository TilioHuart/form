package fr.openent.form.core.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum RgpdLifetimes {
  THREE(3),
  SIX(6),
  NINE(9),
  TWELVE(12);

  private final Integer value;

  RgpdLifetimes(Integer value) {
    this.value = value;
  }

  public Integer getValue() {
    return this.value;
  }

  public static List<Integer> getAllValues() {
    return Arrays.stream(RgpdLifetimes.values())
            .map(RgpdLifetimes::getValue)
            .collect(Collectors.toList());
  }

  public static RgpdLifetimes getRgpdLifetimes(Integer value) {
     return Arrays.stream(RgpdLifetimes.values())
             .filter(choiceTypes -> choiceTypes.getValue().equals(value))
             .findFirst()
             .orElse(null);
  }
}