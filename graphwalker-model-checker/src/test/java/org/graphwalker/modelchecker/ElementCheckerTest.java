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
import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Requirement;
import org.graphwalker.core.model.Vertex;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;

/**
 * Created by krikar on 2015-11-08.
 */
public class ElementCheckerTest {

  @Test
  public void testDefault() {
    List<String> issues = ElementChecker.hasIssues(new Vertex().build());
    Assert.assertThat(issues.size(), is(0));
  }

  @Test
  public void testRequirement() {
    Vertex vertex = new Vertex().addRequirement(new Requirement(""));
    List<String> issues = ElementChecker.hasIssues(vertex.build());
    Assert.assertThat(issues.size(), is(1));
    Assert.assertThat(issues.get(0), is("Requirement cannot be an empty string"));
  }

  @Test
  public void testActions() {
    Edge edge = new Edge().addAction(new Action(""));
    List<String> issues = ElementChecker.hasIssues(edge.build());
    Assert.assertThat(issues.size(), is(1));
    Assert.assertThat(issues.get(0), is("Script statement cannot be an empty string"));
  }
}
