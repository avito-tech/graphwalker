package org.graphwalker.io.factory.yed;

/*-
 * #%L
 * GraphWalker Input/Output
 * %%
 * Copyright (C) 2005 - 2018 Avito
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

import org.graphwalker.core.model.Argument;
import org.graphwalker.core.model.Vertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YEdDataset {

  private final Vertex source, target;

  private final List<Argument.List> arguments;

  public YEdDataset(Vertex source, Vertex target) {
    this.source = source;
    this.target = target;
    this.arguments = new ArrayList<>();
  }

  public YEdDataset addArguments(Argument.List arguments) {
    this.arguments.add(arguments);
    return this;
  }

  public Vertex getSource() {
    return source;
  }

  public Vertex getTarget() {
    return target;
  }

  public List<Argument.List> getArgumentLists() {
    return Collections.unmodifiableList(arguments);
  }

  public boolean hasTarget(Vertex vertex) {
    return target.equals(vertex);
  }

  @Override
  public String toString() {
    return String.format("%s->%s{%s}", source, target, arguments);
  }
}
