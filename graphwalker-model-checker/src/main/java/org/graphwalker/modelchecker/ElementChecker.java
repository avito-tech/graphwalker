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

import org.graphwalker.core.model.Action;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Requirement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by krikar on 2015-11-08.
 */
public class ElementChecker {

  private ElementChecker() {
  }

  /**
   * Checks for problems or any possible errors.
   * Any findings will be added to a list of strings.
   * <p/>
   * TODO: Implement a rule framework so that organisations and projects can create their own rule set (think model based code convention)
   *
   * @return A list of issues found
   */
  static public List<String> hasIssues(Element element) {
    List<String> issues = new ArrayList<>();

    if (element.getId() == null) {
      issues.add("Id cannot be null");
    }
    if (element.hasRequirements()) {
      for (Requirement requirement : element.getRequirements()) {
        if (requirement.getKey().isEmpty()) {
          issues.add("Requirement cannot be an empty string");
        }
      }
    }
    if (element.hasActions()) {
      for (Action action : element.getActions()) {
        if (action.getScript().isEmpty()) {
          issues.add("Script statement cannot be an empty string");
        }
      }
    }

    return issues;
  }
}
