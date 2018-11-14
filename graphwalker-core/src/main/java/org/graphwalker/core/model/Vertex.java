package org.graphwalker.core.model;

/*
 * #%L
 * GraphWalker Core
 * %%
 * Copyright (C) 2005 - 2014 GraphWalker
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

import org.graphwalker.core.common.Objects;

import java.util.ArrayList;
import java.util.List;

import static org.graphwalker.core.common.Objects.isNotNullOrEmpty;
import static org.graphwalker.core.common.Objects.unmodifiableList;

/**
 * <h1>Vertex</h1>
 * The  Vertex holds the information for a state in a model.
 * </p>
 * The vertex is the verification point for a test. It's here where the test asserts
 * that the system under test is in the expected state.
 * The vertex is uniquely identified by its id.
 * The source vertex is not mandatory, but in a model, there should be only one
 * such instance. Also, the target vertex is not mandatory, but again, in a model,
 * there should be only one such instance.
 *
 * @author Nils Olsson
 */
public class Vertex extends CachedBuilder<Vertex, Vertex.RuntimeVertex> {

  private String sharedState;
  private String groupName;
  private List<Action> setActions = new ArrayList<>();
  private CodeTag codeTag;

  /**
   * Gets the name of the shared state.
   *
   * @return The name as a string.
   * @see Vertex#setSharedState
   */
  public String getSharedState() {
    return sharedState;
  }

  /**
   * Sets the name of the shared state of this vertex.
   * If the vertex is to act as a shared state, the name of the shared state needs to be set to a non-empty
   * string.
   * </p>
   * The shared state is portal to other shared states in other models. It creates a 'virtual edge'
   * between to vertices sharing the same name in their shared state.
   * </p>
   * <img src="doc-files/Vertex.setSharedState.png">
   * </p>
   * In the 2 models above, GraphWalker will create virtual edges, the dotted arrows. These edges will
   * allow passages between the 2 models.
   *
   * @param sharedState The name of the shared state.
   * @return The vertex
   */
  public Vertex setSharedState(String sharedState) {
    this.sharedState = sharedState;
    invalidateCache();
    return this;
  }

  public Vertex setGroupName(String groupName) {
    this.groupName = groupName;
    invalidateCache();
    return this;
  }

  public List<Action> getSetActions() {
    return unmodifiableList(setActions);
  }

  public Vertex setSetActions(List<Action> setActions) {
    this.setActions = setActions;
    return this;
  }

  public Vertex addSetAction(Action setAction) {
    this.setActions.add(setAction);
    return this;
  }

  @Override
  public String toString() {
    return "Vertex(" + super.getName() + ")";
  }

  /**
   * Creates an immutable vertex from this vertex.
   *
   * @return An immutable vertex as a RuntimeVertex
   */
  @Override
  protected RuntimeVertex createCache() {
    return new RuntimeVertex(this);
  }

  public void setCodeTag(CodeTag codeTag) {
    this.codeTag = codeTag;
  }

  public CodeTag getCodeTag() {
    return codeTag;
  }

  /**
   * <h1>RuntimeVertex</h1>
   * Immutable class for Vertex
   * </p>
   * This class is used in models. It guarantees that that the internal states of
   * the instance will not change after it's construction.
   */
  public static class RuntimeVertex extends RuntimeBase {

    private final String sharedState;

    private final boolean indegrees;
    private final boolean outdegrees;

    private final String groupName;
    private final CodeTag codeTag;

    private RuntimeVertex(Vertex vertex) {
      super(vertex.getId(), vertex.getName(), vertex.getDescription(), vertex.getRequirements(), vertex.getProperties());
      this.sharedState = vertex.getSharedState();
      this.indegrees = vertex.hasIndegrees();
      this.outdegrees = vertex.hasOutdegrees();
      this.groupName = vertex.groupName;
      this.codeTag = vertex.getCodeTag();
    }

    public boolean hasIndegrees() {
      return indegrees;
    }

    public boolean hasOutdegrees() {
      return outdegrees;
    }

    public String getGroupName() {
      return groupName;
    }

    /**
     * Gets the name of the shared state.
     *
     * @return The name as a string.
     * @see Vertex#setSharedState
     */
    public String getSharedState() {
      return sharedState;
    }

    /**
     * Returns true if the vertex has a valid shared state.
     *
     * @return True if the vertex has a shared state.
     */
    public boolean hasSharedState() {
      return isNotNullOrEmpty(sharedState);
    }

    public CodeTag getCodeTag() {
      return codeTag;
    }

    /**
     * TODO Needs documentation
     */
    @Override
    public void accept(ElementVisitor visitor) {
      visitor.visit(this);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result
               + ((sharedState == null) ? 0 : sharedState.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object o) {
      if (!super.equals(o)) {
        return false;
      }
      RuntimeVertex that = (RuntimeVertex) o;
      return Objects.equals(sharedState, that.sharedState);
    }
  }
}
