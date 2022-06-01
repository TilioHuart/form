package fr.openent.form.core;

public enum Events {
  ACCESS("ACCESS"),
  CREATE("CREATE");

  private final String name;

  Events(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}