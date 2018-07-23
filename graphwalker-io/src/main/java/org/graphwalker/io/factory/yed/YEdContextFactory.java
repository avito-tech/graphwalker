package org.graphwalker.io.factory.yed;

/*
 * #%L
 * GraphWalker Input/Output
 * %%
 * Copyright (C) 2005 - 2014 GraphWalker
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;
import com.yworks.xml.graphml.ArcEdgeDocument;
import com.yworks.xml.graphml.BezierEdgeDocument;
import com.yworks.xml.graphml.EdgeLabelType;
import com.yworks.xml.graphml.GenericEdgeDocument;
import com.yworks.xml.graphml.GenericGroupNodeDocument;
import com.yworks.xml.graphml.GenericNodeDocument;
import com.yworks.xml.graphml.GroupNodeDocument;
import com.yworks.xml.graphml.ImageNodeDocument;
import com.yworks.xml.graphml.NodeLabelType;
import com.yworks.xml.graphml.PolyLineEdgeDocument;
import com.yworks.xml.graphml.QuadCurveEdgeDocument;
import com.yworks.xml.graphml.ShapeNodeDocument;
import com.yworks.xml.graphml.SplineEdgeDocument;
import com.yworks.xml.graphml.TableNodeDocument;
import com.yworks.xml.graphml.impl.EdgeLabelTypeImpl;
import com.yworks.xml.graphml.impl.NodeLabelTypeImpl;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.FilenameUtils;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.graphdrawing.graphml.xmlns.DataType;
import org.graphdrawing.graphml.xmlns.GraphType;
import org.graphdrawing.graphml.xmlns.GraphmlDocument;
import org.graphdrawing.graphml.xmlns.KeyType;
import org.graphdrawing.graphml.xmlns.NodeType;
import org.graphdrawing.graphml.xmlns.impl.DataTypeImpl;
import org.graphdrawing.graphml.xmlns.impl.KeyForTypeImpl;
import org.graphdrawing.graphml.xmlns.impl.KeyTypeImpl;
import org.graphwalker.core.machine.Context;
import org.graphwalker.core.model.Action;
import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Edge.RuntimeEdge;
import org.graphwalker.core.model.Guard;
import org.graphwalker.core.model.Model;
import org.graphwalker.core.model.Requirement;
import org.graphwalker.core.model.Vertex;
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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.awt.Color.BLACK;
import static java.awt.Color.GREEN;
import static java.awt.Color.MAGENTA;
import static java.awt.Color.RED;
import static java.awt.Color.YELLOW;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;


/**
 * @author Nils Olsson
 */
public final class YEdContextFactory implements ContextFactory {

  private static final Logger logger = LoggerFactory.getLogger(YEdContextFactory.class);
  private static final String NAMESPACE = "declare namespace xq='http://graphml.graphdrawing.org/xmlns';";
  private static final String Y = "declare namespace y='http://www.yworks.com/xml/graphml';";
  private static final String FILE_TYPE = "graphml";
  private static final Set<String> SUPPORTED_TYPE = new HashSet<>(asList("**/*.graphml"));

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
        document.documentProperties().setSourceName(path.toFile().getName().replaceFirst("[.][^.]+$", ""));
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
              .setWeight(in.getWeight());
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

