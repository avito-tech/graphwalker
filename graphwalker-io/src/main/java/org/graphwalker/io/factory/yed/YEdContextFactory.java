package org.graphwalker.io.factory.yed;

/*
 * #%L
 * GraphWalker Input/Output
 * %%
 * Original work Copyright (c) 2005 - 2014 GraphWalker
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

import com.google.common.collect.*;
import com.yworks.xml.graphml.EdgeType;
import com.yworks.xml.graphml.*;
import com.yworks.xml.graphml.impl.EdgeLabelTypeImpl;
import com.yworks.xml.graphml.impl.NodeLabelTypeImpl;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.graphdrawing.graphml.xmlns.NodeType;
import org.graphdrawing.graphml.xmlns.*;
import org.graphdrawing.graphml.xmlns.impl.DataTypeImpl;
import org.graphdrawing.graphml.xmlns.impl.KeyForTypeImpl;
import org.graphdrawing.graphml.xmlns.impl.KeyTypeImpl;
import org.graphwalker.core.machine.Context;
import org.graphwalker.core.model.*;
import org.graphwalker.core.model.Edge.RuntimeEdge;
import org.graphwalker.core.model.Vertex.RuntimeVertex;
import org.graphwalker.dsl.antlr.yed.YEdDescriptiveErrorListener;
import org.graphwalker.dsl.yed.YEdEdgeParser;
import org.graphwalker.dsl.yed.YEdLabelLexer;
import org.graphwalker.dsl.yed.YEdVertexParser;
import org.graphwalker.io.common.ResourceNotFoundException;
import org.graphwalker.io.common.ResourceUtils;
import org.graphwalker.io.factory.ContextFactory;
import org.graphwalker.io.factory.ContextFactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.awt.Color.*;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static org.apache.commons.collections4.map.ListOrderedMap.listOrderedMap;
import static org.apache.commons.lang3.StringEscapeUtils.escapeXml10;
import static org.graphwalker.core.model.LineStyle.Property;
import static org.graphwalker.core.model.LineStyle.*;
import static org.graphwalker.core.model.TypePrefix.BOOLEAN;
import static org.graphwalker.core.model.TypePrefix.*;
import static org.graphwalker.core.model.VertexStyle.Label;
import static org.graphwalker.core.model.VertexStyle.LineType;
import static org.graphwalker.core.model.VertexStyle.*;
import static org.graphwalker.dsl.yed.YEdEdgeParser.*;


/**
 * @author Nils Olsson
 */
public final class YEdContextFactory implements ContextFactory {

  private static final Logger logger = LoggerFactory.getLogger(YEdContextFactory.class);
  private static final String NAMESPACE = "declare namespace xq='http://graphml.graphdrawing.org/xmlns';";
  private static final String Y = "declare namespace y='http://www.yworks.com/xml/graphml';";
  private static final String FILE_TYPE = "graphml";
  private static final Set<String> SUPPORTED_TYPE = new HashSet<>(asList("**/*.graphml"));

  public static final Object GROUP_NAME = new Object();
  public static final Object OVER_GROUP = new Object();

  private boolean linkStyles = true;
  private String crossGroupSourceArrow = "white_diamond";
  private String sameGroupSourceArrow = "none";
  private String crossGroupTargetArrow = "white_delta";
  private String sameGroupTargetArrow = "standard";
  private String crossGroupLineStyle = "dashed_dotted";
  private String sameGroupLineStyle = "line";

  @Override
  public Set<String> getSupportedFileTypes() {
    return SUPPORTED_TYPE;
  }

  @Override
  public boolean accept(java.nio.file.Path path) {
    return FilenameUtils.getExtension(path.toString()).equalsIgnoreCase(FILE_TYPE);
  }

