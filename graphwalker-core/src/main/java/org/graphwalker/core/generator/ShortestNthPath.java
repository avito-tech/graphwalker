package org.graphwalker.core.generator;

/*
 * #%L
 * GraphWalker Core
 * %%
 * Copyright (C) 2005 - 2018 GraphWalker
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

import org.graphwalker.core.algorithm.FloydWarshall;
import org.graphwalker.core.algorithm.Yen;
import org.graphwalker.core.condition.ReachedStopCondition;
import org.graphwalker.core.machine.Context;
import org.graphwalker.core.model.Edge.RuntimeEdge;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Path;
import org.graphwalker.core.model.Vertex.RuntimeVertex;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import io.jenetics.EnumGene;
import io.jenetics.Genotype;
import io.jenetics.Optimize;
import io.jenetics.PartiallyMatchedCrossover;
import io.jenetics.PermutationChromosome;
import io.jenetics.SwapMutator;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.stat.DoubleMomentStatistics;
import io.jenetics.util.ISeq;
import io.jenetics.util.RandomRegistry;

import static io.jenetics.engine.Limits.bySteadyFitness;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;

/**
 * Path with ability to pick n-th of possible to be generated.
 *
 * @author Ivan Bonkin
 */
public class ShortestNthPath extends PathGeneratorBase<ReachedStopCondition> {

  public static class UseTop {

    private final int value;

    private UseTop(int value) {
      this.value = value;
    }
  }

  public static UseTop useTop(int value) {
    return new UseTop(value);
  }

  public abstract static class FitnessFunction implements Function<Genotype<EnumGene<Path<Element>>>, Double> {

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

  /**
   * Selects k paths with max weight.
   *
   * @implSpec {@code sum(weight * (n-i))}
   */
  public static class Smoke extends FitnessFunction {

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

  /**
   * Selects k paths with all high probability edges and min distance.
   *
   * @implSpec {@code sum((hasNoEdgeWithWeightLessThan(threshold) ? 1 : 0) * (n-i) / distance)}
   */
  public static class HappyPath extends FitnessFunction {

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
        if (hasNoEdgeWithWeightLessThan(sizedPaths.get(i), happyThreshold)) {
          value += 1. * (size - i) / distance(sizedPaths.get(i)) * pow(weight(sizedPaths.get(i)), .25);
        }
      }

      return value;
    }
  }

  /**
   * Selects k paths with max covered vertices.
   *
   * @implSpec {@code sum(vertices * (n-i) * weight)}
   */
  public static class Regression extends FitnessFunction {

    public Regression(int size) {
      super(size);
    }

