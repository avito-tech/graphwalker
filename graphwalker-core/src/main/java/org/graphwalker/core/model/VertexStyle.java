package org.graphwalker.core.model;


import java.awt.Color;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

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
    return value != null ? name + "=\"" + value + "\"" : "";
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

public class VertexStyle {

  public static final VertexStyle DEFAULT_VERTEX_STYLE = new VertexStyle(
    new Configuration("BevelNode2"),
    new Geometry(250, 100, 0, 0),
    new Fill("#00FF00", "#00FF00"),
    new Border("#030303", new LineType("line"), 1.0),
    new Label(
      new Geometry(100, 20, 0, 0),
      new Alignment("center"),
      new FontFamily("Dialog"),
      new FontStyle("plain"),
      (short) 12,
      new TextColor("#000000"),
      null, null)
  );

  public static final VertexStyle SCALED_VERTEX_STYLE = new VertexStyle(
    new Configuration("BevelNode2"),
    new Geometry(450, 200, 0, 0),
    new Fill("#00FF00", "#00FF33"),
    new Border("#030303", new LineType("line"), 1.0),
    new Label(
      new Geometry(100, 20, 0, 0),
      new Alignment("left"),
      new FontFamily("Dialog"),
      new FontStyle("plain"),
      (short) 12,
      new TextColor("#000000"),
      null, null)
  );

  private final Configuration configuration;

  private final Geometry geometry;

  private final Fill fill;

  private final Border border;

  private final Label label;

  public VertexStyle(Configuration configuration, Geometry geometry, Fill fill, Border border, Label label) {
    this.configuration = configuration;
    this.geometry = geometry;
    this.fill = fill;
    this.border = border;
    this.label = label;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public Geometry getGeometry() {
    return geometry;
  }

  public Fill getFill() {
    return fill;
  }

  public Border getBorder() {
    return border;
  }

  public Label getLabel() {
    return label;
  }

  public VertexStyle withBorderColor(Color borderColor) {
    return new VertexStyle(configuration, geometry, fill,
      new Border(
        format(
          "#%02x%02x%02x",
          borderColor.getRed(),
          borderColor.getGreen(),
          borderColor.getBlue()
        ),
        border.line,
        7.0
      ),
      label
    );
  }

  @Override
  public String toString() {
    return "VertexStyle{" + joinWithComma(configuration, geometry, fill, border, label) + '}';
  }

  public static class Geometry {

    private final Property<Double> width, height, x, y;

    public Geometry(double width, double height, double x, double y) {
      this.width = new Property<Double>(width, "width");
      this.height = new Property<Double>(height, "height");
      this.x = new Property<Double>(x, "x");
      this.y = new Property<Double>(y, "y");
    }

    public Property<Double> getWidth() {
      return width;
    }

    public Property<Double> getHeight() {
      return height;
    }

    public Property<Double> getX() {
      return x;
    }

    public Property<Double> getY() {
      return y;
    }

    @Override
    public String toString() {
      return "Geometry{" + joinWithComma(width, height, x, y) + '}';
    }
  }

  public static class Fill {

    private final Property<String> color, color2;

    public Fill(String color, String color2) {
      this.color = new Property<String>(color, "color");
      this.color2 = new Property<String>(color2, "color2");
    }

    public Property<String> getColor() {
      return color;
    }

    public Property<String> getColor2() {
      return color2;
    }

    @Override
    public String toString() {
      return "Fill{" + joinWithComma(color, color2) + '}';
    }
  }

  public static class Border {

    private final Property<String> color;
    private final LineType line;
    private final Property<Double> width;

    public Border(String color, LineType line, double width) {
      this.color = new Property<String>(color, "color");
      this.line = line;
      this.width = new Property<Double>(width, "width");
    }

    public Property<String> getColor() {
      return color;
    }

    public LineType getLine() {
      return line;
    }

    public Property<Double> getWidth() {
      return width;
    }

    @Override
    public String toString() {
      return "Border{" + joinWithComma(color, line, width) + '}';
    }
  }

  public static class Configuration  extends AbstractNullableType implements CharSequence {
    public Configuration(String value) {
      super(value, "configuration");
    }
  }

  public static class LineType extends AbstractNullableType implements CharSequence {
    public LineType(String value) {
      super(value, "type");
    }
  }

  public static class Alignment extends AbstractNullableType implements CharSequence {
    public Alignment(String value) {
      super(value, "alignment");
    }
  }

  public static class FontFamily extends AbstractNullableType implements CharSequence {
    public FontFamily(String value) {
      super(value, "fontFamily");
    }
  }

  public static class FontStyle extends AbstractNullableType implements CharSequence {
    public FontStyle(String value) {
      super(value, "fontStyle");
    }
  }

  public static class TextColor extends AbstractNullableType implements CharSequence {
    public TextColor(String value) {
      super(value, "textColor");
    }
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

  public static class Label {

    private final Geometry geometry;
    private final Alignment alignment;
    private final FontFamily fontFamily;
    private final FontStyle fontStyle;
    private final Property<Short> fontSize;
    private final TextColor textColor;
    private final Property<String> lineColor;
    private final Property<String> backgroundColor;

    public Label(Geometry geometry, Alignment alignment, FontFamily fontFamily, FontStyle fontStyle, short fontSize, TextColor textColor, String lineColor, String backgroundColor) {
      this.geometry = geometry;
      this.alignment = alignment;
      this.fontFamily = fontFamily;
      this.fontStyle = fontStyle;
      this.fontSize = new Property<Short>(fontSize, "fontSize");
      this.textColor = textColor;
      this.lineColor = new Property<String>(lineColor, "lineColor");
      this.backgroundColor = new Property<String>(backgroundColor, "backgroundColor");
    }

    public Geometry getGeometry() {
      return geometry;
    }

    public Alignment getAlignment() {
      return alignment;
    }

    public FontFamily getFontFamily() {
      return fontFamily;
    }

    public FontStyle getFontStyle() {
      return fontStyle;
    }

    public Property<Short> getFontSize() {
      return fontSize;
    }

    public TextColor getTextColor() {
      return textColor;
    }

    public Property<String> getLineColor() {
      return lineColor;
    }

    public Property<String> getBackgroundColor() {
      return backgroundColor;
    }

    @Override
    public String toString() {
      return "Label{" + joinWithComma(geometry, alignment, fontFamily, fontStyle, fontSize, textColor, lineColor, backgroundColor) + '}';
    }
  }

  private static String joinWithComma(Object ... objects) {
    return Stream.of(objects).map(Object::toString).filter(s -> !s.isEmpty()).collect(joining(", "));
  }

}
