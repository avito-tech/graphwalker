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
import org.graphwalker.core.machine.MachineException;
import org.graphwalker.core.model.Action;
import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Guard;
import org.graphwalker.core.model.Path;
import org.graphwalker.core.model.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.script.Bindings;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.script.ScriptContext.ENGINE_SCOPE;
import static jdk.nashorn.api.scripting.NashornScriptEngine.NASHORN_GLOBAL;
import static org.graphwalker.core.generator.ShortestPath.Statistics.Reason.ACTION_ERROR;
import static org.graphwalker.core.generator.ShortestPath.Statistics.Reason.GUARD_CONDITION;
import static org.graphwalker.core.generator.ShortestPath.Statistics.Reason.GUARD_ERROR;

/**
 * @author Ivan Bonkin
 */
public class ShortestPath extends PathGeneratorBase<ReachedStopCondition> {

  private static final Logger LOG = LoggerFactory.getLogger(ShortestPath.class);

  private Action[] actionsToBeExecutedBefore;
  private AtomicReference<Consumer<Long>> generationMillisStats = new AtomicReference<>(aLong -> {});

  public ShortestPath(ReachedStopCondition stopCondition) {
    setStopCondition(stopCondition);
    this.actionsToBeExecutedBefore = null;
  }

  /**
   * @param actionsToBeExecutedBefore actions to be executed <em>after</em> speculative execution of the first edge of the route
   */
  public ShortestPath(ReachedStopCondition stopCondition, Action ...actionsToBeExecutedBefore) {
    setStopCondition(stopCondition);
    this.actionsToBeExecutedBefore = actionsToBeExecutedBefore;
  }

  public ShortestPath(ReachedStopCondition stopCondition, Dataset dataset) {
    setStopCondition(stopCondition);
    this.actionsToBeExecutedBefore = dataset.selectPathActions();
  }

  public Consumer<Long> getGenerationMillisStats() {
    return generationMillisStats.get();
  }

  public void setGenerationMillisStats(Consumer<Long> generationMillisStats) {
    requireNonNull(generationMillisStats);
    this.generationMillisStats.set(generationMillisStats);
  }

  @Override
  public Context getNextStep() {
    Context context = super.getNextStep();

    if (context.getCurrentElement() instanceof Vertex.RuntimeVertex) {
      List<Element> elements = context.filter(context.getModel().getElements(context.getCurrentElement()));
      if (elements.isEmpty()) {
        throw new NoPathFoundException(context.getCurrentElement());
      }
      Element target = null;
      int distance = MAX_VALUE;

      long startTime = System.currentTimeMillis();

      FloydWarshall floydWarshall = context.getAlgorithm(FloydWarshall.class);
      for (Element element : context.filter(getStopCondition().getTargetElements())) {
        int edgeDistance = floydWarshall.getShortestDistance(context.getCurrentElement(), element);
        if (edgeDistance < distance) {
          distance = edgeDistance;
          target = element;
        }
      }

      Yen yen = context.getAlgorithm(Yen.class);
      Yen.NextShortestPath iterator = yen.nextShortestPath((Vertex.RuntimeVertex) context.getCurrentElement(), (Vertex.RuntimeVertex) target);

      Bindings engineBindings = (Bindings) context.getScriptEngine().getBindings(ENGINE_SCOPE).get(NASHORN_GLOBAL);
      Map<String, Object> globalCopy = new HashMap<>();
      for (Map.Entry<String, Object> e : engineBindings.entrySet()) {
        globalCopy.put(e.getKey(), e.getValue());
      }

      Statistics statistics = new Statistics();

      next:
      while (iterator.hasNext()) {
        try {
          Path<Element> path = iterator.next();
          Action[] actions = actionsToBeExecutedBefore;

          for (Element element : path) {

            if (element instanceof Edge.RuntimeEdge) {
              if (actions != null) {
                for (Action action : actions) {
                  try {
                    context.getScriptEngine().eval(action.getScript());
                  } catch (ScriptException e) {
                    LOG.error(e.getMessage());
                    statistics.actionError(path, element, action);
                    throw new PathGenerationException(statistics, context, e);
                  } finally {
                    actions = null;
                  }
                }
              }
              try {
                if (!context.isAvailable((Edge.RuntimeEdge) element)) {
                  statistics.guarded(path, element, ((Edge.RuntimeEdge) element).getGuard());
                  iterator.remove();
                  continue next;
                }
              } catch (MachineException e) {
                statistics.guardError(path, element, ((Edge.RuntimeEdge) element).getGuard());
                throw new PathGenerationException(statistics, context, e);
              }
            }

            for (Action action : element.getActions()) {
              context.execute(action);
            }
          }

          path.pollFirst();

          LOG.info("Found shortest path: \"{}\"", path);
          generationMillisStats.get().accept(System.currentTimeMillis() - startTime);

          Element nextElement = path.pollFirst();
          if (null != actionsToBeExecutedBefore
            && nextElement instanceof Edge.RuntimeEdge
            && null != ((Edge.RuntimeEdge) nextElement).getArguments()
            && !((Edge.RuntimeEdge) nextElement).getArguments().isEmpty()) {
            actionsToBeExecutedBefore = null;
          }
          return context.setCurrentElement(nextElement);

        } finally {
          Bindings localBindings = context.getScriptEngine().createBindings();
          localBindings.putAll(globalCopy);
          ScriptObjectMirror mirror = (ScriptObjectMirror)localBindings;
          context.getScriptEngine().getBindings(ENGINE_SCOPE).put(NASHORN_GLOBAL, mirror);
        }
      }
      throw new NoPathFoundException(context.getCurrentElement());
    } else {
      return context.setCurrentElement(((Edge.RuntimeEdge) context.getCurrentElement()).getTargetVertex());
    }
  }

