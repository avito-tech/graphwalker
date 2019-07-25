package org.graphwalker.dsl.antlr.dot;

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

import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Model;
import org.graphwalker.core.model.Vertex;
import org.graphwalker.dsl.dot.DOTBaseListener;
import org.graphwalker.dsl.dot.DOTParser;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DotModelListener extends DOTBaseListener {

  private Model model = new Model();
  private Map<String, Vertex> vertices = new HashMap<>();

  public Model getModel() {
    return model;
  }

  private Vertex createVertex(String id, String label) {
    if ("Start".equalsIgnoreCase(id) || "Start".equalsIgnoreCase(label)) {
      vertices.put(id, null);
      return null;
    }
    if (!vertices.containsKey(id)) {
      vertices.put(id, new Vertex().setId(id).setName(label != null ? label : id));
      model.addVertex(vertices.get(id));
    }
    return vertices.get(id);
  }

  @Override
  public void enterGraph(DOTParser.GraphContext context) {
    model.setName(context.id().getText());
  }

  @Override
  public void enterNode_stmt(DOTParser.Node_stmtContext context) {
    if (context.attr_list() != null) {
      Map<String, String> attributes = context.attr_list().a_list().stream()
        .map(DOTParser.A_listContext::getText)
        .map(item -> item.split("="))
        .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1].replace("\"", "")));
      String label = attributes.get("label");
      createVertex(context.node_id().id().getText(), label);
    } else {
      createVertex(context.node_id().id().getText(), null);
    }
  }

  @Override
  public void enterEdge_stmt(DOTParser.Edge_stmtContext context) {
    Vertex source = createVertex(context.node_id().id().getText(), null);
    for (DOTParser.Node_idContext node_id: context.edgeRHS().node_id()) {
      Vertex target = createVertex(node_id.getText(), null);
      Edge edge = new Edge()
        .setSourceVertex(source)
        .setTargetVertex(target);
      if (context.attr_list() != null) {
        Map<String, String> attributes = context.attr_list().a_list().stream()
          .map(DOTParser.A_listContext::getText)
          .map(item -> item.split("="))
          .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1].replace("\"", "")));
        String label = attributes.get("label");
        edge.setId(label).setName(label);
      }
      model.addEdge(edge);
      source = target;
    }
  }
}
