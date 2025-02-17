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
import org.graphwalker.core.machine.Context;
import org.graphwalker.core.model.Edge.RuntimeEdge;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Path;
import org.graphwalker.core.model.Vertex.RuntimeVertex;

import java.util.*;

import static java.util.Objects.requireNonNull;
import static org.graphwalker.core.model.Model.RuntimeModel;

public class Yen implements Algorithm {

  private final RuntimeModel model;
  private final List<RuntimeEdge> modifiableEdges;

  public Yen(Context context) {
    this.modifiableEdges = new ArrayList<>(context.getModel().getEdges());
    this.model = context.getModel().withEdges(modifiableEdges);
  }

  private static List<RuntimeEdge> getEdges(Path<Element> path) {
    List<RuntimeEdge> edges = new ArrayList<>();
    for (Element element : path) {
      if (element instanceof RuntimeEdge) {
        edges.add((RuntimeEdge) element);
      }
    }
    return edges;
  }

  private static Path<Element> cloneTo(Path<Element> path, int edgePos) {
    List<RuntimeEdge> edgeList = getEdges(path);
    int l = edgeList.size();
    if (edgePos > l)
      edgePos = l;

    Path<Element> elements = new Path<>();

    int iterationNb = 1;
    for (RuntimeEdge edge : edgeList.subList(0, edgePos)) {
      if (iterationNb++ == 1) {
        elements.add(edge.getSourceVertex());
      }
      elements.add(edge);
      elements.add(edge.getTargetVertex());
    }

    return new Path<Element>(elements);
  }

  private static Path<Element> cloneTo(Path<Element> path) {
    return cloneTo(path, Integer.MAX_VALUE);
  }

  private static void concat(Path<Element> dest, Path<Element> src) {
    if (dest.isEmpty()) {
      dest.addAll(src);
    } else if (src.isEmpty()) {
      // Do nothing
    } else {
      if (dest.getLast().equals(src.getFirst())) {
        src.removeFirst();
      }
      dest.addAll(src);
    }
  }

  private List<RuntimeEdge> removeNode(RuntimeVertex vertex) {
    LinkedList<RuntimeEdge> removedEdges = new LinkedList<RuntimeEdge>();

    Iterator<RuntimeEdge> iterator = modifiableEdges.iterator();
    while (iterator.hasNext()) {
      RuntimeEdge runtimeEdge = iterator.next();
      if (Objects.equals(runtimeEdge.getTargetVertex(), vertex)
        || Objects.equals(runtimeEdge.getSourceVertex(), vertex)) {

        iterator.remove();
        removedEdges.add(runtimeEdge);
      }
    }

    return removedEdges;
  }

  public NextShortestPath nextShortestPath(RuntimeVertex sourceLabel, RuntimeVertex targetLabel) {
    return new NextShortestPath(sourceLabel, targetLabel);
  }

  public class NextShortestPath implements Iterator<Path<Element>> {

    private final RuntimeVertex targetLabel;
    private final LinkedList<Path<Element>> ksp;
    private final PriorityQueue<Path<Element>> candidates;
    private Path<Element> next;

    private NextShortestPath(RuntimeVertex sourceLabel, RuntimeVertex targetLabel) {
      requireNonNull(sourceLabel);
      requireNonNull(targetLabel);

      this.targetLabel = targetLabel;
      this.ksp = new LinkedList<>();
      this.candidates = new PriorityQueue<>(new Comparator<Path<Element>>() {
        @Override
        public int compare(Path<Element> elements, Path<Element> other) {
          int thisLength = 0, otherLength = 0;
          double thisWeight = 0, otherWeight = 0;
          for (Element element : elements) {
            if (element instanceof RuntimeEdge) {
              thisWeight += 1.0;
              thisLength += 1;
            }
          }
          for (Element element : other) {
            if (element instanceof RuntimeEdge) {
              otherWeight += 1.0;
              otherLength += 1;
            }
          }
          int lengthCompared = Integer.compare(thisLength, otherLength);
          if (lengthCompared != 0) {
            return lengthCompared;
          }
          return Double.compare(thisWeight, otherWeight);
        }
      });

      // Compute and add the shortest path
      Dijkstra dijkstra = new Dijkstra(model);
      dijkstra.execute(sourceLabel);
      Path<Element> kthPath = dijkstra.getPath(targetLabel);
      ksp.add(kthPath);
      next = kthPath;
    }

