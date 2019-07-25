package org.graphwalker.core.algorithm;

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

import org.graphwalker.core.common.Objects;
import org.graphwalker.core.model.Edge.RuntimeEdge;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Model;
import org.graphwalker.core.model.Path;
import org.graphwalker.core.model.Vertex.RuntimeVertex;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * @author Ivan Bonkin
 */
public class Dijkstra implements Algorithm {

  private final List<RuntimeEdge> edges;
  private Set<RuntimeVertex> settledNodes;
  private Set<RuntimeVertex> unSettledNodes;
  private Map<RuntimeVertex, RuntimeVertex> predecessors;
  private Map<RuntimeVertex, Integer> distance;

  public Dijkstra(Model.RuntimeModel model) {
    // create a copy of the array so that we can operate on this array
    this.edges = new ArrayList<>(model.getEdges());
  }

  public void execute(RuntimeVertex source) {
    settledNodes = new HashSet<RuntimeVertex>();
    unSettledNodes = new HashSet<RuntimeVertex>();
    distance = new HashMap<RuntimeVertex, Integer>();
    predecessors = new HashMap<RuntimeVertex, RuntimeVertex>();
    distance.put(source, 0);
    unSettledNodes.add(source);
    while (unSettledNodes.size() > 0) {
      RuntimeVertex node = getMinimum(unSettledNodes);
      settledNodes.add(node);
      unSettledNodes.remove(node);
      findMinimalDistances(node);
    }
  }

  private void findMinimalDistances(RuntimeVertex node) {
    List<RuntimeVertex> adjacentNodes = getNeighbors(node);
    for (RuntimeVertex target : adjacentNodes) {
      if (getShortestDistance(target) > getShortestDistance(node)
        + getDistance(node, target)) {
        distance.put(target, getShortestDistance(node)
          + getDistance(node, target));
        predecessors.put(target, node);
        unSettledNodes.add(target);
      }
    }
  }

  private int getDistance(RuntimeVertex node, RuntimeVertex target) {
    for (RuntimeEdge edge : edges) {
      if (Objects.equals(edge.getSourceVertex(), node)
        && Objects.equals(edge.getTargetVertex(), target)) {
        return 1;
      }
    }
    throw new RuntimeException("Should not happen");
  }

  private List<RuntimeVertex> getNeighbors(RuntimeVertex node) {
    List<RuntimeVertex> neighbors = new ArrayList<RuntimeVertex>();
    for (RuntimeEdge edge : edges) {
      if (Objects.equals(edge.getSourceVertex(), node) && !isSettled(edge.getTargetVertex())) {
        neighbors.add(edge.getTargetVertex());
      }
    }
    return neighbors;
  }

  private RuntimeVertex getMinimum(Set<RuntimeVertex> vertexes) {
    RuntimeVertex minimum = null;
    for (RuntimeVertex vertex : vertexes) {
      if (minimum == null) {
        minimum = vertex;
      } else {
        if (getShortestDistance(vertex) < getShortestDistance(minimum)) {
          minimum = vertex;
        }
      }
    }
    return minimum;
  }

  private boolean isSettled(RuntimeVertex vertex) {
    requireNonNull(vertex);
    return settledNodes.contains(vertex);
  }

  private int getShortestDistance(RuntimeVertex destination) {
    Integer d = distance.get(destination);
    if (d == null) {
      return Integer.MAX_VALUE;
    } else {
      return d;
    }
  }

  /*
   * This method returns the path from the source to the selected target and
   * NULL if no path exists
   */
  public Path<Element> getPath(RuntimeVertex target) {
    LinkedList<RuntimeVertex> path = new LinkedList<RuntimeVertex>();
    RuntimeVertex step = target;
    // check if a path exists
    if (predecessors.get(step) == null) {
      return null;
    }
    path.add(step);
    while (predecessors.get(step) != null) {
      step = predecessors.get(step);
      path.add(step);
    }
    // Put it into the correct order
    Collections.reverse(path);

    return withEdges(new Path<>(path));
  }

  private Path<Element> withEdges(Path<RuntimeVertex> vertices) {
    Path<Element> elements = new Path<>();
    if (!vertices.isEmpty()) {
      elements.add(vertices.getFirst());
    }

    List<RuntimeVertex> list = new ArrayList<>(vertices);
    a:
    for (int i = 1; i < vertices.size(); i++) {
      for (RuntimeEdge edge : this.edges) {
        RuntimeVertex source = list.get(i - 1);
        RuntimeVertex target = list.get(i);
        if (Objects.equals(edge.getSourceVertex(), source)
          && Objects.equals(edge.getTargetVertex(), target)) {
          elements.add(edge);
          elements.add(target);
          continue a;
        }
      }
      throw new IllegalStateException("No egde between");
    }
    return elements;
  }

}
