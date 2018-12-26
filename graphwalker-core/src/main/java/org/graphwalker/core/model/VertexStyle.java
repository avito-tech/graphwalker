package org.graphwalker.core.model;


import java.awt.Color;
import java.util.Objects;

import static java.lang.String.format;

class AbstractType implements CharSequence {
  final String value;

  public AbstractType(String value) {
    this.value = value;
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
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AbstractType that = (AbstractType) o;
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
      new TextColor("#000000")
    )
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
    return "VertexStyle{" +
      "configuration=" + configuration +
      ", geometry=" + geometry +
      ", fill=" + fill +
      ", border=" + border +
      ", label=" + label +
      '}';
  }

  public static class Geometry {

    private final double width, height, x, y;

    public Geometry(double width, double height, double x, double y) {
      this.width = width;
      this.height = height;
      this.x = x;
      this.y = y;
    }

    public double getWidth() {
      return width;
    }

    public double getHeight() {
      return height;
    }

    public double getX() {
      return x;
    }

    public double getY() {
      return y;
    }

    @Override
    public String toString() {
      return "Geometry{" +
        "width=" + width +
        ", height=" + height +
        ", x=" + x +
        ", y=" + y +
        '}';
    }
  }

  public static class Fill {

    private final String color, color2;

    public Fill(String color, String color2) {
      this.color = color;
      this.color2 = color2;
    }

    public String getColor() {
      return color;
    }

    public String getColor2() {
      return color2;
    }

    @Override
    public String toString() {
      return "Fill{" +
        "color='" + color + '\'' +
        ", color2='" + color2 + '\'' +
        '}';
    }
  }

  public static class Border {

    private final String color;
    private final LineType line;
    private final double width;

    public Border(String color, LineType line, double width) {
      this.color = color;
      this.line = line;
      this.width = width;
    }

    public String getColor() {
      return color;
    }

    public LineType getLine() {
      return line;
    }

    public double getWidth() {
      return width;
    }

    @Override
    public String toString() {
      return "Border{" +
        "color='" + color + '\'' +
        ", line=" + line +
        ", width=" + width +
        '}';
    }
  }

  public static class Configuration  extends AbstractType implements CharSequence {
    public Configuration(String value) {
      super(value);
    }
  }

  public static class LineType extends AbstractType implements CharSequence {
    public LineType(String value) {
      super(value);
    }
  }

  public static class Alignment extends AbstractType implements CharSequence {
    public Alignment(String value) {
      super(value);
    }
  }

  public static class FontFamily extends AbstractType implements CharSequence {
    public FontFamily(String value) {
      super(value);
    }
  }

  public static class FontStyle extends AbstractType implements CharSequence {
    public FontStyle(String value) {
      super(value);
    }
  }

  public static class TextColor extends AbstractType implements CharSequence {
    public TextColor(String value) {
      super(value);
    }
  }

  public static class Label {

    private final Geometry geometry;
    private final Alignment alignment;
    private final FontFamily fontFamily;
    private final FontStyle fontStyle;
    private final short fontSize;
    private final TextColor textColor;

    public Label(Geometry geometry, Alignment alignment, FontFamily fontFamily, FontStyle fontStyle, short fontSize, TextColor textColor) {
      this.geometry = geometry;
      this.alignment = alignment;
      this.fontFamily = fontFamily;
      this.fontStyle = fontStyle;
      this.fontSize = fontSize;
      this.textColor = textColor;
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

    public short getFontSize() {
      return fontSize;
    }

    public TextColor getTextColor() {
      return textColor;
    }
  }

}
