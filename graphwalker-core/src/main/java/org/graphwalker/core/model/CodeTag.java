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

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.graphwalker.core.model.TypePrefix.*;

/**
 * <code>@code</code> tag in description block.
 *
 * @author Ivan Bonkin
 */
public class CodeTag {

  public interface Expression<T> {

    Expression<T> copy();
  }

  public static class Value<T> implements Expression<T> {

    private final T result;

    public Value(T result) {
      this.result = result;
    }

    public T result() {
      return result;
    }

    @Override
    public String toString() {
      return result instanceof String ? "\"" + result + "\"" : result.toString();
    }

    @Override
    public Value<T> copy() {
      return new Value<T>(result);
    }
  }

  public static class DatasetVariable<T> implements Expression<T> {

    private final T result;

    public DatasetVariable(T result) {
      this.result = result;
    }

    public T result() {
      return result;
    }

    @Override
    public String toString() {
      return result.toString();
    }

    @Override
    public DatasetVariable<T> copy() {
      return new DatasetVariable<T>(result);
    }
  }

  public static class TypedDatasetVariable<T> extends DatasetVariable<T> {

    private final TypePrefix typePrefix;

    public TypedDatasetVariable(T result, TypePrefix typePrefix) {
      super(result);
      this.typePrefix = typePrefix;
    }

    public TypePrefix getTypePrefix() {
      return typePrefix;
    }
  }

  public static abstract class AbstractMethod {

    final TypePrefix typePrefix;
    final String name;

    final List<Expression> arguments;

    public abstract AbstractMethod copy();

    AbstractMethod(TypePrefix typePrefix, String name, List<Expression> arguments) {
      Objects.requireNonNull(name, "Method name should be initialized");
      this.typePrefix = typePrefix;
      this.name = name;
      this.arguments = arguments;
    }

    public TypePrefix getTypePrefix() {
      return typePrefix;
    }

    public String getName() {
      return name;
    }

    public List<Expression> getArguments() {
      return arguments;
    }

    public String asJavaMethodCall() {
      String asYedScript = toString();
      for (TypePrefix prefix : values()) {
        if (!prefix.value.isEmpty()) {
          asYedScript = asYedScript.replace(prefix.value, "");
        }
      }
      return asYedScript + ";";
    }

    @Override
    public String toString() {
      return String.format("%s%s(%s)",
        typePrefix, name, arguments.stream().map(Expression::toString).collect(joining(", ")));
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) return true;
      if (other == null || getClass() != other.getClass()) return false;
      AbstractMethod that = (AbstractMethod) other;
      return typePrefix == that.typePrefix &&
        Objects.equals(name, that.name) &&
        Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
      return Objects.hash(typePrefix, name, arguments);
    }
  }

  public static class VoidMethod extends AbstractMethod implements Expression<Void> {

    public VoidMethod(String name, List<Expression> arguments) {
      super(VOID, name, arguments);
    }

    @Override
    public VoidMethod copy() {
      return new VoidMethod(name, arguments.stream().map(Expression::copy).collect(toList()));
    }
  }

  public static class BooleanMethod extends AbstractMethod implements Expression<Boolean> {

    public BooleanMethod(String name, List<Expression> arguments) {
      super(BOOLEAN, name, arguments);
    }

    @Override
    public BooleanMethod copy() {
      return new BooleanMethod(name, arguments.stream().map(Expression::copy).collect(toList()));
    }
  }

  public static class StringMethod extends AbstractMethod implements Expression<String> {

    public StringMethod(String name, List<Expression> arguments) {
      super(STRING, name, arguments);
    }

    @Override
    public StringMethod copy() {
      return new StringMethod(name, arguments.stream().map(Expression::copy).collect(toList()));
    }
  }

  public static class DoubleMethod extends AbstractMethod implements Expression<Double> {

    public DoubleMethod(String name, List<Expression> arguments) {
      super(NUMBER, name, arguments);
    }

    @Override
    public DoubleMethod copy() {
      return new DoubleMethod(name, arguments.stream().map(Expression::copy).collect(toList()));
    }
  }

  private final AbstractMethod method;

  /**
   * @param method only Boolean/Void methods accepted
   */
  public CodeTag(Expression method) {
    if (!(method instanceof VoidMethod || method instanceof BooleanMethod)) {
      throw new IllegalArgumentException("Method " + method + "can not be a root expression, only boolean or void methods are valid");
    }
    this.method = (AbstractMethod) method;
  }

  public AbstractMethod getMethod() {
    return method;
  }

  @Override
  public String toString() {
    return "@code " + method + ";";
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    return Objects.equals(method, ((CodeTag) other).method);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method);
  }
}
