package fr.openent.form.core.enums;

import java.util.Arrays;

public enum ApiVersions {
  ONE_NINE("1.9"),
  TWO_ZERO("2.0");

  private final String value;

  ApiVersions(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }

  public static ApiVersions getApiVersions(String value) {
     return Arrays.stream(ApiVersions.values())
             .filter(apiVersions -> apiVersions.getValue().equals(value))
             .findFirst()
             .orElse(null);
  }

  public boolean isUnderOrEqual(String version) {
    return version != null && this.getValue().compareTo(version) <= 0;
  }

  public boolean isAfter(String version) {
    return version != null && this.getValue().compareTo(version) <= 0;
  }
}