  private static void appendVertex(StringBuilder str, String id, String name, String description,
                                   List<Action> actions, List<String> outdegrees,
                                   List<IndegreeLabel> indegrees, Color col) {

    double scale = !actions.isEmpty() || (description != null && description.length() >= 50) ? 1.7 : 1.0;

    String newLine = System.lineSeparator();
    str.append("    <node id=\"" + id + "\">").append(newLine);
    str.append("      <data key=\"d0\" >").append(newLine);
    str.append("        <y:ShapeNode >").append(newLine);
    str.append("          <y:Geometry  x=\"250.000\" y=\"150.000\" width=\"" + scale * 250.0 + "\" height=\"" + scale * 60 + "\"/>").append(newLine);
    str.append("          <y:Fill color=\"" + format("#%02x%02x%02x", col.getRed(), col.getGreen(), col.getBlue()) + "\"  transparent=\"false\"/>").append(newLine);
    str.append("          <y:BorderStyle type=\"line\" width=\"1.0\" color=\"#000000\" />").append(newLine);
    str.append("          <y:NodeLabel x=\"1.5\" y=\"5.500\" width=\"100.0\" height=\"20.000\" "
      + "visible=\"true\" alignment=\"center\" fontFamily=\"Dialog\" fontSize=\"12\" "
      + "fontStyle=\"plain\" textColor=\"#000000\" modelName=\"internal\" modelPosition=\"c\" autoSizePolicy=\"content\">"
      + name);

    if (description != null && !description.trim().isEmpty()) {
      String formattedDescription = description
        .replaceAll("([^\\\\])\\\\n", "$1\n")
        .replace("\\\\n", "\\n");
      str.append(newLine + "/* " + formattedDescription + " */");
    }

    if (!actions.isEmpty()) {
      str.append(newLine + "INIT: ");
      for (int i = 0; i < actions.size(); i++) {
        str.append(actions.get(i).getScript() + (i < actions.size() - 1 ? ", " : ";"));
      }
    }

    if (!outdegrees.isEmpty()) {
      str.append(newLine + "OUTDEGREE: ");
      for (int i = 0; i < outdegrees.size(); i++) {
        str.append(outdegrees.get(i) + (i < outdegrees.size() - 1 ? ", " : ";"));
      }
    }

    if (!indegrees.isEmpty()) {
      str.append(newLine + "INDEGREE: ");
      for (int i = 0; i < indegrees.size(); i++) {
        str.append(indegrees.get(i) + (i < indegrees.size() - 1 ? ", " : ";"));
      }
    }

    str.append("</y:NodeLabel>").append(newLine);
    str.append("          <y:Shape type=\"rectangle\"/>").append(newLine);
    str.append("        </y:ShapeNode>").append(newLine);
    str.append("      </data>").append(newLine);
    str.append("    </node>").append(newLine);
  }

  private static void appendEdge(StringBuilder str, String id, String srcId, String destId,
                                 String name, Guard guard, List<Action> actions, int dependency,
                                 String description, Double weight, Color col) {
    String newLine = System.lineSeparator();
    String colorCode = format("#%02x%02x%02x", col.getRed(), col.getGreen(), col.getBlue());

    str.append("    <edge id=\"" + id + "\" source=\"" + srcId + "\" target=\"" + destId + "\">").append(newLine);
    str.append("      <data key=\"d1\" >").append(newLine);
    str.append("        <y:PolyLineEdge >").append(newLine);
    str.append("          <y:Path sx=\"-23.75\" sy=\"15.0\" tx=\"-23.75\" ty=\"-15.0\">").append(newLine);
    str.append("            <y:Point x=\"273.0\" y=\"95.0\"/>").append(newLine);
    str.append("            <y:Point x=\"209.0\" y=\"95.0\"/>").append(newLine);
    str.append("            <y:Point x=\"209.0\" y=\"143.7\"/>").append(newLine);
    str.append("            <y:Point x=\"265.0\" y=\"143.7\"/>").append(newLine);
    str.append("          </y:Path>").append(newLine);
    str.append("          <y:LineStyle type=\"line\" width=\"1.0\" color=\"" + colorCode + "\" />").append(newLine);
    str.append("          <y:Arrows source=\"none\" target=\"standard\"/>").append(newLine);
    if (!name.isEmpty()) {
      String label = name;

      if (actions != null && !actions.isEmpty()) {
        label += " /";
        for (Action action : actions) {
          label += action.getScript();
        }
      }
      if (guard != null) {
        label += newLine + "[" + guard.getScript() + "]";
      }
      if (description != null && !description.trim().isEmpty()) {
        String formattedDescription = description
          .replaceAll("([^\\\\])\\\\n", "$1\n")
          .replace("\\\\n", "\\n");
        label += newLine + "/* " + formattedDescription + " */";
      }

      if (dependency != 0) {
        label += "\ndependency=" + dependency;
      }

      if (weight != null && weight < 1.0) {
        label += "\nweight=" + weight;
      }

      label = label.replaceAll("&", "&amp;");
      label = label.replaceAll("<", "&lt;");
      label = label.replaceAll(">", "&gt;");
      label = label.replaceAll("'", "&apos;");
      label = label.replaceAll("\"", "&quot;");

      str.append("          <y:EdgeLabel x=\"-148.25\" y=\"30.0\" width=\"169.0\" height=\"18.0\" "
        + "visible=\"true\" alignment=\"center\" fontFamily=\"Dialog\" fontSize=\"12\" "
        + "fontStyle=\"plain\" textColor=\"" + colorCode + "\" modelName=\"free\" modelPosition=\"anywhere\" "
        + "preferredPlacement=\"on_edge\" distance=\"2.0\" ratio=\"0.5\">" + label);
      str.append("</y:EdgeLabel>").append(newLine);
    }

    str.append("          <y:BendStyle smoothed=\"true\"/>").append(newLine);
    str.append("        </y:PolyLineEdge>").append(newLine);
    str.append("      </data>").append(newLine);
    str.append("    </edge>").append(newLine);
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
        appendVertex(str, "n" + n, "Start", null, emptyList(), emptyList(), emptyList(), GREEN);
        IndexedCollection<RuntimeVertex> destGroup = groupedVertices.getOrDefault(((RuntimeEdge) context.getNextElement()).getTargetVertex().getGroupName(), NO_GROUP);
        appendEdge(str, "e" + e, "n" + n, destGroup + uniqueVertices.get(((RuntimeEdge) context.getNextElement()).getTargetVertex()),
          context.getNextElement().getName(),
          ((RuntimeEdge) context.getNextElement()).getGuard(),
          context.getNextElement().getActions(),
          ((RuntimeEdge) context.getNextElement()).getDependency(),
          context.getNextElement().getDescription(),
          ((RuntimeEdge) context.getNextElement()).getWeight(),
          BLACK);
      }

