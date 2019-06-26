package org.graphwalker.core.model;

import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;


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
