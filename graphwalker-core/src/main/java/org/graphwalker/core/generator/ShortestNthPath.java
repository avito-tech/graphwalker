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

import java.util.ArrayList;
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
import io.jenetics.engine.Limits;
import io.jenetics.stat.DoubleMomentStatistics;
import io.jenetics.util.ISeq;
import io.jenetics.util.RandomRegistry;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.max;

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
   * Selects routes with most different edges.
   *
   * @implSpec {@code sum((n-i)*length) / max(0.5, foundDuplicates)}
   */
  public static class HavingMostDifferentEdges extends FitnessFunction {

    public HavingMostDifferentEdges(int size) {
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

      int pathsOrderingBonus = 0;
      for (int i = 0; i < size; i++) {
        pathsOrderingBonus += sizedPaths.get(i).size() * (size - i);
      }

      return pathsOrderingBonus / max(0.5, duplicates);
    }
  }

  private final FitnessFunction ff;
  private final UseTop useTop;
  private final int index;

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

    sort(paths, ff);

    return context.setCurrentElement(new ArrayList<>(paths.get(index)).get(1));
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
      .limit(Limits.bySteadyFitness(100))
      .limit(2500)
      .peek(statistics)
      .collect(EvolutionResult.toBestEvolutionResult());

    // System.out.println(result.getBestPhenotype());

    paths.clear();
    paths.addAll(result.getBestPhenotype().getGenotype().getChromosome()
      .toSeq()
      .map(EnumGene::getAllele)
      .asList());
  }
}
