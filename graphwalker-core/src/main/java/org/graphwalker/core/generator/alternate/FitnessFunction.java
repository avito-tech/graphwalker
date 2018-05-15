package org.graphwalker.core.generator.alternate;

import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Path;

import java.util.List;
import java.util.function.Function;

import io.jenetics.EnumGene;
import io.jenetics.Genotype;

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

  protected static boolean hasNoEdgeWithWeightLessThan(Path<Element> path, double weight) {
    for (Element element : path) {
      if (element instanceof Edge.RuntimeEdge) {
        if (((Edge.RuntimeEdge) element).getWeight() < weight) {
          return false;
        }
      }
    }
    return true;
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