      Set<RuntimeVertex> hasNoInput = new HashSet<>(context.getModel().getVertices());
      Set<RuntimeVertex> hasNoOutput = new HashSet<>(context.getModel().getVertices());

      for (RuntimeEdge e : context.getModel().getEdges()) {
        RuntimeVertex src = e.getSourceVertex();
        RuntimeVertex dest = e.getTargetVertex();

        if (src == null || dest == null) {
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
        if (g > 1) {
          srcId = groupedVertices.getOrDefault(src.getGroupName(), NO_GROUP) + srcId;
          destId = groupedVertices.getOrDefault(dest.getGroupName(), NO_GROUP) + destId;
        }

        appendEdge(str, id, srcId, destId,
          edgeName,
          e.hasGuard() ? e.getGuard() : null,
          e.hasActions() ? e.getActions() : emptyList(),
          e.getDependency(),
          e.getDescription(),
          e.getWeight(),
          color);
      }

      for (Map.Entry<String, IndexedCollection<RuntimeVertex>> group : groupedVertices.entrySet()) {
        if (group.getKey() != null && g > 1) {
          str.append("<node id=\"" + "n" + group.getValue().index + "\" yfiles.foldertype=\"group\">").append(newLine);
          str.append("<data key=\"d0\">").append(newLine);
          str.append("  <y:ProxyAutoBoundsNode>").append(newLine);
          str.append("    <y:Realizers active=\"0\">").append(newLine);
          str.append("      <y:GroupNode>").append(newLine);
          str.append("        <y:Geometry height=\"500.0\" width=\"561.25\" x=\"4000.0\" y=\"3000.0\"/>").append(newLine);
          str.append("        <y:Fill color=\"#F2F0D8\" transparent=\"false\"/>").append(newLine);
          str.append("        <y:BorderStyle color=\"#000000\" type=\"line\" width=\"1.0\"/>").append(newLine);
          str.append("        <y:NodeLabel alignment=\"right\" autoSizePolicy=\"node_width\" backgroundColor=\"#404040\" borderDistance=\"0.0\" fontFamily=\"Dialog\" fontSize=\"16\" fontStyle=\"plain\" hasLineColor=\"false\" height=\"25.0\" horizontalTextPosition=\"center\" iconTextGap=\"4\" modelName=\"internal\" modelPosition=\"t\" textColor=\"#FFFFFF\" verticalTextPosition=\"bottom\" visible=\"true\" width=\"455.0\" x=\"0.0\" y=\"0.0\">" + group.getKey() + "</y:NodeLabel>").append(newLine);
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
          str.append("        <y:NodeLabel alignment=\"right\" autoSizePolicy=\"node_width\" backgroundColor=\"#EBEBEB\" borderDistance=\"0.0\" fontFamily=\"Dialog\" fontSize=\"15\" fontStyle=\"plain\" hasLineColor=\"false\" height=\"24.0\" horizontalTextPosition=\"center\" iconTextGap=\"4\" modelName=\"internal\" modelPosition=\"t\" textColor=\"#000000\" verticalTextPosition=\"bottom\" visible=\"true\" width=\"60.0\" x=\"-4.0\" y=\"0.0\">" + group.getKey() + "</y:NodeLabel>").append(newLine);
          str.append("        <y:Shape type=\"roundrectangle\"/>").append(newLine);
          str.append("        <y:State closed=\"true\" closedHeight=\"50.0\" closedWidth=\"50.0\" innerGraphDisplayEnabled=\"false\"/>").append(newLine);
          str.append("        <y:Insets bottom=\"5\" bottomF=\"5.0\" left=\"5\" leftF=\"5.0\" right=\"5\" rightF=\"5.0\" top=\"5\" topF=\"5.0\"/>").append(newLine);
          str.append("        <y:BorderInsets bottom=\"0\" bottomF=\"0.0\" left=\"0\" leftF=\"0.0\" right=\"0\" rightF=\"0.0\" top=\"0\" topF=\"0.0\"/>").append(newLine);
          str.append("      </y:GroupNode>").append(newLine);
          str.append("    </y:Realizers>").append(newLine);
          str.append("  </y:ProxyAutoBoundsNode>").append(newLine);
          str.append("</data>").append(newLine);
          str.append("<graph edgedefault=\"directed\" id=\"" + "n" + group.getValue().index + ":\">").append(newLine);
        }

        for (RuntimeVertex v : group.getValue().items) {
          String id = group.getKey() != null && g > 1
            ? "n" + group.getValue().index + "::" + uniqueVertices.get(v)
            : uniqueVertices.get(v);
          Color color;
          if (hasNoInput.contains(v)) {
            logger.warn("Vertex " + v + " has no input edges (marked with \"magenta\" color). " +
              "It could not be tested!");
            color = MAGENTA;
          } else if (hasNoOutput.contains(v)) {
            logger.warn("Vertex " + v + " has no output edges (marked with \"red\" color). " +
              "Most of path generating techniques will not work correctly with that graph!");
            color = RED;
          } else {
            color = YELLOW;
          }
          appendVertex(str, id, v.getName(), v.getDescription(), v.getActions(), emptyList(), emptyList(), color);
        }
        if (group.getKey() != null && g > 1) {
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
        appendVertex(str, "n" + n, "Start", null, emptyList(), emptyList(), emptyList(), GREEN);
        IndexedCollection<RuntimeVertex> destGroup = groupedVertices.getOrDefault(((RuntimeEdge) context.getNextElement()).getTargetVertex().getGroupName(), NO_GROUP);
        appendEdge(str, "e" + e, "n" + n, destGroup + uniqueVertices.get(((RuntimeEdge) context.getNextElement()).getTargetVertex()),
          context.getNextElement().getName(),
          ((RuntimeEdge) context.getNextElement()).getGuard(),
          context.getNextElement().getActions(),
          ((RuntimeEdge) context.getNextElement()).getDependency(),
          context.getNextElement().getDescription(),
          ((RuntimeEdge) context.getNextElement()).getWeight(),
          BLACK);
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
          e.hasGuard() ? e.getGuard() : null,
          e.hasActions() ? e.getActions() : emptyList(),
          e.getDependency(),
          e.getDescription(),
          e.getWeight(),
          color);
      }

      for (RuntimeVertex v : groupedVertices.get(selectOnlyGroup).items) {
        String id = uniqueVertices.get(v);
        Color color;
        if (hasNoInput.contains(v)) {
          if (v.hasName() && v.getName().equalsIgnoreCase("start")) {
            color = GREEN;
          } else {
            logger.warn("Vertex " + v + " has no input edges (marked with \"magenta\" color). " +
              "It could not be tested!");
            color = MAGENTA;
          }
        } else if (hasNoOutput.contains(v)) {
          logger.warn("Vertex " + v + " has no output edges (marked with \"red\" color). " +
            "Most of path generating techniques will not work correctly with that graph!");
          color = RED;
        } else {
          color = YELLOW;
        }
        appendVertex(str, id, v.getName(), v.getDescription(), v.getActions(),
          new ArrayList<>(outdegrees.getOrDefault(v, emptyMap()).values()),
          indegrees.getOrDefault(v, emptyList()), color);
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
      outputStream.write(String.valueOf(getAsString(contexts)).getBytes());
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
    LinkedHashMap<String, Deque<XmlObject>> groupedWorkQueue = new LinkedHashMap<>();
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

    String sourceName = document.documentProperties().getSourceName();

    while (!groupedWorkQueue.isEmpty()) {
      Deque<XmlObject> workQueue = groupedWorkQueue.values().iterator().next();
      while (!workQueue.isEmpty()) {
        XmlObject object = workQueue.pop();
        if (object instanceof NodeType) {
          NodeType node = (NodeType) object;
          if (0 < node.getGraphArray().length) {
            String groupName = sourceName;
            if (groupName == null && groupedWorkQueue.keySet().iterator().next() == null) {
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
                          groupName = part;
                          break data;
                        }
                      }
                    }
                  }
                }
              }
            }
            groupedWorkQueue.compute(groupName, (g, prev) -> {
              Deque<XmlObject> queue = prev != null ? prev : new ArrayDeque<>();
              for (GraphType subgraph : node.getGraphArray()) {
                queue.addAll(asList(subgraph.getNodeArray()));
              }
              return queue;
            });
          } else {
            Vertex vertex = new Vertex();
            vertex.setGroupName(sourceName != null ? sourceName : groupedWorkQueue.keySet().iterator().next());
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
                  YEdVertexParser parser = new YEdVertexParser(getTokenStream(label.toString()));
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
                          String description = indegreeContext.description() != null ? indegreeContext.description().getText() : "";
                          Guard guard = indegreeContext.guard() != null ? new Guard(indegreeContext.guard().getText()) : null;
                          double weight;
                          try {
                            weight = indegreeContext.weight() != null ? parseDouble(indegreeContext.weight().Value().getText()) : 1.0;
                          } catch (NumberFormatException e) {
                            throw new NumberFormatException("For input WEIGHT string: \"" + indegreeContext.weight().getText() + "\"");
                          }
                          indegrees.put(indegreeContext.element().getText(), new IndegreeVertex(vertex, description, guard, weight));
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

  private Edge addEdges(Model model, GraphmlDocument document, Map<String, Vertex> elements, Vertex startVertex) throws XmlException {
    Edge startEdge = null;

    List<KeyType> keys = getKeyArray(document);
    Map<String, KeyType> propKeys = new HashMap<>();
    for (KeyType key : keys) {
      if (key.getFor() == KeyForTypeImpl.EDGE && !key.isSetYfilesType()) {
        propKeys.put(key.getId(), key);
      }
    }

    for (XmlObject object : document.selectPath(NAMESPACE + "$this/xq:graphml/xq:graph/xq:edge")) {
      if (object instanceof org.graphdrawing.graphml.xmlns.EdgeType) {
        org.graphdrawing.graphml.xmlns.EdgeType edgeType = (org.graphdrawing.graphml.xmlns.EdgeType) object;
        if (edgeType == null) {
          throw new XmlException("Expected a valid edge");
        }
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
          if (0 < data.getDomNode().getChildNodes().getLength()) {
            if (isSupportedEdge(data.xmlText())) {
              StringBuilder label = new StringBuilder();
              com.yworks.xml.graphml.EdgeType supportedEdge = getSupportedEdge(data.xmlText());
              if (supportedEdge != null) {
                for (EdgeLabelType edgeLabel : supportedEdge.getEdgeLabelArray()) {
                  label.append(((EdgeLabelTypeImpl) edgeLabel).getStringValue());
                }
              }
              YEdEdgeParser parser = new YEdEdgeParser(getTokenStream(label.toString()));
              parser.removeErrorListeners();
              parser.addErrorListener(YEdDescriptiveErrorListener.INSTANCE);
              YEdEdgeParser.ParseContext parseContext = parser.parse();

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
              for (YEdEdgeParser.FieldContext field : parseContext.field()) {
                if (null != field.names()) {
                  edge.setName(field.names().getText());
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
                }
              }
              if (null != edge.getTargetVertex()) {
                if (null != startVertex &&
                  null != edgeType.getSource() &&
                  edgeType.getSource().equals(startVertex.getId())) {
                  edge.setSourceVertex(null);
                  edge.setId(edgeType.getId());
                  model.addEdge(edge);
                  startEdge = edge;
                } else if (null != edge.getSourceVertex()) {
                  edge.setId(edgeType.getId());
                  model.addEdge(edge);
                }
              }
            }
          }
        }
      }
    }
    return startEdge;
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

  private List<Action> convertEdgeAction(List<YEdEdgeParser.ActionContext> actionContexts) {
    List<Action> actions = new ArrayList<>();
    for (YEdEdgeParser.ActionContext actionContext : actionContexts) {
      actions.add(new Action(actionContext.getText()));
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

  private Set<Requirement> convertEdgeRequirement(List<YEdEdgeParser.ReqtagContext> reqtagContexts) {
    Set<Requirement> requirements = new HashSet<>();
    for (YEdEdgeParser.ReqtagContext reqtagContext : reqtagContexts) {
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

  private CommonTokenStream getTokenStream(String label) {
    CharStream inputStream = CharStreams.fromString(label);
    YEdLabelLexer lexer = new YEdLabelLexer(inputStream);
    lexer.removeErrorListeners();
    lexer.addErrorListener(YEdDescriptiveErrorListener.INSTANCE);
    return new CommonTokenStream(lexer);
  }

}
