package org.graphwalker.modelchecker;

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

import org.graphwalker.core.condition.EdgeCoverage;
import org.graphwalker.core.generator.RandomPath;
import org.graphwalker.core.machine.Context;
import org.graphwalker.core.model.Vertex;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by krikar on 2015-11-08.
 */
public class ContextChecker {

  private ContextChecker() {
  }

  /**
   * Checks the context for problems or any possible errors.
   * Any findings will be added to a list of strings.
   * <p/>
   * TODO: Implement a rule framework so that organisations and projects can create their own rule set (think model based code convention)
   *
   * @return A list of issues found in the context
   */
  static public List<String> hasIssues(Context context) {
    List<String> issues = new ArrayList<>();

    if (context.getModel() == null) {
      issues.add("No model found in context");
      return issues;
    }

    // Check the model
    issues.addAll(ModelChecker.hasIssues(context.getModel()));

    // Check for start element (or shared state)
    if (context.getNextElement() == null && !context.getModel().hasSharedStates()) {
      issues.add("The model has neither a start element or a defined shared state.");
      return issues;
    }

    // Check for a non-strongly connected graph and in combination with
    // random generator with full edge coverage.
    if (context.getPathGenerator() instanceof RandomPath) {
      if (context.getPathGenerator().getStopCondition() instanceof EdgeCoverage) {
        EdgeCoverage edgeCoverage = (EdgeCoverage) context.getPathGenerator().getStopCondition();
        if (edgeCoverage.getPercent() == 100) {
          int countNumOfCulDeSac = 0;
          for (Vertex.RuntimeVertex vertex : context.getModel().getVertices()) {
            if (context.getModel().getOutEdges(vertex).size() == 0) {
              countNumOfCulDeSac++;
            }
          }
          if (countNumOfCulDeSac > 1) {
            issues.add("The model has multiple cul-de-sacs, and is requested to run using a random " +
                       "path generator and 100% edge coverage. That will not work.");
          } else if (countNumOfCulDeSac == 1) {
            issues.add("The model has one cul-de-sacs, and is requested to run using a random " +
                       "path generator and 100% edge coverage. That might not work.");
          }
        }
      }
    }

    return issues;
  }
}
