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

import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

/**
 * @author Ivan Bonkin
 */
public class LineStyle {

  public static final LineStyle DEFAULT_EDGE_STYLE = new LineStyle(
    new LineType("line"),
    new LineColor("#000000"),
    1.0
  );

  private final LineType type;
  private final LineColor color;

  private final Property<Double> width;

  public LineStyle(LineType type, LineColor color, double width) {
    this.type = type;
    this.color = color;
    this.width = new Property<>(width, "width");
  }

  public LineType getType() {
    return type;
  }

  public LineColor getColor() {
    return color;
  }

  public Property<Double> getWidth() {
    return width;
  }

  @Override
  public String toString() {
    return "LineStyle{" + joinWithComma(type, color, width) + '}';
  }

  public static class LineColor extends AbstractNullableType implements CharSequence {
    final String value;

    public LineColor(String value) {
      super(value, "color");
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public static class LineType extends AbstractNullableType implements CharSequence {
    final String value;

    public LineType(String value) {
      super(value, "type");
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  private static String joinWithComma(Object ... objects) {
    return Stream.of(objects).map(Object::toString).filter(s -> !s.isEmpty()).collect(joining(", "));
  }

  public static class Property<T> extends AbstractNullableType implements CharSequence {
    final T value;

    public Property(T value, String name) {
      super(value != null ? value.toString() : null, name);
      this.value = value;
    }

    public T getValue() {
      return value;
    }
  }

}
