package org.graphwalker.core.generator.alternate;

import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Path;

import java.util.List;

import static java.lang.Math.pow;

/**
 * Selects k paths with all high probability edges and min distance.
 *
 * @implSpec {@code sum((hasNoEdgeWithWeightLessThan(threshold) ? 1 : 0) * (n-i) / distance)}
 */
public class HappyPath extends FitnessFunction {

  private final double happyThreshold;

  public HappyPath(int size, double happyThreshold) {
    super(size);
    if (happyThreshold > 1 || happyThreshold < 0) {
      throw new IllegalArgumentException("Threshold should be in range [0; 1]");
    }
    this.happyThreshold = happyThreshold;
  }

  @Override
  public double value(List<Path<Element>> sizedPaths) {
    double value = 0.;
    for (int i = 0; i < size; i++) {
      value += 1. * (size - i)
        / pow(4., edgeWithWeightLessThan(sizedPaths.get(i), happyThreshold))
        / distance(sizedPaths.get(i))
        * pow(weight(sizedPaths.get(i)), .1);
    }

    return value;
  }
}
