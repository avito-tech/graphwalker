package org.graphwalker.core.generator.alternate;

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
