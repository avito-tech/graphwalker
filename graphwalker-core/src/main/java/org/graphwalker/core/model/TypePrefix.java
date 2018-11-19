package org.graphwalker.core.model;

public enum TypePrefix {
  VOID("", void.class),
  STRING("(String)", String.class),
  NUMBER("(Number)", double.class),
  BOOLEAN("(Boolean)", boolean.class);

  final String value;
  final Class<?> typeClass;

  TypePrefix(String value, Class<?> typeClass) {
    this.value = value;
    this.typeClass = typeClass;
  }

  public Class<?> getTypeClass() {
    return typeClass;
  }

  @Override
  public String toString() {
    return value;
  }
}
