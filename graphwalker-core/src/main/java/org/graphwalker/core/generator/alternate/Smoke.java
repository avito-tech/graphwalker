package org.graphwalker.core.generator.alternate;

import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Path;

import java.util.List;

/**
 * Selects k paths with max weight.
 *
 * @implSpec {@code sum(weight * (n-i))}
 */
public class Smoke extends FitnessFunction {

  public Smoke(int size) {
    super(size);
  }

  @Override
  public double value(List<Path<Element>> sizedPaths) {
    double weight = 0.;
    for (int i = 0; i < size; i++) {
      weight += 1. * (size - i) * weight(sizedPaths.get(i));
    }

    return weight;
  }
}
