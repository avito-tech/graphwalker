package org.graphwalker.core.model;

import java.util.Objects;

class AbstractNullableType implements CharSequence {
  final String value, name;

  public AbstractNullableType(String value, String name) {
    this.value = value;
    this.name = name;
  }

  @Override
  public int length() {
    return value.length();
  }

  @Override
  public char charAt(int i) {
    return value.charAt(i);
  }

  @Override
  public CharSequence subSequence(int i, int j) {
    return value.subSequence(i, j);
  }

  @Override
  public String toString() {
    return value != null ? name + "=\"" + value + "\" " : " ";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AbstractNullableType that = (AbstractNullableType) o;
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
