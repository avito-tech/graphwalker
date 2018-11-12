package org.graphwalker.core.model;

public enum TypePrefix {
  VOID(""), STRING("(String)"), NUMBER("(Number)"), BOOLEAN("(Boolean)");

  final String value;

  TypePrefix(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}
