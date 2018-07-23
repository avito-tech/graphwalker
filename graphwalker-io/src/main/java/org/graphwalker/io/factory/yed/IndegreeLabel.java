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

import org.graphwalker.core.model.Guard;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.graphwalker.core.model.Vertex.RuntimeVertex;

class IndegreeLabel {

  private final String name;

  private final String description;

  private final Guard guard;

  private final double weight;

  private final Set<RuntimeVertex> matchingOutdegrees;

  private IndegreeLabel(String name, String description, Guard guard, double weight, Set<RuntimeVertex> outdegreeSide) {
    this.name = name;
    this.description = description;
    this.guard = guard;
    this.weight = weight;
    this.matchingOutdegrees = outdegreeSide;
  }

  public IndegreeLabel(String name, String description, Guard guard, double weight, RuntimeVertex outdegreeSide) {
    this(name, description, guard, weight, new HashSet<>(singleton(outdegreeSide)));
  }

  public IndegreeLabel withName(String newName) {
    return new IndegreeLabel(newName, description, guard, weight, matchingOutdegrees);
  }

  public String getName() {
    return name;
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

  public Set<RuntimeVertex> getMatchingOutdegrees() {
    return matchingOutdegrees;
  }

  public static IndegreeLabel merge(IndegreeLabel v1, IndegreeLabel v2) {
    if (!Objects.equals(v1, v2)) {
      throw new IllegalArgumentException("IndegreeLabel must be equal to be merged");
    }
    v1.matchingOutdegrees.addAll(v2.matchingOutdegrees);
    return v1;
  }

  @Override
  public String toString() {
    return name
      + (isNotBlank(description) ? " /* " + description + " */" : "")
      + (guard != null ? " [ " + guard.getScript() + " ]" : "")
      + (weight < 1.0 ? " weight=" + weight : "");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IndegreeLabel label = (IndegreeLabel) o;
    return Double.compare(label.weight, weight) == 0 &&
      Objects.equals(name, label.name) &&
      Objects.equals(description, label.description) &&
      Objects.equals(guard, label.guard);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, guard, weight);
  }
}
