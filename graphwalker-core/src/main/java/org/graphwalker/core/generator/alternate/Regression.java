package org.graphwalker.core.generator.alternate;

import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Path;
import org.graphwalker.core.model.Vertex;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Selects k paths with max covered vertices.
 *
 * @implSpec {@code sum(vertices * (n-i) * weight)}
 */
public class Regression extends FitnessFunction {

  public Regression(int size) {
    super(size);
  }

  @Override
  public double value(List<Path<Element>> sizedPaths) {
    Set<Vertex.RuntimeVertex> vertices = new HashSet<>();

    for (Path<Element> path : sizedPaths) {
      for (Element element : path) {
        if (element instanceof Vertex.RuntimeVertex) {
          vertices.add((Vertex.RuntimeVertex) element);
        }
      }
    }

    double pathsOrderingBonus = 0;
    for (int i = 0; i < size; i++) {
      pathsOrderingBonus += weight(sizedPaths.get(i)) * (size - i);
    }

    return vertices.size() * pathsOrderingBonus;
  }
}
