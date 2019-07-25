package org.graphwalker.core.model;

/*
 * #%L
 * GraphWalker Core
 * %%
 * Original work Copyright (c) 2005 - 2018 GraphWalker
 * Modified work Copyright (c) 2018 - 2019 Avito
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * @author Ivan Bonkin
 */
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
