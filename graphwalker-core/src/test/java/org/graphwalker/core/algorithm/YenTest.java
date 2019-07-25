package org.graphwalker.core.algorithm;

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

import org.graphwalker.core.machine.TestExecutionContext;
import org.graphwalker.core.model.*;
import org.junit.Test;

import java.util.List;

import static org.graphwalker.core.Models.findVertex;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class YenTest {

  private static final Vertex a = new Vertex().setName("a").setId("a");
  private static final Vertex b = new Vertex().setName("b").setId("b");
  private static final Vertex c = new Vertex().setName("c").setId("c");
  private static final Vertex d = new Vertex().setName("d").setId("d");
  private static final Vertex e = new Vertex().setName("e").setId("e");
  private static final Vertex f = new Vertex().setName("f").setId("f");
  private static final Vertex g = new Vertex().setName("g").setId("g");
  private static final Vertex z = new Vertex().setName("z").setId("z");

  private static final Edge ab = new Edge().setName("ab").setId("ab").setSourceVertex(a).setTargetVertex(b);
  private static final Edge ag = new Edge().setName("ag").setId("ag").setSourceVertex(a).setTargetVertex(g);
  private static final Edge ae = new Edge().setName("ae").setId("ae").setSourceVertex(a).setTargetVertex(e);
  private static final Edge bc = new Edge().setName("bc").setId("bc").setSourceVertex(b).setTargetVertex(c);
  private static final Edge be = new Edge().setName("be").setId("be").setSourceVertex(b).setTargetVertex(e);
  private static final Edge bf = new Edge().setName("bf").setId("bf").setSourceVertex(b).setTargetVertex(f);
  private static final Edge cd = new Edge().setName("cd").setId("cd").setSourceVertex(c).setTargetVertex(d);
  private static final Edge df = new Edge().setName("df").setId("df").setSourceVertex(d).setTargetVertex(f);
  private static final Edge dz = new Edge().setName("dz").setId("dz").setSourceVertex(d).setTargetVertex(z);
  private static final Edge ef = new Edge().setName("ef").setId("ef").setSourceVertex(e).setTargetVertex(f);
  private static final Edge fz = new Edge().setName("fz").setId("fz").setSourceVertex(f).setTargetVertex(z);
  private static final Edge gz = new Edge().setName("gz").setId("gz").setSourceVertex(g).setTargetVertex(z);

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
   * Expected output:
   * <blockquote><pre>
   * [a->b->c->d->z]
   * [a->b->f->z]
   * [a->b->c->d->f->z]
   * [a->e->f->z]
   * [a->b->e->f->z]
   * [a->g->z]
   * </pre></blockquote>
   */
  @Test
  public void generateMoreThanExist() {
    Yen yen = new Yen(new TestExecutionContext().setModel(model));
    List<Path<Element>> paths = yen.ksp(findVertex(model, "a"), findVertex(model, "z"), 10);

    for (Path<Element> path : paths) {
      System.out.println(path);
    }

    assertThat(paths, hasSize(6));
  }
}