    @Override
    public boolean hasNext() {
      if (next != null) {
        return true;
      }
      // Get previous shortest path
      Path<Element> previousPath = ksp.getLast();
      // Iterate over all of the nodes in the (k-1)st shortest path except for the target node; for each node,
      // (up to) one new candidate path is generated by temporarily modifying the graph and then running
      // Dijkstra's algorithm to find the shortest path between the node and the target in the modified
      // graph
      int edgeNb = previousPath != null ? getEdges(previousPath).size() : 0;
      for (int edgePos = 0; edgePos < edgeNb; edgePos++) {
        // Initialize a container to store the modified (removed) modifiableEdges for this node/iteration
        LinkedList<RuntimeEdge> removedEdges = new LinkedList<>();

        // Spur node = currently visited node in the (k-1)st shortest path
        RuntimeVertex spurNode = getEdges(previousPath).get(edgePos).getSourceVertex();

        // Root path = prefix portion of the (k-1)st path up to the spur node
        Path<Element> rootPath = cloneTo(previousPath, edgePos);

        // Iterate over all of the (k-1) shortest paths
        for (Path<Element> p : ksp) {
          Path stub = cloneTo(p, edgePos);
          // Check to see if this path has the same prefix/root as the (k-1)st shortest path
          if (rootPath.equals(stub)) {
            // If so, eliminate the next edge in the path from the graph (later on, this forces the spur
            // node to connect the root path with an un-found suffix path)
            RuntimeEdge re = getEdges(p).get(edgePos);
            //noinspection StatementWithEmptyBody
            while (modifiableEdges.remove(re));
            removedEdges.add(re);
          }
        }

        // Temporarily remove all of the nodes in the root path, other than the spur node, from the graph
        for (RuntimeEdge rootPathEdge : getEdges(rootPath)) {
          RuntimeVertex rn = rootPathEdge.getSourceVertex();
          if (!rn.equals(spurNode)) {
            removedEdges.addAll(removeNode(rn));
          }
        }

        // Spur path = shortest path from spur node to target node in the reduced graph
        Dijkstra dij = new Dijkstra(model);
        dij.execute(spurNode);
        Path<Element> spurPath = dij.getPath(targetLabel);

        // If a new spur path was identified...
        if (spurPath != null && !spurPath.isEmpty()) {
          // Concatenate the root and spur paths to form the new candidate path
          Path<Element> totalPath = cloneTo(rootPath);
          concat(totalPath, spurPath);

          // If candidate path has not been generated previously, add it
          if (!candidates.contains(totalPath))
            candidates.add(totalPath);
        }

        // Restore all of the modifiable edges that were removed during this iteration
        modifiableEdges.addAll(removedEdges);
      }

      /* Identify the candidate path with the shortest cost */
      boolean isNewPath;
      Path<Element> kthPath;
      do {
        kthPath = candidates.poll();
        isNewPath = true;
        if (kthPath != null) {
          for (Path p : ksp) {
            // Check to see if this candidate path duplicates a previously found path
            if (p.equals(kthPath)) {
              isNewPath = false;
              break;
            }
          }
        }
      } while (!isNewPath);

      next = kthPath;
      if (next != null) {
        ksp.add(kthPath);
        return true;
      }
      return false;
    }

    @Override
    public Path<Element> next() {
      if (next == null) {
        if (hasNext()) {
          Path<Element> result = new Path<>(next);
          next = null;
          return result;
        }
        throw new NoSuchElementException();
      } else {
        Path<Element> result = new Path<>(next);
        next = null;
        return result;
      }
    }

    @Override
    public void remove() {
      if (next != null) {
        throw new IllegalStateException();
      }
    }
  }

  /**
   * Computes the K shortest paths in a graph from node s to node t using Yen's algorithm
   *
   * @param sourceLabel the starting node for all of the paths
   * @param targetLabel the ending node for all of the paths
   * @param K           the number of shortest paths to compute
   * @return a list of the K shortest paths from s to t, ordered from shortest to longest
   */
  public List<Path<Element>> ksp(RuntimeVertex sourceLabel, RuntimeVertex targetLabel, int K) {
    Iterator<Path<Element>> iterator = nextShortestPath(sourceLabel, targetLabel);
    List<Path<Element>> copy = new ArrayList<>();
    for (int k = 0; k < K && iterator.hasNext(); k++) {
      copy.add(iterator.next());
    }
    return copy;
  }
}
