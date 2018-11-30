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
import org.graphwalker.core.model.Path;
import org.graphwalker.core.model.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import static java.lang.Integer.MAX_VALUE;
import static javax.script.ScriptContext.ENGINE_SCOPE;
import static jdk.nashorn.api.scripting.NashornScriptEngine.NASHORN_GLOBAL;

/**
 * @author Ivan Bonkin
 */
public class ShortestPath extends PathGeneratorBase<ReachedStopCondition> {

  private static final Logger LOG = LoggerFactory.getLogger(ShortestPath.class);

  private Action[] actionsToBeExecutedBefore;

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

      next:
      while (iterator.hasNext()) {
        try {
          Path<Element> path = iterator.next();
          Action[] actions = actionsToBeExecutedBefore;

          for (Element element : path) {

            if (element instanceof Edge.RuntimeEdge) {
              if (actions != null) {
                try {
                  for (Action action : actions) {
                    context.getScriptEngine().eval(action.getScript());
                  }
                } catch (ScriptException e) {
                  LOG.error(e.getMessage());
                  throw new MachineException(context, e);
                } finally {
                  actions = null;
                }
              }
              if (!context.isAvailable((Edge.RuntimeEdge) element)) {
                iterator.remove();
                continue next;
              }
            }

            for (Action action : element.getActions()) {
              context.execute(action);
            }
          }

          path.pollFirst();

          LOG.info("Found shortest path: \"{}\"", path);

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

}
