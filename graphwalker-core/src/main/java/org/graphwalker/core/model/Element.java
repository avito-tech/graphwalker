package org.graphwalker.core.model;

/*
 * #%L
 * GraphWalker Core
 * %%
 * Original work Copyright (c) 2005 - 2014 GraphWalker
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

import java.util.List;
import java.util.Set;

/**
 * <h1>Element</h1>
 * The  Element is the interface for items in a model. Typical edges and vertices..
 *
 * @author Nils Olsson
 */
public interface Element {

  String getId();

  boolean hasId();

  /**
   * Gets the name of the element..
   *
   * @return The name as a string.
   * @see Vertex#setName
   * @see Edge#setName
   */
  String getName();

  /**
   * Returns true if the element has a name.
   * </p>
   * A valid name is anon empty string.
   *
   * @return True if the element has a valid name.
   */
  boolean hasName();

  /**
   * Commentary to the node.
   *
   * @return element's text description
   */
  String getDescription();

  Set<Requirement> getRequirements();

  boolean hasRequirements();

  List<Action> getActions();

  boolean hasActions();

  void accept(ElementVisitor visitor);

  Object getProperty(String key);

  boolean hasProperty(String key);
}
