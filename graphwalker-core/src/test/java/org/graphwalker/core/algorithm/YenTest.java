package org.graphwalker.core.algorithm;

import org.graphwalker.core.machine.TestExecutionContext;
import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Model;
import org.graphwalker.core.model.Path;
import org.graphwalker.core.model.Vertex;
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