    @Override
    public double value(List<Path<Element>> sizedPaths) {
      Set<RuntimeVertex> vertices = new HashSet<>();

      for (Path<Element> path : sizedPaths) {
        for (Element element : path) {
          if (element instanceof RuntimeVertex) {
            vertices.add((RuntimeVertex) element);
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

  /**
   * Selects routes with most different edges.
   *
   * @implSpec {@code sum((n-i)*weight) / max(0.5, foundDuplicates)}
   */
  public static class Sanity extends FitnessFunction {

    public Sanity(int size) {
      super(size);
    }

    @Override
    public double value(List<Path<Element>> sizedPaths) {
      Set<RuntimeEdge> edges = new HashSet<>();
      int duplicates = 0;

      for (Path<Element> path : sizedPaths) {
        for (Element element : path) {
          if (element instanceof RuntimeEdge) {
            if (!edges.add((RuntimeEdge) element)) {
              duplicates++;
            }
          }
        }
      }

      double pathsOrderingBonus = 0;
      for (int i = 0; i < size; i++) {
        pathsOrderingBonus += weight(sizedPaths.get(i)) * (size - i);
      }

      return pathsOrderingBonus / max(0.5, duplicates);
    }
  }

  /**
   * Selects k paths with lowest distance.
   *
   * @implSpec {@code sum((n-i)/distance)}
   */
  public static class ShortestPaths extends FitnessFunction {

    public ShortestPaths(int size) {
      super(size);
    }

    @Override
    public double value(List<Path<Element>> sizedPaths) {

      double pathsOrderingBonus = 0;
      for (int i = 0; i < size; i++) {
        pathsOrderingBonus += 1. * (size - i) / distance(sizedPaths.get(i)) * pow(weight(sizedPaths.get(i)), .25);
      }

      return pathsOrderingBonus;
    }
  }

  private static double weight(Path<Element> path) {
    double weight = 1.;
    for (Element element : path) {
      if (element instanceof RuntimeEdge) {
        weight *= ((RuntimeEdge) element).getWeight();
      }
    }
    return weight;
  }

  private static boolean hasNoEdgeWithWeightLessThan(Path<Element> path, double weight) {
    for (Element element : path) {
      if (element instanceof RuntimeEdge) {
        if (((RuntimeEdge) element).getWeight() < weight) {
          return false;
        }
      }
    }
    return true;
  }

  private static int distance(Path<Element> path) {
    int distance = 0;
    for (Element element : path) {
      if (element instanceof RuntimeEdge) {
        distance++;
      }
    }
    return distance;
  }

  private final FitnessFunction ff;
  private final UseTop useTop;
  private final int index;
  private final Path<Element> cachedPath = new Path<>();

  public ShortestNthPath(ReachedStopCondition stopCondition, FitnessFunction ff, UseTop useTop, int index) {
    if (useTop.value < ff.size) {
      throw new IllegalArgumentException("Number of paths should not be less than batch size");
    }
    if (ff.size <= index) {
      throw new IllegalArgumentException("Index should be in bounds of batch size");
    }

    this.ff = ff;
    this.useTop = useTop;
    this.index = index;
    setStopCondition(stopCondition);
  }

  @Override
  public Context getNextStep() {
    Context context = super.getNextStep();

    if (cachedPath.isEmpty()) {
      if (context.getCurrentElement() instanceof RuntimeVertex) {
        List<Element> elements = context.filter(context.getModel().getElements(context.getCurrentElement()));
        if (elements.isEmpty()) {
          throw new NoPathFoundException(context.getCurrentElement());
        }
        Element target = null;
        int distance = MAX_VALUE;
        FloydWarshall floydWarshall = context.getAlgorithm(FloydWarshall.class);
        for (Element element : context.filter(getStopCondition().getTargetElements())) {
          int edgeDistance = floydWarshall.getShortestDistance(context.getCurrentElement(), element);
          if (edgeDistance < distance) {
            distance = edgeDistance;
            target = element;
          }
        }

        Yen yen = context.getAlgorithm(Yen.class);
        List<Path<Element>> paths = yen.ksp((RuntimeVertex) context.getCurrentElement(), (RuntimeVertex) target, useTop.value);

        if (paths.size() > 1) {
          sort(paths, ff);
        }

        cachedPath.addAll(paths.get(min(paths.size() - 1, index)));
        cachedPath.pollFirst();
      } else {
        return context.setCurrentElement(((RuntimeEdge) context.getCurrentElement()).getTargetVertex());
      }
    }

    return context.setCurrentElement(cachedPath.pollFirst());
  }

  @Override
  public boolean hasNextStep() {
    return !getStopCondition().isFulfilled();
  }

  private void sort(List<Path<Element>> paths, FitnessFunction ff) {

    RandomRegistry.setRandom(new Random(42));
    Engine<EnumGene<Path<Element>>, Double> engine = Engine
      .builder(
        ff,
        PermutationChromosome.of(ISeq.of(paths)))
      .optimize(Optimize.MAXIMUM)
      .populationSize(1000)
      .offspringFraction(0.9)
      .alterers(
        new SwapMutator<>(0.01),
        new PartiallyMatchedCrossover<>(0.3))
      .build();

    final EvolutionStatistics<Double, DoubleMomentStatistics> statistics =
      EvolutionStatistics.ofNumber();

    final EvolutionResult<EnumGene<Path<Element>>, Double> result = engine.stream()
      .limit(bySteadyFitness(100))
      .limit(2500)
      .peek(statistics)
      .collect(EvolutionResult.toBestEvolutionResult());

    paths.clear();
    paths.addAll(result.getBestPhenotype().getGenotype().getChromosome()
      .toSeq()
      .map(EnumGene::getAllele)
      .asList());
  }
}
