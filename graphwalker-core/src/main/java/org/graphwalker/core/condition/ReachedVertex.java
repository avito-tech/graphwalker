package org.graphwalker.core.condition;

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

import org.graphwalker.core.machine.Context;
import org.graphwalker.core.model.Element;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.graphwalker.core.common.Objects.*;
import static org.graphwalker.core.model.Vertex.RuntimeVertex;

/**
 * <h1>ReachedVertex</h1>
 * The ReachedVertex stop condition is fulfilled when the traversing of the model reaches the named vertex..
 * </p>
 *
 * @author Nils Olsson
 */
public class ReachedVertex extends ReachedStopConditionBase {

  public ReachedVertex(String target) {
    super(target);
  }

  public ReachedVertex(String groupName, String target) {
    super(isNotNullOrEmpty(groupName)
      ? groupName + "$" + target
      : target);
  }

  public Set<Element> getTargetElements() {
    Set<Element> elements = new HashSet<>();
    List<RuntimeVertex> vertices = getContext().getModel().findVertices(getValue());
    if (isNotNullOrEmpty(vertices)) {
      elements.addAll(vertices);
    }
    return elements;
  }

  @Override
  protected void validate(Context context) {
    super.validate(context);
    if (isNotNull(context) && isNull(context.getModel().findVertices(getValue()))) {
      throw new StopConditionException("Vertex [" + getValue() + "] not found");
    }
  }
}
