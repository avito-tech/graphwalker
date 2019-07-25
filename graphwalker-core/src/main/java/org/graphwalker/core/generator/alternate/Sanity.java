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
 * @author Ivan Bonkin
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
