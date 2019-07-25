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

import org.graphwalker.core.machine.Context;
import org.graphwalker.core.model.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by krikar on 2015-11-08.
 */
public class ContextsChecker {

  /**
   * Checks the context for problems or any possible errors.
   * Any findings will be added to a list of strings.
   * <p/>
   * TODO: Implement a rule framework so that organisations and projects can create their own rule set (think model based code convention)
   *
   * @return A list of issues found in the context
   */
  static public List<String> hasIssues(List<Context> contexts) {
    List<String> issues = new ArrayList<>();

    // Check that individual contexts are valid
    for (Context context : contexts) {
      issues.addAll(ContextChecker.hasIssues(context));
    }

    // Check that ids are unique
    Set<String> ids = new HashSet<>();
    for (Context context : contexts) {
      if (!ids.add(context.getModel().getId())) {
        issues.add("Id of the model is not unique: " + context.getModel().getId());
      }
    }

    // Check that all internal ids are unique
    Set<Element> elements = new HashSet<>();
    for (Context context : contexts) {
      if (!elements.add(context.getModel())) {
        issues.add("Internal id of the model is not unique: " + context);
      }
      for (Element element : context.getModel().getElements()) {
        if (!elements.add(element)) {
          issues.add("Internal id of the element is not unique: " + element);
        }
      }
    }

    return issues;
  }
}
