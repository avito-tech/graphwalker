package org.graphwalker.core.algorithm;

import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Model;
import org.graphwalker.core.model.Path;
import org.graphwalker.core.model.Vertex;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class DijkstraTest {

  private static final Vertex v00 = new Vertex().setName("v00").setId("v00");
  private static final Vertex v01 = new Vertex().setName("v01").setId("v01");
  private static final Vertex v10 = new Vertex().setName("v10").setId("v10");
  private static final Vertex v20 = new Vertex().setName("v20").setId("v20");
  private static final Vertex v31 = new Vertex().setName("v31").setId("v31");

  private static final Edge e1 = new Edge().setName("e1").setId("e1").setSourceVertex(v00).setTargetVertex(v01);
  private static final Edge e2 = new Edge().setName("e2").setId("e2").setSourceVertex(v00).setTargetVertex(v10);
  private static final Edge e3 = new Edge().setName("e3").setId("e3").setSourceVertex(v10).setTargetVertex(v20);
  private static final Edge e4 = new Edge().setName("e4").setId("e4").setSourceVertex(v20).setTargetVertex(v31);
  private static final Edge e5 = new Edge().setName("e5").setId("e5").setSourceVertex(v01).setTargetVertex(v31);

  private static final Model model = new Model().addEdge(e1).addEdge(e2).addEdge(e3).addEdge(e4).addEdge(e5);

  @Test
  public void shortestDistance() {
    Dijkstra dijkstra = new Dijkstra(model.build());
    dijkstra.execute(v00.build());

    Path<Element> path = dijkstra.getPath(v31.build());
    System.out.println(path);
    assertThat(path, hasSize(3 + 2));
  }

  @Test
  public void noDistance() {
    Dijkstra dijkstra = new Dijkstra(model.build());
    dijkstra.execute(v00.build());
    assertThat(dijkstra.getPath(v00.build()), is(equalTo(null)));
  }
}
