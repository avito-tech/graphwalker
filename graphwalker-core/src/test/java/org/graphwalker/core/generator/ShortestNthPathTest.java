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

import org.graphwalker.core.condition.ReachedVertex;
import org.graphwalker.core.generator.ShortestNthPath.HavingMostDifferentEdges;
import org.graphwalker.core.machine.Context;
import org.graphwalker.core.machine.TestExecutionContext;
import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Model;
import org.graphwalker.core.model.Vertex;
import org.junit.Test;

import static org.graphwalker.core.generator.ShortestNthPath.useTop;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Ivan Bonkin
 */
public class ShortestNthPathTest {

  private static final Vertex a = new Vertex().setName("a").setId("a");
  private static final Vertex b = new Vertex().setName("b").setId("b");
  private static final Vertex c = new Vertex().setName("c").setId("c");
  private static final Vertex d = new Vertex().setName("d").setId("d");
  private static final Vertex e = new Vertex().setName("e").setId("e");
  private static final Vertex f = new Vertex().setName("f").setId("f");
  private static final Vertex g = new Vertex().setName("g").setId("g");
  private static final Vertex z = new Vertex().setName("z").setId("z");

  private static final Edge ab = new Edge().setName("ab").setId("ab").setSourceVertex(a).setTargetVertex(b).setWeight(1.);
  private static final Edge ag = new Edge().setName("ag").setId("ag").setSourceVertex(a).setTargetVertex(g).setWeight(8.);
  private static final Edge ae = new Edge().setName("ae").setId("ae").setSourceVertex(a).setTargetVertex(e).setWeight(1.);
  private static final Edge bc = new Edge().setName("bc").setId("bc").setSourceVertex(b).setTargetVertex(c).setWeight(1.);
  private static final Edge be = new Edge().setName("be").setId("be").setSourceVertex(b).setTargetVertex(e).setWeight(1.);
  private static final Edge bf = new Edge().setName("bf").setId("bf").setSourceVertex(b).setTargetVertex(f).setWeight(2.);
  private static final Edge cd = new Edge().setName("cd").setId("cd").setSourceVertex(c).setTargetVertex(d).setWeight(1.);
  private static final Edge df = new Edge().setName("df").setId("df").setSourceVertex(d).setTargetVertex(f).setWeight(1.);
  private static final Edge dz = new Edge().setName("dz").setId("dz").setSourceVertex(d).setTargetVertex(z).setWeight(1.);
  private static final Edge ef = new Edge().setName("ef").setId("ef").setSourceVertex(e).setTargetVertex(f).setWeight(4.);
  private static final Edge fz = new Edge().setName("fz").setId("fz").setSourceVertex(f).setTargetVertex(z).setWeight(4.);
  private static final Edge gz = new Edge().setName("gz").setId("gz").setSourceVertex(g).setTargetVertex(z).setWeight(3.);

  private static final Model.RuntimeModel model = new Model()
    .addEdge(ab)
    .addEdge(ag)
    .addEdge(ae)
    .addEdge(bc)
    .addEdge(be)
    .addEdge(bf)
    .addEdge(cd)
    .addEdge(df)
    .addEdge(dz)
    .addEdge(ef)
    .addEdge(fz)
    .addEdge(gz)
    .build();

  /**
   * Expected path:
   * <blockquote>
   * [a->b->c->d->f->z]
   * </blockquote>
   */
  @Test
  public void topHavingMostDifferentEdges() {
    ShortestNthPath path = new ShortestNthPath(
      new ReachedVertex("z"),
      new HavingMostDifferentEdges(3), useTop(6), 0);
    Context context = new TestExecutionContext(model, path);
    context.setCurrentElement(a.build());
    Element nextElement = context.getPathGenerator().getNextStep().getCurrentElement();

    assertThat(nextElement, equalTo(ab.build()));
  }

  /**
   * Expected path:
   * <blockquote>
   * [a->e->f->z]
   * </blockquote>
   */
  @Test
  public void secondHavingMostDifferentEdges() {
    ShortestNthPath path = new ShortestNthPath(
      new ReachedVertex("z"),
      new HavingMostDifferentEdges(3), useTop(6), 1);
    Context context = new TestExecutionContext(model, path);
    context.setCurrentElement(a.build());
    Element nextElement = context.getPathGenerator().getNextStep().getCurrentElement();

    assertThat(nextElement, equalTo(ae.build()));
  }

  /**
   * Expected path:
   * <blockquote>
   * [a->g->z]
   * </blockquote>
   */
  @Test
  public void thirdHavingMostDifferentEdges() {
    ShortestNthPath path = new ShortestNthPath(
      new ReachedVertex("z"),
      new HavingMostDifferentEdges(3), useTop(6), 2);
    Context context = new TestExecutionContext(model, path);
    context.setCurrentElement(a.build());
    Element nextElement = context.getPathGenerator().getNextStep().getCurrentElement();

    assertThat(nextElement, equalTo(ag.build()));
  }
}
