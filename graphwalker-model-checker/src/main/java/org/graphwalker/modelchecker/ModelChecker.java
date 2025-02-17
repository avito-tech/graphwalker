package org.graphwalker.modelchecker;

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
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Model;
import org.graphwalker.core.model.Vertex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by krikar on 2015-11-08.
 */
public class ModelChecker {

  private ModelChecker() {
  }

  /**
   * Checks the model for problems or any possible errors.
   * Any findings will be added to a list of strings.
   * <p/>
   * TODO: Implement a rule framework so that organisations and projects can create their own rule set (think model based code convention)
   *
   * @return A list of issues found in the runtime model
   */
  static public List<String> hasIssues(Model.RuntimeModel model) {
    List<String> issues = new ArrayList<>(ElementChecker.hasIssues(model));

    // Check that individual elements are valid
    for (Vertex.RuntimeVertex vertex : model.getVertices()) {
      issues.addAll(VertexChecker.hasIssues(vertex));
    }
    for (Edge.RuntimeEdge edge : model.getEdges()) {
      issues.addAll(EdgeChecker.hasIssues(edge));
    }

    // Check that ids are unique
    Set<String> ids = new HashSet<>();
    for (Element element : model.getElements()) {
      if (!ids.add(element.getId())) {
        if (element instanceof Edge.RuntimeEdge) {
          issues.add("Id of the edge is not unique: " + element.getId());
        } else {
          issues.add("Id of the vertex is not unique: " + element.getId());
        }
      }
    }

    // Check for unnamed selfloop edges.
    for (Edge.RuntimeEdge edge : model.getEdges()) {
      if (!edge.hasName() &&
          null != edge.getSourceVertex() &&
          null != edge.getTargetVertex() &&
          edge.getSourceVertex().equals(edge.getTargetVertex())) {
        issues.add("Vertex: " + edge.getSourceVertex() + ", have a unnamed self loop edge.");
      }
    }
    return issues;
  }
}
