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

import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Vertex;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;

/**
 * Created by krikar on 2015-11-08.
 */
public class EdgeCheckerTest {

  @Test
  public void testDefault() {
    List<String> issues = EdgeChecker.hasIssues(new Edge().build());
    Assert.assertThat(issues.size(), is(1));
    Assert.assertThat(issues.get(0), is("Edge must have a target vertex."));

    issues = EdgeChecker.hasIssues(new Edge().setTargetVertex(new Vertex()).build());
    Assert.assertThat(issues.size(), is(0));
  }

  @Test
  public void testName() {
    Edge edge = new Edge().setTargetVertex(new Vertex());
    List<String> issues = EdgeChecker.hasIssues(edge.setName("").build());
    Assert.assertThat(issues.size(), is(0));

    issues = EdgeChecker.hasIssues(edge.setName("spaces in name").build());
    Assert.assertThat(issues.size(), is(1));
    Assert.assertThat(issues.get(0), is("Name of edge cannot have any white spaces."));
  }

  @Test
  public void testWeight() {
    Edge edge = new Edge().setTargetVertex(new Vertex());
    edge.setWeight(-1.);
    List<String> issues = EdgeChecker.hasIssues(edge.setName("").build());
    Assert.assertThat(issues.size(), is(1));
    Assert.assertThat(issues.get(0), is("The weight must be a value between 0 and 1."));

    edge.setWeight(.5);
    issues = EdgeChecker.hasIssues(edge.setName("").build());
    Assert.assertThat(issues.size(), is(0));
  }
}
