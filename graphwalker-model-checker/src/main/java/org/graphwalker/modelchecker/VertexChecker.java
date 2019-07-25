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

import com.google.common.base.CharMatcher;
import org.graphwalker.core.model.Vertex;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by krikar on 2015-11-08.
 */
public class VertexChecker {

  private VertexChecker() {
  }

  /**
   * Checks the vertex for problems or any possible errors.
   * Any findings will be added to a list of strings.
   * <p/>
   * TODO: Implement a rule framework so that organisations and projects can create their own rule set (think model based code convention)
   *
   * @return A list of issues found in the vertex
   */
  static public List<String> hasIssues(Vertex.RuntimeVertex vertex) {
    List<String> issues = new ArrayList<>(ElementChecker.hasIssues(vertex));

    if (vertex.getName() == null) {
      issues.add("Name of vertex cannot be null");
    } else {
      if (vertex.getName().isEmpty()) {
        issues.add("Name of vertex cannot be an empty string");
      }
      if (CharMatcher.WHITESPACE.matchesAnyOf(vertex.getName())) {
        issues.add("Name of vertex cannot have any white spaces.");
      }
    }
    return issues;
  }
}
