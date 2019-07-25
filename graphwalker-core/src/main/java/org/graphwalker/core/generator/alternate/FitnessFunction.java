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

import io.jenetics.EnumGene;
import io.jenetics.Genotype;
import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Path;

import java.util.List;
import java.util.function.Function;

public abstract class FitnessFunction implements Function<Genotype<EnumGene<Path<Element>>>, Double> {

  protected static double weight(Path<Element> path) {
    double weight = 1.;
    for (Element element : path) {
      if (element instanceof Edge.RuntimeEdge) {
        weight *= ((Edge.RuntimeEdge) element).getWeight();
      }
    }
    return weight;
  }

  protected static int edgeWithWeightLessThan(Path<Element> path, double weight) {
    int edges = 0;
    for (Element element : path) {
      if (element instanceof Edge.RuntimeEdge) {
        if (((Edge.RuntimeEdge) element).getWeight() < weight) {
          edges++;
        }
      }
    }
    return edges;
  }

  protected static int distance(Path<Element> path) {
    int distance = 0;
    for (Element element : path) {
      if (element instanceof Edge.RuntimeEdge) {
        distance++;
      }
    }
    return distance;
  }

  protected final int size;

  public abstract double value(List<Path<Element>> sizedPaths);

  protected FitnessFunction(int size) {
    this.size = size;
  }

  @Override
  public Double apply(Genotype<EnumGene<Path<Element>>> genotype) {
    return value(genotype.getChromosome().toSeq()
      .subSeq(0, size)
      .map(EnumGene::getAllele)
      .asList());
  }

}
