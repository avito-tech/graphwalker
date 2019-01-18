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

import org.graphwalker.core.ModelBuilder;
import org.graphwalker.core.condition.ReachedVertex;
import org.graphwalker.core.machine.Context;
import org.graphwalker.core.machine.TestExecutionContext;
import org.graphwalker.core.model.Action;
import org.graphwalker.core.model.Guard;
import org.graphwalker.core.model.Model;
import org.junit.Test;

import static org.graphwalker.core.generator.ShortestPath.PathGenerationException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Ivan Bonkin
 */
public class ShortestPathTest {

  @Test
  public void testSimpleRoute() {
    Model model = new ModelBuilder()
      .connect("start", "v2")
      .connect("start", new Action("var closed = 0;"), "v5")
      .connect("v2", "v3")
      .connect("v3", "end")
      .connect("v5", new Guard("closed == 1"), "end")
      .connect("v5", "start")
      .getModel();

    Context context = new TestExecutionContext(model, new ShortestPath(new ReachedVertex("end")));
    context.setCurrentElement(context.getModel().getElementById("start"));
    assertThat(context.getPathGenerator().getNextStep().getCurrentElement().getId(), equalTo("start$v2"));
  }

  @Test
  public void testMediumRoute() {
    Model model = new ModelBuilder()
      .connect("v0", new Action("var e5 = false, e6 = false, e7 = false;"), "v01", "v02", "v03", "v04", "v05", "v06", "v07")
      .connect("v01", "v1")
      .connect("v02", "v2")
      .connect("v03", "v3")
      .connect("v04", "v4")
      .connect("v05", new Guard("e5 == true"), "v5")
      .connect("v06", new Guard("e6 == true"), "v6")
      .connect("v07", new Guard("e7 == true"), "v7")
      .connect("v1", new Action("e7 = true;"), "v2")
      .connect("v2", new Action("e6 = true;"), "v3")
      .connect("v3", new Action("e5 = true;"), "v4")
      .connect("v4", new Guard("e5 == true"), "v5")
      .connect("v5", new Guard("e6 == true"), "v6")
      .connect("v6", new Guard("e7 == true"), "v7")
      .getModel();

    Context context = new TestExecutionContext(model, new ShortestPath(new ReachedVertex("v7")));
    context.setCurrentElement(context.getModel().getElementById("v0"));
    assertThat(context.getPathGenerator().getNextStep().getCurrentElement().getId(), equalTo("v0$v01"));
  }

  @Test(expected = PathGenerationException.class)
  public void testErrorInGuard() {
    Model model = new ModelBuilder()
      .connect("start", new Action("var g1=false;"), "v0")
      .connect("v0", new Guard("g1==true"), "v10")
      .connect("v10", "v30")
      .connect("v0", "v11")
      .connect("v11", new Guard("g2==true"), "v20")
      .connect("v20", "v30")
      .getModel();

    Context context = new TestExecutionContext(model, new ShortestPath(new ReachedVertex("v30")));
    context.setCurrentElement(context.getModel().getElementById("start"));
    try {
      context.getPathGenerator().getNextStep();
    } catch (PathGenerationException e) {
      System.out.println(e.getMessage());
      assertThat(e.getMessage(), equalTo("Error finding ShortestPath by following attempts :\n" +
        "1. start→start$v0→v0⇛[v0$v10, GUARD_CONDITION: g1==true]⇨v10⇨v10$v30⇨v30\n" +
        "2. start→start$v0→v0→v0$v11→v11⇛[v11$v20, GUARD_ERROR: g2==true]⇨v20⇨v20$v30⇨v30"));
      throw e;
    }
  }

}
