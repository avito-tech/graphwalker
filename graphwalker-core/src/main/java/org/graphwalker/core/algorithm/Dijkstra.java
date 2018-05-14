package org.graphwalker.core.algorithm;

import org.graphwalker.core.common.Objects;
import org.graphwalker.core.model.Edge.RuntimeEdge;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Model;
import org.graphwalker.core.model.Path;
import org.graphwalker.core.model.Vertex.RuntimeVertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
