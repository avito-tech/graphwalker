package org.graphwalker.core.generator.alternate;

import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Path;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Math.max;
import static java.lang.Math.pow;

/**
 * Selects routes with most different edges.
 *
 * @implSpec {@code sum((n-i)*weight) / max(0.5, foundDuplicates)}
 */
public class Sanity extends FitnessFunction {

  public Sanity(int size) {
    super(size);
  }

  @Override
  public double value(List<Path<Element>> sizedPaths) {
    Set<Edge.RuntimeEdge> edges = new HashSet<>();
    int duplicates = 0;

    for (Path<Element> path : sizedPaths) {
      for (Element element : path) {
        if (element instanceof Edge.RuntimeEdge) {
          if (!edges.add((Edge.RuntimeEdge) element)) {
            duplicates++;
          }
        }
      }
    }

    double pathsOrderingBonus = 0;
    for (int i = 0; i < size; i++) {
      pathsOrderingBonus += weight(sizedPaths.get(i)) * (size - i) / pow(distance(sizedPaths.get(i)), 0.5);
    }

    return pathsOrderingBonus / max(0.5, duplicates);
  }
}