  @Override
  public boolean hasNextStep() {
    return !getStopCondition().isFulfilled();
  }

  public static class Statistics {

    private final List<Attempt> attempts = new ArrayList<>();
    private int skipped = 0;

    public static class Attempt {
      private final String element;
      private final List<String> elementsBefore, elementsAfter;
      Reason reason;
      String script;

      public Attempt(Path<Element> path, Element element, Reason reason, String script) {
        this.element = element.getName();
        this.elementsBefore = path.before(element).stream().map(Element::getName).collect(toList());
        this.elementsAfter = path.after(element).stream().map(Element::getName).collect(toList());
        this.reason = reason;
        this.script = script;
      }

      public String getElement() {
        return element;
      }

      public List<String> getElementsBefore() {
        return elementsBefore;
      }

      public List<String> getElementsAfter() {
        return elementsAfter;
      }

      public Reason getReason() {
        return reason;
      }

      public String getScript() {
        return script;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attempt attempt = (Attempt) o;
        return element.equals(attempt.element) &&
          elementsBefore.equals(attempt.elementsBefore) &&
          reason == attempt.reason &&
          script.equals(attempt.script);
      }

      @Override
      public int hashCode() {
        return hash(element, elementsBefore, reason, script);
      }
    }

    public enum Reason {
      ACTION_ERROR, GUARD_ERROR, GUARD_CONDITION
    }

    public void actionError(Path<Element> path, Element element, Action action) {
      Attempt attempt = new Attempt(path, element, ACTION_ERROR, action.getScript());
      if (!attempts.contains(attempt)) {
        attempts.add(attempt);
      } else {
        skipped++;
      }
    }

    public void guardError(Path<Element> path, Element element, Guard guard) {
      Attempt attempt = new Attempt(path, element, GUARD_ERROR, guard.getScript());
      if (!attempts.contains(attempt)) {
        attempts.add(attempt);
      } else {
        skipped++;
      }
    }

    public void guarded(Path<Element> path, Element element, Guard guard) {
      Attempt attempt = new Attempt(path, element, GUARD_CONDITION, guard.getScript());
      if (!attempts.contains(attempt)) {
        attempts.add(attempt);
      } else {
        skipped++;
      }
    }

    public List<Attempt> getAttempts() {
      return attempts;
    }

    public int getSkipped() {
      return skipped;
    }
  }

  public static class PathGenerationException extends MachineException {

    private final Statistics statistics;

    public Statistics getStatistics() {
      return statistics;
    }

    @Override
    public String getMessage() {
      String msg = "Error finding ShortestPath by following attempts " + (statistics.skipped > 0 ? "(skipped " + statistics.skipped + " similar paths):\n" : ":\n");
      for (int i = 1; i <= statistics.attempts.size(); i++) {
        Statistics.Attempt a = statistics.attempts.get(i-1);
        msg += i + ". "
          + String.join("→", a.getElementsBefore())
          + "⇛[" + a.getElement() + ", " + a.reason + ": " + a.script + "]"
          + a.getElementsAfter().stream().collect(joining("⇨", "⇨", ""));
        if (i < statistics.attempts.size()) {
          msg += "\n";
        }
      }
      return msg;
    }

    @Override
    public String getLocalizedMessage() {
      return getMessage();
    }

    public PathGenerationException(Statistics statistics, Context context, Throwable e) {
      super(context, e);
      this.statistics = statistics;
    }
  }

}
