package org.graphwalker.core.model;

import java.util.ArrayList;

import static java.util.stream.Collectors.joining;

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

  @Override
  public String toString() {
    if (type == TypePrefix.STRING) {
      return name + ": \"" + value + "\"";
    }
    return name + ": " + value + "";
  }

  public static class List extends ArrayList<Argument> implements Comparable<List> {

    @Override
    public String toString() {
      return stream().map(Argument::toString).collect(joining(", ", "{", "}"));
    }

    @Override
    public int compareTo(List arguments) {
      return this.toString().compareTo(arguments.toString());
    }
  }

}
