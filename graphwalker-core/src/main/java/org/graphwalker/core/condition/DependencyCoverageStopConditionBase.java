package org.graphwalker.core.condition;

/*-
 * #%L
 * GraphWalker Core
 * %%
 * Original work Copyright (c) 2005 - 2017 GraphWalker
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

/**
 * @author Miroslav Janeski
 */
public abstract class DependencyCoverageStopConditionBase extends StopConditionBase {

  private final int dependency;

  protected DependencyCoverageStopConditionBase(int dependency) {
    super(String.valueOf(dependency));
    if (0 > dependency) {
      throw new StopConditionException("A dependency cannot be negative");
    }
    this.dependency = dependency;
  }

  public int getDependency() {
    return dependency;
  }

  protected double getDependencyAsDouble() {
    return (double) getDependency() / 100;
  }
}
