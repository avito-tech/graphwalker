package org.graphwalker.core.example;

import org.graphwalker.core.condition.VertexCoverage;
import org.graphwalker.core.generator.RandomPath;
import org.graphwalker.core.machine.ExecutionContext;
import org.graphwalker.core.machine.ExecutionStatus;
import org.graphwalker.core.machine.Machine;
import org.graphwalker.core.machine.MachineException;
import org.graphwalker.core.machine.SimpleMachine;
import org.graphwalker.core.model.Action;
import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Guard;
import org.graphwalker.core.model.Model;
import org.graphwalker.core.model.Vertex;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Bonkin
 */
public class FactoryContextTest extends ExecutionContext {

  public static class GroupA {

    public void vertex1() {
    }

  }

  public static class GroupB {

    public void edge1() {
    }

    public void vertex2() {
    }

    public void vertex1() {
      throw new RuntimeException();
    }

  }

  public boolean isFalse() {
    return false;
  }

  public boolean isTrue() {
    return true;
  }

  public void myAction() {
  }

  @Override
  public Map<String, Object> groups() {
    HashMap<String, Object> groups = new HashMap<>();
    groups.put("A", new GroupA());
    groups.put("B", new GroupB());
    return groups;
  }

  @Test
  public void success() throws Exception {
    Vertex start = new Vertex();
    Model model = new Model().addEdge(new Edge()
      .setName("edge1")
      .setGuard(new Guard("isTrue()"))
      .setSourceVertex(start
        .setName("vertex1")
        .setGroupName("A"))
      .setTargetVertex(new Vertex()
        .setName("vertex2")
        .setGroupName("B"))
      .addAction(new Action("myAction();")));
    this.setModel(model.build());
    this.setPathGenerator(new RandomPath(new VertexCoverage(100)));
    setNextElement(start);
    Machine machine = new SimpleMachine(this);
    while (machine.hasNextStep()) {
      machine.getNextStep();
    }
  }

  @Test(expected = MachineException.class)
  public void failure() {
    Vertex start = new Vertex();
    Model model = new Model().addEdge(new Edge()
      .setName("edge1")
      .setGuard(new Guard("isFalse()"))
      .setSourceVertex(start
        .setName("vertex1")
        .setGroupName("A"))
      .setTargetVertex(new Vertex()
        .setName("vertex2")
        .setGroupName("B")));
    this.setModel(model.build());
    this.setPathGenerator(new RandomPath(new VertexCoverage(100)));
    setNextElement(start);
    Machine machine = new SimpleMachine(this);
    while (machine.hasNextStep()) {
      machine.getNextStep();
    }
  }

  @Test
  public void exception() throws Exception {
    Vertex start = new Vertex();
    Model model = new Model().addEdge(new Edge()
      .setName("edge1")
      .setGuard(new Guard("isTrue()"))
      .setSourceVertex(start
        .setName("vertex1")
        .setGroupName("B"))
      .setTargetVertex(new Vertex()
        .setName("vertex2")
        .setGroupName("B")));
    this.setModel(model.build());
    this.setPathGenerator(new RandomPath(new VertexCoverage(100)));
    setNextElement(start);
    Machine machine = new SimpleMachine(this);
    assertThat(getExecutionStatus(), is(ExecutionStatus.NOT_EXECUTED));
    try {
      while (machine.hasNextStep()) {
        machine.getNextStep();
        assertThat(getExecutionStatus(), is(ExecutionStatus.EXECUTING));
      }
    } catch (Throwable t) {
      assertTrue(MachineException.class.isAssignableFrom(t.getClass()));
      assertThat(getExecutionStatus(), is(ExecutionStatus.FAILED));
    }
  }
}
