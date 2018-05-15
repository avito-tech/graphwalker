package org.graphwalker.core.generator.alternate;

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
import org.graphwalker.core.generator.NoPathFoundException;
import org.graphwalker.core.generator.PathGeneratorBase;
import org.graphwalker.core.machine.Context;
import org.graphwalker.core.model.Action;
import org.graphwalker.core.model.Edge.RuntimeEdge;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Path;
import org.graphwalker.core.model.Vertex.RuntimeVertex;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.script.Bindings;

import io.jenetics.EnumGene;
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
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.api.scripting.ScriptUtils;

import static io.jenetics.engine.Limits.bySteadyFitness;
import static java.lang.Integer.MAX_VALUE;
import static javax.script.ScriptContext.ENGINE_SCOPE;
import static jdk.nashorn.api.scripting.NashornScriptEngine.NASHORN_GLOBAL;

/**
 * Path with ability to pick n-th of possible to be generated.
 *
 * @author Ivan Bonkin
 */
public class ShortestNthPath extends PathGeneratorBase<ReachedStopCondition> {

  public static UseTop useTop(int value) {
    return new UseTop(value);
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

        Iterator<Path<Element>> iterator = paths.iterator();

        Bindings engineBindings = (Bindings) context.getScriptEngine().getBindings(ENGINE_SCOPE).get(NASHORN_GLOBAL);
        Map<String, Object> globalCopy = new HashMap<>();
        for (Map.Entry<String, Object> e : engineBindings.entrySet()) {
          globalCopy.put(e.getKey(), e.getValue());
        }

        next:
        while (iterator.hasNext()) {
          try {
            Path<Element> path = iterator.next();

            for (Element element : path) {

              if (element instanceof RuntimeEdge) {
                if (!context.isAvailable((RuntimeEdge) element)) {
                  iterator.remove();
                  continue next;
                }
              }

              for (Action action : element.getActions()) {
                context.execute(action);
              }
            }

          } finally {
            Bindings localBindings = context.getScriptEngine().createBindings();
            localBindings.putAll(globalCopy);
            ScriptObjectMirror mirror = ScriptUtils.wrap(localBindings);
            context.getScriptEngine().getBindings(ENGINE_SCOPE).put(NASHORN_GLOBAL, mirror);
          }
        }

        if (paths.size() > 1) {
          sort(paths, ff);
        } else if (paths.isEmpty()) {
          throw new NoAnyPathFoundException();
        }

        try {
          cachedPath.addAll(paths.get(index));
        } catch (IndexOutOfBoundsException ex) {
          throw new NoAlternatePathFoundException(index);
        }

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
      .limit(bySteadyFitness(250))
      .limit(5000)
      .peek(statistics)
      .collect(EvolutionResult.toBestEvolutionResult());

    paths.clear();
    paths.addAll(result.getBestPhenotype().getGenotype().getChromosome()
      .toSeq()
      .map(EnumGene::getAllele)
      .asList());
  }
}
