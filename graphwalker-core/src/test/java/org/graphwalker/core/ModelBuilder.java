package org.graphwalker.core;

/*-
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

import org.graphwalker.core.model.Action;
import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Guard;
import org.graphwalker.core.model.Model;
import org.graphwalker.core.model.Vertex;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * @author Ivan Bonkin
 */
public class ModelBuilder {

  private final Model model;
  private final Map<String, Vertex> cachedVertices = new ConcurrentHashMap<>();

  public ModelBuilder() {
    this.model = new Model();
  }

  public ModelBuilder connect(String srcIdAndName, String... dstIdAndNames) {
    return connect(srcIdAndName, (Guard) null, (Action) null, dstIdAndNames);
  }

  public ModelBuilder connect(String srcIdAndName, Guard guard, String... dstIdAndNames) {
    return connect(srcIdAndName, guard, (Action) null, dstIdAndNames);
  }

  public ModelBuilder connect(String srcIdAndName, Action action, String... dstIdAndNames) {
    return connect(srcIdAndName, (Guard) null, action, dstIdAndNames);
  }

  public ModelBuilder connect(String srcIdAndName, Guard guard, Action action, String... dstIdAndNames) {
    Vertex src = cachedVertices.computeIfAbsent(srcIdAndName, s -> new Vertex().setName(srcIdAndName).setId(srcIdAndName));
    for (String dstIdAndName : dstIdAndNames) {
      Vertex dst = cachedVertices.computeIfAbsent(dstIdAndName, s -> new Vertex().setName(dstIdAndName).setId(dstIdAndName));
      String edgeIdAndName = srcIdAndName + "$" + dstIdAndName;
      model.addEdge(new Edge()
        .setName(edgeIdAndName)
        .setId(edgeIdAndName)
        .setSourceVertex(src)
        .setTargetVertex(dst)
        .setGuard(guard)
        .setActions(action != null ? singletonList(action) : emptyList())
      );
    }
    return this;
  }

  public Model getModel() {
    return model;
  }
}