  @Override
  public List<Context> create(Path path) throws IOException {
    List<Context> contexts = new ArrayList<>();

    if (ResourceUtils.isDirectory(path)) {
      DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path);
      for (Path file : directoryStream) {
        contexts.add(read(file));
      }
    } else {
      contexts.add(read(path));
    }
    return contexts;
  }

  @Override
  public Context create(List<Path> paths) {
    List<GraphmlDocument> documents = new ArrayList<>();
    try {
      for (Path path : paths) {
        GraphmlDocument document = GraphmlDocument.Factory.parse(ResourceUtils.getResourceAsStream(path.toString()));
        document.documentProperties().put(GROUP_NAME, path.toFile().getName().replaceFirst("[.][^.]+$", ""));
        document.documentProperties().put(OVER_GROUP, path.getParent().getFileName().toString());
        documents.add(document);
      }
    } catch (XmlException e) {
      logger.error(e.getMessage());
      throw new ContextFactoryException("Create. The file appears not to be valid yEd formatted.");
    } catch (IOException | ResourceNotFoundException e) {
      logger.error(e.getMessage());
      throw new ContextFactoryException("Could not read the file.");
    }
    return read(FilenameUtils.getBaseName(paths.get(0).toString()), documents);
  }

  @Override
  public boolean setOption(String key, Object value) {
    switch (key) {
      case "linkStyles":
        linkStyles = (boolean) value;
        return true;
      case "crossGroupSourceArrow":
        crossGroupSourceArrow = (String) value;
        return true;
      case "sameGroupSourceArrow":
        sameGroupSourceArrow = (String) value;
        return true;
      case "crossGroupTargetArrow":
        crossGroupTargetArrow = (String) value;
        return true;
      case "sameGroupTargetArrow":
        sameGroupTargetArrow = (String) value;
        return true;
      case "crossGroupLineStyle":
        crossGroupLineStyle = (String) value;
        return true;
      case "sameGroupLineStyle":
        sameGroupLineStyle = (String) value;
        return true;
      default:
        logger.info("Option \"" + key + "\" was rejected by YEdContextFactory");
        return false;
    }
  }

  public List<Context> create(String graphmlStr) {
    List<Context> contexts = new ArrayList<>();
    contexts.add(read(graphmlStr));
    return contexts;
  }

  private Context read(Path path) {
    GraphmlDocument document = null;
    try {
      document = GraphmlDocument.Factory.parse(ResourceUtils.getResourceAsStream(path.toString()));
    } catch (XmlException e) {
      logger.error(e.getMessage());
      throw new ContextFactoryException("Read path. The file " + path + " appears not to be valid yEd formatted.");
    } catch (IOException | ResourceNotFoundException e) {
      logger.error(e.getMessage());
      throw new ContextFactoryException("Could not read the file.");
    }
    return read(document, FilenameUtils.getBaseName(path.toString()));
  }

  private Context read(String graphmlStr) {
    GraphmlDocument document = null;
    try {
      document = GraphmlDocument.Factory.parse(graphmlStr);
    } catch (XmlException e) {
      logger.error(e.getMessage());
      throw new ContextFactoryException("Read xml. The file appears not to be valid yEd formatted.");
    }
    return read(document, "");
  }

  private Context read(GraphmlDocument document, String name) {
    Context context = new YEdContext();
    Edge startEdge;
    Map<String, Vertex> elements = new HashMap<>();
    Model model = new Model();
    try {
      Vertex startVertex = addVertices(model, document, elements).start;
      startEdge = addEdges(model, document, elements, startVertex);
    } catch (XmlException e) {
      logger.error(e.getMessage());
      throw new ContextFactoryException("Read by name. The file seems not to be of valid yEd format.");
    }

    model.setName(name);
    context.setModel(model.build());
    if (null != startEdge) {
      context.setNextElement(startEdge);
    }

    return context;
  }

  private Context read(String name, List<GraphmlDocument> documents) {
    Context context = new YEdContext();
    Edge startEdge = null;
    Map<String, Vertex> elements = new HashMap<>();
    Model model = new Model();
    Multimap<String, IndegreeVertex> indegrees = ArrayListMultimap.create();
    Multimap<String, Vertex> outdegrees = ArrayListMultimap.create();
    try {
      for (GraphmlDocument document : documents) {
        AddResult<Vertex> addResult = addVertices(model, document, elements, indegrees, outdegrees);
        Vertex startVertex = addResult.start;
        Edge edge = addEdges(model, document, elements, startVertex);
        if (edge != null) {
          startEdge = edge;
        }
      }
      for (Edge edge : model.getEdges()) {
        Vertex target;
        if (null != (target = edge.getTargetVertex())) {
          List<Action> setActions = target.getSetActions();
          if (!setActions.isEmpty()) {
            for (Action set : setActions) {
              if (!edge.getActions().contains(set)) {
                edge.addAction(set);
              }
            }
          }
        }
      }
      for (Map.Entry<String, Collection<IndegreeVertex>> indegreeEntry : indegrees.asMap().entrySet()) {
        String edgeName = indegreeEntry.getKey();
        for (Vertex out : outdegrees.get(edgeName)) {
          for (IndegreeVertex in : indegreeEntry.getValue()) {
            Edge edge = new Edge()
              .setSourceVertex(out)
              .setTargetVertex(in.getVertex())
              .setName(edgeName)
              .setDescription(in.getDescription())
              .setGuard(in.getGuard())
              .setWeight(in.getWeight())
              .setCodeTag(in.getCodeTag());
            for (Action set : in.getVertex().getSetActions()) {
              edge.addAction(set);
            }
            model.addEdge(edge);
          }
        }
      }

    } catch (XmlException e) {
      logger.error(e.getMessage());
      throw new ContextFactoryException("Read documents. The file seems not to be of valid yEd format.");
    }

    model.setName(name);
    context.setModel(model.build());
    if (null != startEdge) {
      context.setNextElement(startEdge);
    }

    return context;
  }

  private static void appendVertex(StringBuilder str, String id, String name, String description, VertexStyle style,
                                   List<Action> actions, List<String> outdegrees,
                                   List<IndegreeLabel> indegrees) {
    Geometry geometry = style.getGeometry();
    Fill fill = style.getFill();
    Label label = style.getLabel();
    Border border = style.getBorder();
    Geometry labelGeometry = label.getGeometry();
    Configuration configuration = style.getConfiguration();

    String newLine = System.lineSeparator();
    str.append("    <node id=\"").append(id).append("\">").append(newLine);
    str.append("      <data key=\"d0\">").append(newLine);
    str.append("        <y:GenericNode ").append(configuration.toString()).append(">").append(newLine);
    str.append("          <y:Geometry").append(joinWithSpace(geometry.getX(), geometry.getY(), geometry.getWidth(), geometry.getHeight())).append("/>").append(newLine);
    str.append("          <y:Fill transparent=\"false\"").append(joinWithSpace(fill.getColor(), fill.getColor2())).append("/>").append(newLine);
    str.append("          <y:BorderStyle").append(joinWithSpace(border.getLine(), border.getWidth(), border.getColor())).append("/>").append(newLine);
    str.append("          <y:NodeLabel visible=\"true\" modelName=\"internal\" modelPosition=\"c\" autoSizePolicy=\"content\"").append(joinWithSpace(
      labelGeometry.getX(), labelGeometry.getY(), labelGeometry.getWidth(), labelGeometry.getHeight(),
      label.getAlignment(), label.getFontFamily(), label.getFontSize(), label.getFontStyle(),
      label.getTextColor(), label.getLineColor(), label.getBackgroundColor())).append(">").append(name);

    if (description != null && !description.trim().isEmpty()) {
      String formattedDescription = description
        .replaceAll("([^\\\\])\\\\n", "$1\n")
        .replace("\\\\n", "\\n");
      str.append(newLine)
        .append("/* ")
        .append(escapeXml10(formattedDescription)).append(" */");
    }

    if (!actions.isEmpty()) {
      str.append(newLine).append("INIT: ");
      for (int i = 0; i < actions.size(); i++) {
        str.append(actions.get(i).getScript()).append(i < actions.size() - 1 ? ", " : ";");
      }
    }

    if (!outdegrees.isEmpty()) {
      str.append(newLine).append("OUTDEGREE: ");
      for (int i = 0; i < outdegrees.size(); i++) {
        str.append(outdegrees.get(i)).append(i < outdegrees.size() - 1 ? ", " : ";");
      }
    }

    if (!indegrees.isEmpty()) {
      str.append(newLine).append("INDEGREE: ");
      for (int i = 0; i < indegrees.size(); i++) {
        str.append(escapeXml10(indegrees.get(i).toString())).append(i < indegrees.size() - 1 ? ", " : ";");
      }
    }

    str.append("</y:NodeLabel>").append(newLine);
    str.append("          <y:Shape type=\"rectangle\"/>").append(newLine);
    str.append("        </y:GenericNode>").append(newLine);
    str.append("      </data>").append(newLine);
    str.append("    </node>").append(newLine);
  }

  private void appendEdge(StringBuilder str, String id, String srcId, String destId,
                                 String name, Argument.List arguments, Guard guard,
                                 List<Action> actions, int dependency, String description,
                                 LineStyle style, Double weight) {
    LineStyle.LineType type = style.getType();
    LineColor color = style.getColor();
    Property<Double> width = style.getWidth();

    String newLine = System.lineSeparator();

    boolean crossGroup = !srcId.split("::")[0].equals(destId.split("::")[0]);
    // "PolyLineEdge" seems to be much more readable than "BezierEdge" in large graphs
    final String edgeType = "PolyLineEdge";

    final String sourceArrow = crossGroup ?  crossGroupSourceArrow : sameGroupSourceArrow;
    final String targetArrow = crossGroup ?  crossGroupTargetArrow : sameGroupTargetArrow;

    str.append("    <edge id=\"").append(id).append("\" source=\"").append(srcId).append("\" target=\"").append(destId).append("\">").append(newLine);
    str.append("      <data key=\"d1\" >").append(newLine);
    str.append("        <y:").append(edgeType).append(" >").append(newLine);
    str.append("          <y:Path sx=\"-23.75\" sy=\"15.0\" tx=\"-23.75\" ty=\"-15.0\">").append(newLine);
    str.append("            <y:Point x=\"273.0\" y=\"95.0\"/>").append(newLine);
    str.append("            <y:Point x=\"209.0\" y=\"95.0\"/>").append(newLine);
    str.append("            <y:Point x=\"209.0\" y=\"143.7\"/>").append(newLine);
    str.append("            <y:Point x=\"265.0\" y=\"143.7\"/>").append(newLine);
    str.append("          </y:Path>").append(newLine);
    str.append("          <y:LineStyle ").append(type.toString()).append(width.toString()).append(color.toString()).append("/>").append(newLine);
    str.append("          <y:Arrows source=\"").append(sourceArrow).append("\" target=\"").append(targetArrow).append("\"/>").append(newLine);

    if (name == null) {
      throw new IllegalStateException("Edge @id=\"" + id + "\" has no name");
    }

    if (!name.isEmpty()) {
      StringBuilder label = new StringBuilder(name);

      if (arguments != null && !arguments.isEmpty()) {
        label.append(" ").append(arguments);
      }
      if (actions != null && !actions.isEmpty()) {
        label.append(" /");
        for (Action action : actions) {
          label.append(action.getScript());
        }
      }
      if (guard != null) {
        label.append(newLine).append("[").append(guard.getScript()).append("]");
      }
      if (description != null && !description.trim().isEmpty()) {
        String formattedDescription = description
          .replaceAll("([^\\\\])\\\\n", "$1\n")
          .replace("\\\\n", "\\n");
        label.append(newLine).append("/* ").append(formattedDescription).append(" */");
      }

      if (dependency != 0) {
        label.append("\ndependency=").append(dependency);
      }

      if (weight != null && weight < 1.0) {
        label.append("\nweight=").append(weight);
      }

      str.append("          <y:EdgeLabel x=\"-148.25\" y=\"30.0\" width=\"169.0\" height=\"18.0\" " + "visible=\"true\" alignment=\"center\" fontFamily=\"Dialog\" fontSize=\"12\" " + "fontStyle=\"plain\" textColor=\"");
      str.append(color.getValue()).append("\" modelName=\"free\" modelPosition=\"anywhere\" ").append("preferredPlacement=\"on_edge\" distance=\"2.0\" ratio=\"0.5\">").append(escapeXml10(label.toString()));
      str.append("</y:EdgeLabel>").append(newLine);
    }

    str.append("          <y:BendStyle smoothed=\"true\"/>").append(newLine);
    str.append("        </y:" + edgeType + ">").append(newLine);
    str.append("      </data>").append(newLine);
    str.append("    </edge>").append(newLine);
  }

  private static String getGroupPrefix(LinkedHashMap<String, LinkedHashMultimap<String, RuntimeVertex>> groupedVertices, RuntimeVertex vertex) {
    int overGroupIndex = 0, groupIndex;
    String overGroupLabel = "", groupLabel = "";
    for (String overGroupName : groupedVertices.keySet()) {
      if (Objects.equals(overGroupName, vertex.getOverGroup())) {
        LinkedHashMultimap<String, RuntimeVertex> groups = groupedVertices.get(overGroupName);
        if (null != vertex.getGroupName()) {
          groupIndex = 0;
          for (String groupName : groups.keySet()) {
            if (Objects.equals(groupName, vertex.getGroupName())) {
              groupLabel = "n" + groupIndex + "::";
              break;
            }
            groupIndex++;
          }
        }
        if (null != overGroupName) {
          overGroupLabel = "n" + overGroupIndex + "::";
        }
        break;
      }
      overGroupIndex++;
    }
    return overGroupLabel + groupLabel;
  }

  private static String getColorByHashCode(Object object, byte opacity) {
    int rgb = object.hashCode();
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = (rgb >> 0) & 0xFF;
    return String.format("#%02x%02x%02x%02x", r, g, b, opacity);
  }

  @Override
  public String getAsString(List<Context> contexts) {
    StringBuilder graphmlStr = new StringBuilder();
    for (Context context : contexts) {
      String newLine = System.lineSeparator();
      StringBuilder str = new StringBuilder();

      str.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>").append(newLine);
      str.append("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" "
        + "xmlns:java=\"http://www.yworks.com/xml/yfiles-common/1.0/java\" "
        + "xmlns:sys=\"http://www.yworks.com/xml/yfiles-common/markup/primitives/2.0\" "
        + "xmlns:x=\"http://www.yworks.com/xml/yfiles-common/markup/2.0\" "
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        + "xmlns:y=\"http://www.yworks.com/xml/graphml\" "
        + "xmlns:yed=\"http://www.yworks.com/xml/yed/3\" "
        + "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd\">").append(newLine);
      str.append("  <key id=\"d0\" for=\"node\" yfiles.type=\"nodegraphics\"/>").append(newLine);
      str.append("  <key id=\"d1\" for=\"edge\" yfiles.type=\"edgegraphics\"/>").append(newLine);
      str.append("  <graph id=\"G\" edgedefault=\"directed\">").append(newLine);

      BiMap<RuntimeVertex, String> uniqueVertices = HashBiMap.create();
      BiMap<RuntimeEdge, String> uniqueEdges = HashBiMap.create();

      LinkedHashMap<String, LinkedHashMultimap<String, RuntimeVertex>> groupedVertices = new LinkedHashMap<>();

      // nodes' ids in yEd have format like "n9::n3::n2" / "n2::n1" / "n1", where only last part should be changed
      // http://www.catonmat.net/blog/my-favorite-regex/
      Pattern p = Pattern.compile("^([ -~]*)(\\d+)$");
      for (RuntimeVertex v : context.getModel().getVertices()) {
        String id = v.getId();
        Matcher m = p.matcher(id);
        if (m.find()) {
          String letter = m.group(1);
          int digit = parseInt(m.group(2));
          while (uniqueVertices.containsValue(letter + digit)) {
            digit += 1_000;
          }
          uniqueVertices.put(v, letter + digit);
        } else {
          uniqueVertices.forcePut(v, v.getId());
        }
        final String groupName = v.getGroupName();
        final String overGroupName = v.getOverGroup();
        LinkedHashMultimap<String, RuntimeVertex> overGroups = groupedVertices.computeIfAbsent(overGroupName, s -> {
          LinkedHashMultimap<String, RuntimeVertex> overGroup = LinkedHashMultimap.create();
          overGroup.put(groupName, v);
          return overGroup;
        });
        overGroups.put(groupName, v);
      }
      for (RuntimeEdge e : context.getModel().getEdges()) {
        String id = e.getId();
        Matcher m = p.matcher(id);
        if (m.find()) {
          String letter = m.group(1);
          int digit = parseInt(m.group(2));
          while (uniqueEdges.containsValue(letter + digit)) {
            digit += 1_000;
          }
          uniqueEdges.put(e, letter + digit);
        } else {
          uniqueEdges.forcePut(e, e.getId());
        }
      }

      RuntimeVertex targetVertex;
      if (context.getNextElement() != null
        && context.getNextElement() instanceof RuntimeEdge
        && (targetVertex = ((RuntimeEdge) context.getNextElement()).getTargetVertex()) != null) {
        int n = 0, e = 0;
        while (uniqueVertices.containsValue("n" + n)) {
          n++;
        }
        while (uniqueEdges.containsValue("e" + e)) {
          e++;
        }
        appendVertex(str, "n" + n, "Start", null, DEFAULT_VERTEX_STYLE, emptyList(), emptyList(), emptyList());
        appendEdge(str, "e" + e, "n" + n, getGroupPrefix(groupedVertices, targetVertex) + uniqueVertices.get(targetVertex),
          context.getNextElement().getName(),
          ((RuntimeEdge) context.getNextElement()).getArguments(),
          ((RuntimeEdge) context.getNextElement()).getGuard(),
          context.getNextElement().getActions(),
          ((RuntimeEdge) context.getNextElement()).getDependency(),
          context.getNextElement().getDescription(),
          DEFAULT_EDGE_STYLE,
          ((RuntimeEdge) context.getNextElement()).getWeight());
      }

      Set<RuntimeVertex> hasNoInput = new HashSet<>(context.getModel().getVertices());
      Set<RuntimeVertex> hasNoOutput = new HashSet<>(context.getModel().getVertices());

      for (RuntimeEdge e : context.getModel().getEdges()) {
        RuntimeVertex src = e.getSourceVertex();
        RuntimeVertex dest = e.getTargetVertex();

        if (src == null || dest == null) {
          if (dest != null) {
            hasNoInput.remove(dest);
          } else if (src != null) {
            hasNoOutput.remove(src);
          }
          continue;
        }

        hasNoInput.remove(dest);
        hasNoOutput.remove(src);

        String edgeName;
        if (e.getName() == null) {
          logger.warn("Edge between {} and {} (marked with \"red\" color) has no Text property. It invalidates the model!",
            e.getSourceVertex().getId(), e.getTargetVertex());
          edgeName = "(No text specified!)";
        } else {
          edgeName = e.getName();
        }

        String id = uniqueEdges.get(e);
        String srcId = getGroupPrefix(groupedVertices, src) + uniqueVertices.get(src);
        String destId = getGroupPrefix(groupedVertices, dest) + uniqueVertices.get(dest);

        appendEdge(str, id, srcId, destId,
          edgeName,
          e.getArguments(),
          e.hasGuard() ? e.getGuard() : null,
          e.hasActions() ? e.getActions() : emptyList(),
          e.getDependency(),
          e.getDescription(),
          e.getStyle(),
          e.getWeight());
      }

      int overGroupIndex = 0, groupIndex;
      String overGroupLabel, groupLabel;
      for (Map.Entry<String, LinkedHashMultimap<String, RuntimeVertex>> overGroup : groupedVertices.entrySet()) {
        if (null != overGroup.getKey()) {
          overGroupLabel = "n" + overGroupIndex++;
          str.append("<node id=\"").append(overGroupLabel).append("\" yfiles.foldertype=\"group\">").append(newLine);
          str.append("<data key=\"d0\">").append(newLine);
          str.append("  <y:ProxyAutoBoundsNode>").append(newLine);
          str.append("    <y:Realizers active=\"0\">").append(newLine);
          str.append("      <y:GroupNode>").append(newLine);
          str.append("        <y:Geometry height=\"500.0\" width=\"500.0\" x=\"4000.0\" y=\"3000.0\"/>").append(newLine);
          str.append("        <y:Fill color=\"").append(getColorByHashCode(overGroup.getKey(), (byte) 64)).append("\" color2=\"#FFFFFF80\" transparent=\"false\"/>").append(newLine);
          str.append("        <y:BorderStyle color=\"#000000\" raised=\"false\" type=\"line\" width=\"1.0\"/>").append(newLine);
          str.append("        <y:NodeLabel alignment=\"center\" autoSizePolicy=\"content\" hasBackgroundColor=\"false\" hasLineColor=\"false\" borderDistance=\"0.0\" fontFamily=\"Dialog\" fontSize=\"64\" fontStyle=\"plain\" height=\"25.0\" horizontalTextPosition=\"center\" iconTextGap=\"4\" modelName=\"internal\" modelPosition=\"tr\" textColor=\"#000000\" verticalTextPosition=\"bottom\" visible=\"true\" width=\"455.0\" x=\"0.0\" y=\"0.0\">");
          str.append(overGroup.getKey()).append("</y:NodeLabel>").append(newLine);
          str.append("        <y:Shape type=\"roundrectangle\"/>").append(newLine);
          str.append("        <y:State closed=\"false\" closedHeight=\"50.0\" closedWidth=\"50.0\" innerGraphDisplayEnabled=\"false\"/>").append(newLine);
          str.append("        <y:Insets bottom=\"15\" bottomF=\"15.0\" left=\"15\" leftF=\"15.0\" right=\"15\" rightF=\"15.0\" top=\"15\" topF=\"15.0\"/>").append(newLine);
          str.append("        <y:BorderInsets bottom=\"0\" bottomF=\"0.0\" left=\"0\" leftF=\"0.0\" right=\"0\" rightF=\"0.0\" top=\"0\" topF=\"0.0\"/>").append(newLine);
          str.append("      </y:GroupNode>").append(newLine);
          str.append("      <y:GroupNode>").append(newLine);
          str.append("        <y:Geometry height=\"50.0\" width=\"50.0\" x=\"0.0\" y=\"60.0\"/>").append(newLine);
          str.append("        <y:Fill color=\"#F5F5F5\" transparent=\"false\"/>").append(newLine);
          str.append("        <y:BorderStyle color=\"#000000\" type=\"dashed\" width=\"1.0\"/>").append(newLine);
          str.append("        <y:NodeLabel alignment=\"right\" autoSizePolicy=\"node_width\" backgroundColor=\"#EBEBEB\" borderDistance=\"0.0\" fontFamily=\"Dialog\" fontSize=\"15\" fontStyle=\"plain\" hasLineColor=\"false\" height=\"24.0\" horizontalTextPosition=\"center\" iconTextGap=\"4\" modelName=\"internal\" modelPosition=\"tr\" textColor=\"#000000\" verticalTextPosition=\"bottom\" visible=\"true\" width=\"60.0\" x=\"-4.0\" y=\"0.0\">");
          str.append(overGroup.getKey()).append("</y:NodeLabel>").append(newLine);
          str.append("        <y:Shape type=\"roundrectangle\"/>").append(newLine);
          str.append("        <y:State closed=\"true\" closedHeight=\"50.0\" closedWidth=\"50.0\" innerGraphDisplayEnabled=\"false\"/>").append(newLine);
          str.append("        <y:Insets bottom=\"5\" bottomF=\"5.0\" left=\"5\" leftF=\"5.0\" right=\"5\" rightF=\"5.0\" top=\"5\" topF=\"5.0\"/>").append(newLine);
          str.append("        <y:BorderInsets bottom=\"0\" bottomF=\"0.0\" left=\"0\" leftF=\"0.0\" right=\"0\" rightF=\"0.0\" top=\"0\" topF=\"0.0\"/>").append(newLine);
          str.append("      </y:GroupNode>").append(newLine);
          str.append("    </y:Realizers>").append(newLine);
          str.append("  </y:ProxyAutoBoundsNode>").append(newLine);
          str.append("</data>").append(newLine);
          str.append("<graph edgedefault=\"directed\" id=\"").append(overGroupLabel).append(":\">").append(newLine);
          overGroupLabel += "::";
        } else {
          overGroupLabel = "";
        }
        groupIndex = 0;
        for (Map.Entry<String, Collection<RuntimeVertex>> group : overGroup.getValue().asMap().entrySet()) {
          if (null != group.getKey()) {
            groupLabel = "n" + groupIndex++;
            str.append("<node id=\"").append(overGroupLabel).append(groupLabel).append("\" yfiles.foldertype=\"group\">").append(newLine);
            str.append("<data key=\"d0\">").append(newLine);
            str.append("  <y:ProxyAutoBoundsNode>").append(newLine);
            str.append("    <y:Realizers active=\"0\">").append(newLine);
            str.append("      <y:GroupNode>").append(newLine);
            str.append("        <y:Geometry height=\"500.0\" width=\"500.0\" x=\"4000.0\" y=\"3000.0\"/>").append(newLine);
            str.append("        <y:Fill color=\"#F2F0D880\" transparent=\"false\"/>").append(newLine);
            str.append("        <y:BorderStyle color=\"#000000\" type=\"line\" width=\"1.0\"/>").append(newLine);
            str.append("        <y:NodeLabel alignment=\"right\" autoSizePolicy=\"node_width\" backgroundColor=\"#404040\" borderDistance=\"0.0\" fontFamily=\"Dialog\" fontSize=\"16\" fontStyle=\"plain\" hasLineColor=\"false\" height=\"25.0\" horizontalTextPosition=\"center\" iconTextGap=\"4\" modelName=\"internal\" modelPosition=\"t\" textColor=\"#FFFFFF\" verticalTextPosition=\"bottom\" visible=\"true\" width=\"455.0\" x=\"0.0\" y=\"0.0\">").append(group.getKey()).append("</y:NodeLabel>").append(newLine);
            str.append("        <y:Shape type=\"rectangle\"/>").append(newLine);
            str.append("        <y:DropShadow color=\"#D2D2D2\" offsetX=\"4\" offsetY=\"4\"/>").append(newLine);
            str.append("        <y:State closed=\"false\" closedHeight=\"50.0\" closedWidth=\"50.0\" innerGraphDisplayEnabled=\"false\"/>").append(newLine);
            str.append("        <y:Insets bottom=\"15\" bottomF=\"15.0\" left=\"15\" leftF=\"15.0\" right=\"15\" rightF=\"15.0\" top=\"15\" topF=\"15.0\"/>").append(newLine);
            str.append("        <y:BorderInsets bottom=\"0\" bottomF=\"0.0\" left=\"0\" leftF=\"0.0\" right=\"0\" rightF=\"0.0\" top=\"0\" topF=\"0.0\"/>").append(newLine);
            str.append("      </y:GroupNode>").append(newLine);
            str.append("      <y:GroupNode>").append(newLine);
            str.append("        <y:Geometry height=\"50.0\" width=\"50.0\" x=\"0.0\" y=\"60.0\"/>").append(newLine);
            str.append("        <y:Fill color=\"#F5F5F5\" transparent=\"false\"/>").append(newLine);
            str.append("        <y:BorderStyle color=\"#000000\" type=\"dashed\" width=\"1.0\"/>").append(newLine);
            str.append("        <y:NodeLabel alignment=\"right\" autoSizePolicy=\"node_width\" backgroundColor=\"#EBEBEB\" borderDistance=\"0.0\" fontFamily=\"Dialog\" fontSize=\"15\" fontStyle=\"plain\" hasLineColor=\"false\" height=\"24.0\" horizontalTextPosition=\"center\" iconTextGap=\"4\" modelName=\"internal\" modelPosition=\"t\" textColor=\"#000000\" verticalTextPosition=\"bottom\" visible=\"true\" width=\"60.0\" x=\"-4.0\" y=\"0.0\">").append(group.getKey()).append("</y:NodeLabel>").append(newLine);
            str.append("        <y:Shape type=\"roundrectangle\"/>").append(newLine);
            str.append("        <y:State closed=\"true\" closedHeight=\"50.0\" closedWidth=\"50.0\" innerGraphDisplayEnabled=\"false\"/>").append(newLine);
            str.append("        <y:Insets bottom=\"5\" bottomF=\"5.0\" left=\"5\" leftF=\"5.0\" right=\"5\" rightF=\"5.0\" top=\"5\" topF=\"5.0\"/>").append(newLine);
            str.append("        <y:BorderInsets bottom=\"0\" bottomF=\"0.0\" left=\"0\" leftF=\"0.0\" right=\"0\" rightF=\"0.0\" top=\"0\" topF=\"0.0\"/>").append(newLine);
            str.append("      </y:GroupNode>").append(newLine);
            str.append("    </y:Realizers>").append(newLine);
            str.append("  </y:ProxyAutoBoundsNode>").append(newLine);
            str.append("</data>").append(newLine);
            str.append("<graph edgedefault=\"directed\" id=\"").append(overGroupLabel).append(groupLabel).append(":\">").append(newLine);
            groupLabel += "::";
          } else {
            groupLabel = "";
          }

          for (RuntimeVertex v : group.getValue()) {
            String id = overGroupLabel + groupLabel + uniqueVertices.get(v);
            VertexStyle vertexStyle = v.getStyle();
            if (null == vertexStyle) {
              vertexStyle = !v.getActions().isEmpty() || (v.getDescription() != null && v.getDescription().length() >= 50)
                ? SCALED_VERTEX_STYLE
                : DEFAULT_VERTEX_STYLE;
            }
            if (hasNoInput.contains(v)) {
              logger.warn("Vertex " + v + " has no input edges (marked with \"red\" color). " +
                "It could not be tested!");
              vertexStyle = vertexStyle.withBorderColor(RED);
            } else if (hasNoOutput.contains(v)) {
              logger.debug("Vertex " + v + " has no output edges (marked with \"magenta\" color). " +
                "Some of path generating techniques will not work correctly with that graph!");
              vertexStyle = vertexStyle.withBorderColor(MAGENTA);
            }
            appendVertex(str, id, v.getName(), v.getDescription(), vertexStyle, v.getActions(), emptyList(), emptyList());
          }
          // close tag conditionally
          if (!groupLabel.isEmpty()) {
            str.append("</graph>").append(newLine);
            str.append("</node>").append(newLine);
          }
        }
        // close tag conditionally
        if (!overGroupLabel.isEmpty()) {
          str.append("</graph>").append(newLine);
          str.append("</node>").append(newLine);
        }
      }

      str.append("  </graph>").append(newLine);
      str.append("</graphml>").append(newLine);

      graphmlStr.append(str);
    }
    return graphmlStr.toString();
  }

  private static class IndexedCollection<E> {
    final Collection<E> items;
    final int index;

    public IndexedCollection(int index, E item) {
      this.index = index;
      this.items = item != null ? new ArrayList<>(asList(item)) : new ArrayList<>();
    }

    @Override
    public String toString() {
      return index >= 0 ? "n" + index + "::" : "";
    }
  }
  static IndexedCollection<RuntimeVertex> NO_GROUP = new IndexedCollection<>(-1, null);

  private String getAsString(List<Context> contexts, String selectOnlyGroup,
                             Map<RuntimeVertex, Map<RuntimeVertex, String>> outdegrees,
                             Map<RuntimeVertex, List<IndegreeLabel>> indegrees) {
    StringBuilder graphmlStr = new StringBuilder();
    for (Context context : contexts) {
      String newLine = System.lineSeparator();
      StringBuilder str = new StringBuilder();

      str.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>").append(newLine);
      str.append("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" "
        + "xmlns:java=\"http://www.yworks.com/xml/yfiles-common/1.0/java\" "
        + "xmlns:sys=\"http://www.yworks.com/xml/yfiles-common/markup/primitives/2.0\" "
        + "xmlns:x=\"http://www.yworks.com/xml/yfiles-common/markup/2.0\" "
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        + "xmlns:y=\"http://www.yworks.com/xml/graphml\" "
        + "xmlns:yed=\"http://www.yworks.com/xml/yed/3\" "
        + "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd\">").append(newLine);
      str.append("  <key id=\"d0\" for=\"node\" yfiles.type=\"nodegraphics\"/>").append(newLine);
      str.append("  <key id=\"d1\" for=\"edge\" yfiles.type=\"edgegraphics\"/>").append(newLine);
      str.append("  <graph id=\"G\" edgedefault=\"directed\">").append(newLine);

      BiMap<RuntimeVertex, String> uniqueVertices = HashBiMap.create();
      BiMap<RuntimeEdge, String> uniqueEdges = HashBiMap.create();

      Map<String, IndexedCollection<RuntimeVertex>> groupedVertices = new HashMap<>();

      // nodes' ids in yEd have format like "n9::n3::n2" / "n2::n1" / "n1", where only last part should be changed
      // http://www.catonmat.net/blog/my-favorite-regex/
      Pattern p = Pattern.compile("^([ -~]*)(\\d+)$");
      int g = 0;
      for (RuntimeVertex v : context.getModel().getVertices()) {
        String id = v.getId();
        Matcher m = p.matcher(id);
        if (m.find()) {
          String letter = m.group(1);
          int digit = parseInt(m.group(2));
          while (uniqueVertices.containsValue(letter + digit)) {
            digit += 1_000;
          }
          uniqueVertices.put(v, letter + digit);
        } else {
          uniqueVertices.forcePut(v, v.getId());
        }
        String groupName = v.getGroupName();
        IndexedCollection<RuntimeVertex> group = groupedVertices.get(groupName);
        if (group != null) {
          group.items.add(v);
        } else {
          groupedVertices.put(groupName, new IndexedCollection<>(g++, v));
        }
      }
      for (RuntimeEdge e : context.getModel().getEdges()) {
        String id = e.getId();
        Matcher m = p.matcher(id);
        if (m.find()) {
          String letter = m.group(1);
          int digit = parseInt(m.group(2));
          while (uniqueEdges.containsValue(letter + digit)) {
            digit += 1_000;
          }
          uniqueEdges.put(e, letter + digit);
        } else {
          uniqueEdges.forcePut(e, e.getId());
        }
      }

      if (context.getNextElement() != null
        && context.getNextElement() instanceof RuntimeEdge
        && ((RuntimeEdge) context.getNextElement()).getTargetVertex() != null) {
        int n = 0, e = 0;
        while (uniqueVertices.containsValue("n" + n)) {
          n++;
        }
        while (uniqueEdges.containsValue("e" + e)) {
          e++;
        }
        appendVertex(str, "n" + n, "Start", null, DEFAULT_VERTEX_STYLE, emptyList(), emptyList(), emptyList());
        IndexedCollection<RuntimeVertex> destGroup = groupedVertices.getOrDefault(((RuntimeEdge) context.getNextElement()).getTargetVertex().getGroupName(), NO_GROUP);
        appendEdge(str, "e" + e, "n" + n, destGroup + uniqueVertices.get(((RuntimeEdge) context.getNextElement()).getTargetVertex()),
          context.getNextElement().getName(),
          ((RuntimeEdge) context.getNextElement()).getArguments(),
          ((RuntimeEdge) context.getNextElement()).getGuard(),
          context.getNextElement().getActions(),
          ((RuntimeEdge) context.getNextElement()).getDependency(),
          context.getNextElement().getDescription(),
          ((RuntimeEdge) context.getNextElement()).getStyle(),
          ((RuntimeEdge) context.getNextElement()).getWeight());
      }

      Set<RuntimeVertex> hasNoInput = new HashSet<>(context.getModel().getVertices());
      Set<RuntimeVertex> hasNoOutput = new HashSet<>(context.getModel().getVertices());

      for (RuntimeEdge e : context.getModel().getEdges()) {
        RuntimeVertex src = e.getSourceVertex();
        RuntimeVertex dest = e.getTargetVertex();

        if (src == null || dest == null) {
          continue;
        }

        if (!Objects.equals(src.getGroupName(), selectOnlyGroup)
          || !Objects.equals(dest.getGroupName(), selectOnlyGroup)) {

          continue;
        }

        hasNoInput.remove(dest);
        hasNoOutput.remove(src);

        String edgeName;
        Color color;
        if (e.getName() == null) {
          logger.warn("Edge between {} and {} (marked with \"red\" color) has no Text property. It invalidates the model!",
            e.getSourceVertex().getId(), e.getTargetVertex());
          edgeName = "(No text specified!)";
          color = RED;
        } else {
          edgeName = e.getName();
          color = BLACK;
        }

        String id = uniqueEdges.get(e);
        String srcId = uniqueVertices.get(src);
        String destId = uniqueVertices.get(dest);

        appendEdge(str, id, srcId, destId,
          edgeName,
          e.getArguments(),
          e.hasGuard() ? e.getGuard() : null,
          e.hasActions() ? e.getActions() : emptyList(),
          e.getDependency(),
          e.getDescription(),
          e.getStyle(),
          e.getWeight());
      }

      for (RuntimeVertex v : groupedVertices.get(selectOnlyGroup).items) {
        String id = uniqueVertices.get(v);
        VertexStyle vertexStyle = v.getStyle();
        if (null == vertexStyle) {
          vertexStyle = !v.getActions().isEmpty() || (v.getDescription() != null && v.getDescription().length() >= 50)
            ? SCALED_VERTEX_STYLE
            : DEFAULT_VERTEX_STYLE;
        }
        if (hasNoInput.contains(v) && !indegrees.containsKey(v)) {
          if (v.hasName() && v.getName().equalsIgnoreCase("start")) {
            vertexStyle = vertexStyle.withBorderColor(GREEN);
          } else {
            logger.warn("Vertex " + v + " has no input edges (marked with \"red\" color). " +
              "It could not be tested!");
            vertexStyle = vertexStyle.withBorderColor(RED);
          }
        } else if (hasNoOutput.contains(v) && !outdegrees.containsKey(v)) {
          logger.warn("Vertex " + v + " has no output edges (marked with \"magenta\" color). " +
            "Some of path generating techniques will not work correctly with that graph!");
          vertexStyle = vertexStyle.withBorderColor(MAGENTA);
        }
        appendVertex(str, id, v.getName(), v.getDescription(), vertexStyle, v.getActions(), new ArrayList<>(outdegrees.getOrDefault(v, emptyMap()).values()),
          indegrees.getOrDefault(v, emptyList()));
      }

      str.append("  </graph>").append(newLine);
      str.append("</graphml>").append(newLine);

      graphmlStr.append(str);
    }
    return graphmlStr.toString();
  }


  @Override
  public void write(List<Context> contexts, Path path) throws IOException {
    File folder = path.toFile().getAbsoluteFile();
    folder.mkdirs();
    Path graphmlFile = Paths.get(folder.toString(), contexts.get(0).getModel().getName() + ".graphml");
    try (OutputStream outputStream = Files.newOutputStream(graphmlFile)) {
      outputStream.write(getAsString(contexts).getBytes());
    }
  }

  public void writeToSeparateFiles(Context context, Path path) throws IOException {
    File folder = path.toFile().getAbsoluteFile();
    folder.mkdirs();

    Set<String> groups = new HashSet<>();
    for (RuntimeVertex vertex : context.getModel().getVertices()) {
      if (vertex.getGroupName() != null) {
        groups.add(vertex.getGroupName());
      }
    }

    if (groups.size() > 1) {
      Map<RuntimeVertex, Map<RuntimeVertex, String>> outdegrees = new HashMap<>();
      Map<RuntimeVertex, List<IndegreeLabel>> indegrees = new HashMap<>();

      for (RuntimeEdge e : context.getModel().getEdges()) {
        RuntimeVertex src = e.getSourceVertex();
        RuntimeVertex dest = e.getTargetVertex();

        if (src == null || dest == null) {
          continue;
        }

        if (!Objects.equals(src.getGroupName(), dest.getGroupName())) {
          outdegrees.compute(src, (v, out) -> {
            Map<RuntimeVertex, String> outdegreeNames = out != null ? out : new HashMap<>();
            outdegreeNames.put(dest, e.getName());
            return outdegreeNames;
          });
          indegrees.compute(dest, (v, in) -> {
            List<IndegreeLabel> indegreeLabels = in != null ? in : new ArrayList<>();
            indegreeLabels.add(new IndegreeLabel(e.getName(), e.getDescription(), e.getGuard(), e.getWeight(), src));
            return indegreeLabels;
          });
        }
      }
      // Split different by meaning indegrees, replacing their names
      for (Map.Entry<RuntimeVertex, List<IndegreeLabel>> e : indegrees.entrySet()) {
        List<IndegreeLabel> byHash = new ArrayList<>(e.getValue().stream()
          .collect(toMap(identity(), identity(), IndegreeLabel::merge)).values());
        Map<String, List<IndegreeLabel>> byName = byHash.stream()
          .collect(groupingBy(IndegreeLabel::getName));

        for (Map.Entry<String, List<IndegreeLabel>> group : byName.entrySet()) {
          List<IndegreeLabel> value = group.getValue();
          if (value.size() > 1) {
            for (int suffix = 0; suffix < value.size(); suffix++) {
              String newName = group.getKey() + "$" + (suffix + 1);
              IndegreeLabel label = value.get(suffix);
              for (RuntimeVertex matchedOutdegree : label.getMatchingOutdegrees()) {
                outdegrees.get(matchedOutdegree).replace(e.getKey(), group.getKey(), newName);
              }
              e.getValue().remove(label);
              e.getValue().add(suffix, label.withName(newName));
            }
          }
        }
      }
      for (String group : groups) {
        Path graphmlFile = Paths.get(folder.toString(), getValidFileName(group) + ".graphml");
        try (OutputStream outputStream = Files.newOutputStream(graphmlFile)) {
          outputStream.write(getAsString(singletonList(context), group, outdegrees, indegrees).getBytes());
        }
      }
    } else {
      throw new IllegalStateException("Can not write to separate files, not enough groups");
    }
  }

  private static String getValidFileName(String fileName) {
    String newFileName = fileName.replace("^\\.+", "").replaceAll("[\\\\/:*?\"<>|]", "_");
    if (newFileName.isEmpty()) {
      throw new IllegalStateException(
        "File Name " + fileName + " results in a empty fileName!");
    }
    return newFileName;
  }

  private static class AddResult<T> {
    final Set<T> elements = new HashSet<>();
    T start = null;
  }

  private AddResult<Vertex> addVertices(Model model, GraphmlDocument document, Map<String, Vertex> elements) throws XmlException {
    return addVertices(model, document, elements, ArrayListMultimap.create(), ArrayListMultimap.create());
  }

  private AddResult<Vertex> addVertices(
    Model model,
    GraphmlDocument document,
    Map<String, Vertex> elements,
    Multimap<String, IndegreeVertex> indegrees,
    Multimap<String, Vertex> outdegrees) throws XmlException {

    AddResult<Vertex> addResult = new AddResult<>();
    ListOrderedMap<String, Deque<XmlObject>> groupedWorkQueue = listOrderedMap(new HashMap<>());
    groupedWorkQueue.put(
      null,
      new ArrayDeque<>(asList(document.selectPath(NAMESPACE + "$this/xq:graphml/xq:graph/xq:node"))));

    List<KeyType> keys = getKeyArray(document);
    Map<String, KeyType> propKeys = new HashMap<>();
    for (KeyType key : keys) {
      if (key.getFor() == KeyForTypeImpl.NODE && !key.isSetYfilesType()) {
        propKeys.put(key.getId(), key);
      }
    }

    String overGroupName = (String) document.documentProperties().get(OVER_GROUP);
    String groupName = (String) document.documentProperties().get(GROUP_NAME);
    final String specifiedGroupName = groupName;
    final String specifiedOverGroup = overGroupName;

    while (!groupedWorkQueue.isEmpty()) {
      Deque<XmlObject> workQueue = groupedWorkQueue.values().iterator().next();
      while (!workQueue.isEmpty()) {
        XmlObject object = workQueue.pop();
        if (object instanceof NodeType) {
          NodeType node = (NodeType) object;
          if (0 < node.getGraphArray().length) {
            data:
            for (DataType data : node.getDataArray()) {
              XmlObject[] objects = data.selectPath(Y + "$this//y:NodeLabel");
              if (objects instanceof NodeLabelType[]) {
                NodeLabelType[] labels = (NodeLabelType[]) objects;
                for (NodeLabelType label : labels) {
                  String g = ((NodeLabelTypeImpl) label).getStringValue();
                  if (g != null && !g.isEmpty()) {
                    for (String part : g.split("[\r\n]")) {
                      if (!part.trim().isEmpty()) {
                        if (null != groupedWorkQueue.keySet().iterator().next()) {
                          overGroupName = groupedWorkQueue.keySet().iterator().next();
                        }
                        groupName = part;
                        break data;
                      }
                    }
                  }
                }
              }
            }
            Deque<XmlObject> oldValue = groupedWorkQueue.get(groupName);
            Deque<XmlObject> queue;
            if (oldValue != null) {
              queue = oldValue;
            } else {
              queue = new ArrayDeque<>();
            }
            for (GraphType subGraph : node.getGraphArray()) {
              queue.addAll(asList(subGraph.getNodeArray()));
            }
            if (null == overGroupName) {
              groupedWorkQueue.put(groupName, queue);
            } else {
              groupedWorkQueue.put(1, groupName, queue);
            }
          } else {
            String group = groupedWorkQueue.keySet().iterator().next();
            Vertex vertex = new Vertex()
              .setGroupName(specifiedGroupName != null ? specifiedGroupName : group)
              .setOverGroup(specifiedOverGroup != null ? specifiedOverGroup : overGroupName);
            for (Map.Entry<String, KeyType> entry : propKeys.entrySet()) {
              KeyType value = entry.getValue();
              if (value.isSetDefault()) {
                vertex.setProperty(value.getAttrName(), ((KeyTypeImpl) value).getStringValue().trim());
              }
            }
            for (DataType data : node.getDataArray()) {
              String propName;
              String propCurrentValue;
              String key = data.getKey();
              if (propKeys.containsKey(key)) {
                KeyType currentKey = propKeys.get(key);
                propName = currentKey.getAttrName();
                propCurrentValue = ((DataTypeImpl) data).getStringValue().trim();
                vertex.setProperty(propName, propCurrentValue);
              }

              if (linkStyles) {
                XmlObject[] objects = data.selectPath(Y + "$this//y:GenericNode");
                if (objects instanceof GenericNodeType[]) {
                  GenericNodeType[] labels = (GenericNodeType[]) objects;
                  for (GenericNodeType label : labels) {
                    GeometryType geometry = label.getGeometry();
                    NodeLabelType nodeLabel;
                    try {
                      nodeLabel = label.getNodeLabelArray()[0];
                    } catch (IndexOutOfBoundsException e) {
                      throw new IndexOutOfBoundsException("Node label not found for node with key=\"" + key + "\"");
                    }
                    VertexStyle style = new VertexStyle(
                      new Configuration(label.getConfiguration()),
                      new Geometry(geometry.getWidth(), geometry.getHeight(), geometry.getX(), geometry.getY()),
                      new Fill(label.getFill().getColor(), label.getFill().getColor2()),
                      new Border(label.getBorderStyle().getColor(), new LineType(label.getBorderStyle().getType().toString()), label.getBorderStyle().getWidth()),
                      new Label(
                        new Geometry(nodeLabel.getWidth(), nodeLabel.getHeight(), nodeLabel.getX(), nodeLabel.getY()),
                        new Alignment(nodeLabel.getAlignment().toString()),
                        new FontFamily(nodeLabel.getFontFamily()),
                        new FontStyle(nodeLabel.getFontStyle().toString()),
                        nodeLabel.getFontSize(),
                        new TextColor(nodeLabel.getTextColor()),
                        nodeLabel.getLineColor(),
                        nodeLabel.getBackgroundColor()
                      )
                    );
                    vertex.setStyle(style);
                  }
                }
              }

              if (0 < data.getDomNode().getChildNodes().getLength()) {
                if (isSupportedNode(data.xmlText())) {
                  StringBuilder label = new StringBuilder();
                  com.yworks.xml.graphml.NodeType nodeType = getSupportedNode(data.xmlText());
                  if (nodeType == null) {
                    throw new XmlException("Expected a valid vertex");
                  }

                  for (NodeLabelType nodeLabel : nodeType.getNodeLabelArray()) {
                    label.append(((NodeLabelTypeImpl) nodeLabel).getStringValue());
                  }
                  YEdVertexParser parser = new YEdVertexParser(getTokenStream(label.toString(), object.xmlText()));
                  parser.removeErrorListeners();
                  parser.addErrorListener(YEdDescriptiveErrorListener.INSTANCE);
                  YEdVertexParser.ParseContext parseContext = parser.parse();

                  vertex.setProperty("x", nodeType.getGeometry().getX());
                  vertex.setProperty("y", nodeType.getGeometry().getY());
                  if (null != parseContext.start()) {
                    elements.put(node.getId(), vertex);
                    vertex.setId(node.getId());
                    addResult.start = vertex;
                  } else {
                    for (YEdVertexParser.FieldContext field : parseContext.field()) {
                      if (null != field.names()) {
                        vertex.setName(field.names().getText());
                      }
                      if (null != field.shared() && null != field.shared().Identifier()) {
                        vertex.setSharedState(field.shared().Identifier().getText());
                      }
                      if (null != field.indegrees()) {
                        vertex.setIndegrees(true);
                        for (YEdVertexParser.IndegreeContext indegreeContext : field.indegrees().indegreeList().indegree()) {
                          String description = "";
                          CodeTag codeTag = null;
                          if (null != indegreeContext.description()) {
                            description = indegreeContext.description().getText();
                            if (null != indegreeContext.description().code()) {
                              if (null != indegreeContext.description().code().voidExpression()
                                && null != indegreeContext.description().code().voidExpression().voidMethod()) {
                                codeTag = new CodeTagParser().parseIndegree(
                                  indegreeContext.description().code().voidExpression().voidMethod());
                              }
                            }
                          }
                          Guard guard = indegreeContext.guard() != null ? new Guard(indegreeContext.guard().getText()) : null;
                          double weight;
                          try {
                            weight = indegreeContext.weight() != null ? parseDouble(indegreeContext.weight().Value().getText()) : 1.0;
                          } catch (NumberFormatException e) {
                            throw new NumberFormatException("For input WEIGHT string: \"" + indegreeContext.weight().getText() + "\"");
                          }
                          indegrees.put(indegreeContext.element().getText(), new IndegreeVertex(vertex, description, guard, weight, codeTag));
                        }
                      }
                      if (null != field.outdegrees()) {
                        vertex.setOutdegrees(true);
                        for (YEdVertexParser.OutdegreeContext outdegreeContext : field.outdegrees().outdegreeList().outdegree()) {
                          outdegrees.put(outdegreeContext.element().getText(), vertex);
                        }
                      }
                      if (null != field.reqtags()) {
                        vertex.setRequirements(convertVertexRequirement(field.reqtags().reqtagList().reqtag()));
                      }
                      if (null != field.actions()) {
                        model.addActions(convertVertexAction(field.actions().action()));
                      }
                      if (null != field.sets()) {
                        vertex.setSetActions(convertVertexAction(field.sets().set()));
                      }
                      if (null != field.blocked()) {
                        vertex.setProperty("blocked", true);
                      }
                      if (null != field.description()) {
                        vertex.setDescription(field.description().getText());

                        if (null != field.description().code()) {
                          if (null != field.description().code().voidExpression()
                            && null != field.description().code().voidExpression().voidMethod()) {
                            vertex.setCodeTag(new CodeTagParser().parse(
                              field.description().code().voidExpression().voidMethod()));
                          }
                          if (null != field.description().code().booleanMethod()) {
                            vertex.setCodeTag(new CodeTagParser().parse(
                              field.description().code().booleanMethod()));
                          }
                        }
                      }
                    }
                    elements.put(node.getId(), vertex);
                    vertex.setId(node.getId());
                    model.addVertex(vertex);
                    addResult.elements.add(vertex);
                  }
                }
              }
            }
          }
        }
      }
      groupedWorkQueue.remove(groupedWorkQueue.keySet().iterator().next());
      groupName = null;
    }
    return addResult;
  }

  private boolean isSupportedNode(String xml) {
    return xml.contains("GenericNode")
      || xml.contains("ShapeNode")
      || xml.contains("GenericGroupNode")
      || xml.contains("GroupNode")
      || xml.contains("ImageNode")
      || xml.contains("TableNode");
  }

  private com.yworks.xml.graphml.NodeType getSupportedNode(String xml) throws XmlException {
    if (xml.contains("GenericNode")) {
      return GenericNodeDocument.Factory.parse(xml).getGenericNode();
    } else if (xml.contains("ShapeNode")) {
      return ShapeNodeDocument.Factory.parse(xml).getShapeNode();
    } else if (xml.contains("GenericGroupNode")) {
      return GenericGroupNodeDocument.Factory.parse(xml).getGenericGroupNode();
    } else if (xml.contains("GroupNode")) {
      return GroupNodeDocument.Factory.parse(xml).getGroupNode();
    } else if (xml.contains("ImageNode")) {
      return ImageNodeDocument.Factory.parse(xml).getImageNode();
    } else if (xml.contains("TableNode")) {
      return TableNodeDocument.Factory.parse(xml).getTableNode();
    }
    throw new ContextFactoryException("Unsupported node type: " + xml);
  }

  private List<KeyType> getKeyArray(GraphmlDocument document) {
    if (document.getGraphml() != null) {
      return asList(document.getGraphml().getKeyArray());
    }
    return emptyList();
  }

  private class SingleIndegrees extends HashMap<Vertex, List<Edge>> {

    public List<Edge> update(Vertex vertex, Edge indegree) {
      List<Edge> single = new ArrayList<>(asList(indegree));
      return merge(vertex, single, (v1, v2) -> {v1.addAll(v2); return v1;});
    }

    public Edge into(Vertex vertex) {
      List<Edge> edges = getOrDefault(vertex, null);
      return edges != null && !edges.isEmpty() ? edges.remove(0) : null;
    }

  }

  private Edge addEdges(Model model, GraphmlDocument document, Map<String, Vertex> elements, Vertex startVertex) throws XmlException {
    Edge startEdge = null;
    List<YEdDataset> datasets = new ArrayList<>();

    List<KeyType> keys = getKeyArray(document);
    Map<String, KeyType> propKeys = new HashMap<>();
    for (KeyType key : keys) {
      if (key.getFor() == KeyForTypeImpl.EDGE && !key.isSetYfilesType()) {
        propKeys.put(key.getId(), key);
      }
    }

    SingleIndegrees incoming = new SingleIndegrees();

    for (XmlObject object : document.selectPath(NAMESPACE + "$this/xq:graphml/xq:graph/xq:edge")) {
      if (object instanceof org.graphdrawing.graphml.xmlns.EdgeType) {
        org.graphdrawing.graphml.xmlns.EdgeType edgeType = (org.graphdrawing.graphml.xmlns.EdgeType) object;
        Edge edge = new Edge();
        for (Map.Entry<String, KeyType> entry : propKeys.entrySet()) {
          KeyType value = entry.getValue();
          if (value.isSetDefault()) {
            edge.setProperty(value.getAttrName(), ((KeyTypeImpl) value).getStringValue().trim());
          }
        }
        for (DataType data : edgeType.getDataArray()) {
          String propName;
          String propCurrentValue;
          String key = data.getKey();
          if (propKeys.containsKey(key)) {
            KeyType currentKey = propKeys.get(key);
            propName = currentKey.getAttrName();
            propCurrentValue = ((DataTypeImpl) data).getStringValue().trim();
            edge.setProperty(propName, propCurrentValue);
          }

          if (linkStyles) {
            XmlObject[] objects = new XmlObject[]{};
            for (String edgeName : new String[]{"BezierEdge", "PolyLineEdge", "GenericEdge",
                                                "ArcEdge", "QuadCurveEdge", "SplineEdge"}) {
              objects = data.selectPath(Y + "$this//y:" + edgeName);
              if (objects.length > 0) {
                break;
              }
            }

            if (objects instanceof EdgeType[]) {
              EdgeType[] bezierEdges = (EdgeType[]) objects;
              for (EdgeType bezierEdge : bezierEdges) {
                LineStyleType lineStyle = bezierEdge.getLineStyle();

                LineStyle style = new LineStyle(
                  new LineStyle.LineType(lineStyle.getType().toString()),
                  new LineStyle.LineColor(lineStyle.getColor()),
                  lineStyle.getWidth()
                );
                edge.setStyle(style);
              }
            }
          }

          if (0 < data.getDomNode().getChildNodes().getLength()) {
            if (isSupportedEdge(data.xmlText())) {
              StringBuilder label = new StringBuilder();
              com.yworks.xml.graphml.EdgeType supportedEdge = getSupportedEdge(data.xmlText());
              if (supportedEdge != null) {
                for (EdgeLabelType edgeLabel : supportedEdge.getEdgeLabelArray()) {
                  label.append(((EdgeLabelTypeImpl) edgeLabel).getStringValue());
                }
              }
              YEdEdgeParser parser = new YEdEdgeParser(getTokenStream(label.toString(), object.xmlText()));
              parser.removeErrorListeners();
              parser.addErrorListener(YEdDescriptiveErrorListener.INSTANCE);
              ParseContext parseContext = parser.parse();

              Vertex sourceVertex = elements.get(edgeType.getSource());
              if (null != sourceVertex) {
                edge.setSourceVertex(sourceVertex);
              }
              Vertex targetVertex = elements.get(edgeType.getTarget());
              if (null != targetVertex) {
                edge.setTargetVertex(targetVertex);
                for (Action set : targetVertex.getSetActions()) {
                  edge.addAction(set);
                }
              }
              for (FieldContext field : parseContext.field()) {
                if (null != field.names()) {
                  if (null != field.names().nameList()) {
                    edge.setName(field.names().getText());
                  } else if (null != field.names().nameArgList()) {
                    String name = field.names().nameArgList().IDENTIFIER_ARG().getText();
                    edge.setName(name.contains("{") ? name.substring(0, name.lastIndexOf('{')).trim() : name);
                    Argument.List arguments = new Argument.List();
                    YEdDataset dataset = new YEdDataset(edge.getSourceVertex(), edge.getTargetVertex());
                    for (YEdEdgeParser.LabelArgumentContext ctx : field.names().nameArgList().labelArgList().labelArgument()) {
                      String identifier = ctx.parameterName().IDENTIFIER_NAME().getText();
                      if (null != ctx.stringVariable()) {
                        arguments.add(new Argument(STRING, identifier, unquote(ctx.stringVariable().getText())));
                      } else if (null != ctx.booleanVariable()) {
                        arguments.add(new Argument(BOOLEAN, identifier, ctx.booleanVariable().getText()));
                      } else if (null != ctx.numberVariable()) {
                        arguments.add(new Argument(NUMBER, identifier, ctx.numberVariable().getText()));
                      } else {
                        throw new IllegalStateException("Can not parse dataset variable " + field.names().nameArgList().getText());
                      }
                    }
                    dataset.addArguments(arguments);
                    datasets.add(dataset);
                    edge.setArguments(arguments);
                  } else {
                    throw new IllegalStateException("Can not parse edge name: " + field.getText());
                  }
                }
                if (null != field.guard()) {
                  edge.setGuard(new Guard(field.guard().getText()));
                }
                if (null != field.actions()) {
                  edge.addActions(convertEdgeAction(field.actions().action()));
                }
                if (null != field.reqtags()) {
                  edge.setRequirements(convertEdgeRequirement(field.reqtags().reqtagList().reqtag()));
                }
                if (null != field.blocked()) {
                  edge.setProperty("blocked", true);
                }
                if (null != field.weight() && null != field.weight().Value()) {
                  edge.setWeight(parseDouble(field.weight().Value().getText()));
                }
                if (null != field.dependency() && null != field.dependency().Value()) {
                  edge.setDependency(parseInt((field.dependency().Value().getText())));
                }
                if (null != field.description()) {
                  edge.setDescription(field.description().getText());

                  if (null != field.description().code()) {
                    if (null != field.description().code().voidExpression()
                      && null != field.description().code().voidExpression().voidMethod()) {
                      edge.setCodeTag(new CodeTagParser().parse(
                        field.description().code().voidExpression().voidMethod()));
                    }
                  }
                }
                if (null != field.dataset()) {
                  List<String> argumentNames = new ArrayList<>();
                  for (YEdEdgeParser.TableHeaderCellContext cellContext : field.dataset().htmlTable().tableHeader().tableHeaderCell()) {
                    argumentNames.add(cellContext.Identifier().getText());
                  }
                  YEdDataset dataset = new YEdDataset(edge.getSourceVertex(), edge.getTargetVertex());
                  for (int i = 0; i < field.dataset().htmlTable().tableRow().size(); i++) {
                    YEdEdgeParser.TableRowContext rowContext = field.dataset().htmlTable().tableRow().get(i);
                    Argument.List argumentsRow = new Argument.List();
                    for (int j = 0; j < rowContext.tableBodyCell().size(); j++) {
                      YEdEdgeParser.TableBodyCellContext cellContext = rowContext.tableBodyCell().get(j);
                      String name = argumentNames.get(j);
                      if (null != cellContext.booleanValue()) {
                        argumentsRow.add(new Argument(BOOLEAN, name, cellContext.booleanValue().BOOLEAN().getText()));
                      } else if (null != cellContext.stringValue()) {
                        argumentsRow.add(new Argument(STRING, name, cellContext.stringValue().Identifier().getText()));
                      } else if (null != cellContext.numericValue()) {
                        argumentsRow.add(new Argument(NUMBER, name, cellContext.numericValue().Value().getText()));
                      } else if (null != cellContext.stringQuote()) {
                        String unquotedText = unquote(cellContext.stringQuote().JS_LITERAL().getText());
                        argumentsRow.add(new Argument(STRING, name, unquotedText));
                      } else {
                        throw new IllegalStateException("Can not determine dataset table field type of: \""
                          + cellContext.getText() + "\"");
                      }
                    }
                    dataset.addArguments(argumentsRow);
                  }
                  datasets.add(dataset);
                }
              }
              Vertex edgeTarget = edge.getTargetVertex();
              if (null != edgeTarget
                && (null != edge.getName() || datasets.stream().noneMatch(ds -> ds.hasTarget(edgeTarget)))) {

                if (null != startVertex &&
                  null != edgeType.getSource() &&
                  edgeType.getSource().equals(startVertex.getId())) {
                  edge.setSourceVertex(null);
                  edge.setId(edgeType.getId());
                  model.addEdge(edge);
                  incoming.update(edgeTarget, edge);
                  startEdge = edge;
                } else if (null != edge.getSourceVertex()) {
                  edge.setId(edgeType.getId());
                  model.addEdge(edge);
                  incoming.update(edgeTarget, edge);
                }
              }
            }
          }
        }
      }
    }

    nextDataset:
    for (YEdDataset dataset : datasets) {
      Vertex[] target = new Vertex[dataset.getArgumentLists().size()];
      @SuppressWarnings("unchecked")
      Map<String, TypePrefix>[] argumentValues = new Map[dataset.getArgumentLists().size()];
      Edge e, closer;
      for (e = closer = incoming.into(dataset.getTarget()); e != null; e = incoming.into(e.getSourceVertex())) {
        boolean reachedStart = dataset.getSource().equals(e.getSourceVertex());
        boolean multipleRows = null != dataset.getArgumentLists() && dataset.getArgumentLists().size() > 1;
        for (int i = dataset.getArgumentLists().size() - 1; i >= 0; i--) {
          Argument.List arguments = dataset.getArgumentLists().get(i);
          if (null == argumentValues[i]) {
            argumentValues[i] = null != e.getCodeTag()
              ? arguments.stream().collect(toMap(Argument::getName, Argument::getType))
              : emptyMap();
          }
          if (i == 0) {
            e.setArguments(arguments);
            e.getTargetVertex().addArguments(arguments);
            if (reachedStart && multipleRows) {
              e.setGuard(guardDataset(closer.getTargetVertex().getName(), i));
            }
            if (null != e.getSourceVertex().getCodeTag()) {
              parametrize(e.getSourceVertex().getCodeTag().getMethod(), argumentValues[i]);
            }
            if (!argumentValues[i].isEmpty() && null != e.getCodeTag()) {
              parametrize(e.getCodeTag().getMethod(), argumentValues[i]);
            }
          } else {
            Edge edgeCopy = e.copy()
              .setArguments(arguments)
              .setId(e.getId() + "_" + i);
            if (null != target[i]) {
              edgeCopy.setTargetVertex(target[i]);
            }
            edgeCopy.getTargetVertex().addArguments(arguments);
            if (!argumentValues[i].isEmpty() && null != edgeCopy.getCodeTag()) {
              parametrize(edgeCopy.getCodeTag().getMethod(), argumentValues[i]);
            }
            if (reachedStart && multipleRows) {
              edgeCopy.setGuard(guardDataset(closer.getTargetVertex().getName(), i));
              target[i] = e.getSourceVertex();
            } else {
              target[i] = e.getSourceVertex().copy();
              if (null != target[i].getCodeTag() && null != target[i].getCodeTag()) {
                parametrize(target[i].getCodeTag().getMethod(), argumentValues[i]);
              }
            }
            edgeCopy.setSourceVertex(target[i]);
            model.addEdge(edgeCopy);
          }
        }
        if (reachedStart) {
          if (multipleRows){
            for (Action action : initDataset(closer.getTargetVertex().getName(), dataset.getArgumentLists())) {
              e.getSourceVertex().addSetAction(action);
            }
          }
          continue nextDataset;
        }
      }
      throw new IllegalStateException("Dataset edge \"" + e  + "\" parsing error");
    }

    return startEdge;
  }

  private <T extends CodeTag.AbstractMethod> void parametrize(T method, Map<String, TypePrefix> argumentTypes) {
    ListIterator<CodeTag.Expression> listIterator = method.getArguments().listIterator();
    while (listIterator.hasNext()) {
      CodeTag.Expression expression = listIterator.next();
      TypePrefix matchedType;
      if (expression instanceof CodeTag.AbstractMethod) {
        parametrize((CodeTag.AbstractMethod) expression, argumentTypes);

      } else if (expression instanceof CodeTag.DatasetVariable
        && (matchedType = argumentTypes.get(((CodeTag.DatasetVariable<String>) expression).result())) != null) {
        listIterator.set(new CodeTag.TypedDatasetVariable<String>(((CodeTag.DatasetVariable<String>) expression).result(), matchedType));
      }
    }
  }

  private List<Action> initDataset(String datasetVariable, List<Argument.List> arguments) {
    StringBuilder sb = new StringBuilder("gw.ds." + datasetVariable + " = [");
    sb.append(arguments.stream().map(row -> row
      .stream().map(argument -> argument.getName() + ": \"" + argument.getValue() + "\"")
      // additionally set guard lock to open on that dataset value,
      // so by default any dataset path could be passed through
      .collect(joining(", ")) + ", $open: true").collect(joining("}, {", "{", "}")));
    return Arrays.asList(new Action("var gw = gw || {ds: {}};"), new Action(sb.append("];").toString()));
  }

  /**
   * @param datasetVariable JS variable name
   * @see <a href="http://web.archive.org/web/20161108071447/http://blog.osteele.com/posts/2007/12/cheap-monads/">Explanation</a>
   */
  private Guard guardDataset(String datasetVariable, int id) {
    return new Guard("typeof gw != \"undefined\" && (((gw || {}).ds || {})." + datasetVariable + " || {})[" + id + "].$open");
  }

  private boolean isSupportedEdge(String xml) {
    return xml.contains("PolyLineEdge")
      || xml.contains("GenericEdge")
      || xml.contains("ArcEdge")
      || xml.contains("QuadCurveEdge")
      || xml.contains("SplineEdge")
      || xml.contains("BezierEdge");
  }

  private com.yworks.xml.graphml.EdgeType getSupportedEdge(String xml) throws XmlException {
    if (xml.contains("GenericEdge")) {
      return GenericEdgeDocument.Factory.parse(xml).getGenericEdge();
    } else if (xml.contains("PolyLineEdge")) {
      return PolyLineEdgeDocument.Factory.parse(xml).getPolyLineEdge();
    } else if (xml.contains("ArcEdge")) {
      return ArcEdgeDocument.Factory.parse(xml).getArcEdge();
    } else if (xml.contains("QuadCurveEdge")) {
      return QuadCurveEdgeDocument.Factory.parse(xml).getQuadCurveEdge();
    } else if (xml.contains("SplineEdge")) {
      return SplineEdgeDocument.Factory.parse(xml).getSplineEdge();
    } else if (xml.contains("BezierEdge")) {
      return BezierEdgeDocument.Factory.parse(xml).getBezierEdge();
    }
    throw new ContextFactoryException("Unsupported edge type: " + xml);
  }

  private List<Action> convertEdgeAction(List<ActionContext> actionContexts) {
    List<Action> actions = new ArrayList<>();
    for (ActionContext actionContext : actionContexts) {
      actions.add(new Action(actionContext.getText() + ";"));
    }
    return actions;
  }

  private <T extends ParseTree> List<Action> convertVertexAction(List<T> setContexts) {
    List<Action> actions = new ArrayList<>();
    for (T context : setContexts) {
      actions.add(new Action(context.getText()));
    }
    return actions;
  }

  private Set<Requirement> convertEdgeRequirement(List<ReqtagContext> reqtagContexts) {
    Set<Requirement> requirements = new HashSet<>();
    for (ReqtagContext reqtagContext : reqtagContexts) {
      requirements.add(new Requirement(reqtagContext.getText()));
    }
    return requirements;
  }

  private Set<Requirement> convertVertexRequirement(List<YEdVertexParser.ReqtagContext> reqtagContexts) {
    Set<Requirement> requirements = new HashSet<>();
    for (YEdVertexParser.ReqtagContext reqtagContext : reqtagContexts) {
      requirements.add(new Requirement(reqtagContext.getText()));
    }
    return requirements;
  }

  private CommonTokenStream getTokenStream(String label, String xmlText) {
    CharStream inputStream = CharStreams.fromString(label);
    YEdLabelLexer lexer = new YEdLabelLexer(inputStream);
    lexer.removeErrorListeners();
    YEdDescriptiveErrorListener listener = YEdDescriptiveErrorListener.INSTANCE;

    Pattern pattern = Pattern.compile("id=\"([a-z][0-9]+(::[a-z][0-9]+)*)\"");
    Matcher matcher = pattern.matcher(xmlText);
    String id;

    if (matcher.find()) {
      id = matcher.group(0);
    } else {
        id = "";
    }

    String msg = "Error at element with id [" + id + "]";

    if(xmlText.contains("(No text specified!)")) {
      msg += ". No text specified for element label.";
    }

    listener.setCurrentLabel(msg);
    lexer.addErrorListener(listener);
    return new CommonTokenStream(lexer);
  }

  private String unquote(String quotedString) {
    return quotedString.replaceAll("^\"|\"$", "");
  }

  private static String joinWithSpace(Object ... objects) {
    return Stream.of(objects).map(Object::toString).filter(s -> !s.isEmpty()).collect(joining(" ", " ", ""));
  }
}
