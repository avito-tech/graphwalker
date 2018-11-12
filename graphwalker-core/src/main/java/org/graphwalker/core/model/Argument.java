package org.graphwalker.core.model;

public final class Argument {

  private final TypePrefix type;

  private final String name, value;

  public Argument(TypePrefix type, String name, String value) {
    this.type = type;
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public TypePrefix getType() {
    return type;
  }

  public enum TypePrefix {
    STRING, NUMBER, BOOLEAN;
  }

}
