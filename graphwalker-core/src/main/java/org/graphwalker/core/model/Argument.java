package org.graphwalker.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

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

  public String getQuotedValue() {
    return type == TypePrefix.STRING ? "\"" + value + "\"" : value;
  }

  public TypePrefix getType() {
    return type;
  }

  @Override
  public String toString() {
    return name + ": " + getQuotedValue();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Argument argument = (Argument) o;
    return type == argument.type &&
      Objects.equals(name, argument.name) &&
      Objects.equals(value, argument.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, name, value);
  }

  public static final java.util.List<Argument> EMPTY_LIST = Collections.unmodifiableList(new List());

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
