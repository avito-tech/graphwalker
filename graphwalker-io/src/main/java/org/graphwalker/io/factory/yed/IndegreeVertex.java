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

import org.graphwalker.core.model.CodeTag;
import org.graphwalker.core.model.Guard;
import org.graphwalker.core.model.Vertex;

class IndegreeVertex {

  private final Vertex vertex;

  private final String description;

  private final Guard guard;

  private final double weight;

  private final CodeTag codeTag;

  public IndegreeVertex(Vertex vertex, String description, Guard guard, double weight, CodeTag codeTag) {
    this.vertex = vertex;
    this.description = description;
    this.guard = guard;
    this.weight = weight;
    this.codeTag = codeTag;
  }

  public Vertex getVertex() {
    return vertex;
  }

  public String getDescription() {
    return description;
  }

  public Guard getGuard() {
    return guard;
  }

  public double getWeight() {
    return weight;
  }

  public CodeTag getCodeTag() {
    return codeTag;
  }
}
