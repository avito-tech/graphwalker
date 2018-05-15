package org.graphwalker.core.generator.alternate;

import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Path;

import java.util.List;

import static java.lang.Math.pow;

/**
 * Selects k paths with lowest distance.
 *
 * @implSpec {@code sum((n-i)/distance)}
 */
public class ShortestPaths extends FitnessFunction {

  public ShortestPaths(int size) {
    super(size);
  }

  @Override
  public double value(List<Path<Element>> sizedPaths) {

    double pathsOrderingBonus = 0;
    for (int i = 0; i < size; i++) {
      pathsOrderingBonus += 1. * (size - i) / distance(sizedPaths.get(i)) * pow(weight(sizedPaths.get(i)), .5);
    }

    return pathsOrderingBonus;
  }
}
