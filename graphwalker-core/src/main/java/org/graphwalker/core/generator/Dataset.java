package org.graphwalker.core.generator;

/*
 * #%L
 * GraphWalker Core
 * %%
 * Original work Copyright (c) 2005 - 2018 GraphWalker
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

import org.graphwalker.core.model.Action;

/**
 * @author Ivan Bonkin
 */
public class Dataset {

  private final String name;
  private final int id, size;

  /**
   * @param name dataset parameter name
   * @param id in range [0; datasetSize-1]
   * @param size number of possible ways in dataset
   */
  public Dataset(String name, int id, int size) {
    this.name = name;
    this.id = id;
    this.size = size;
  }

  public String getName() {
    return name;
  }

  public int getId() {
    return id;
  }

  public int getSize() {
    return size;
  }

  public Action[] selectPathActions() {
    Action[] actions = new Action[size];
    for (int i = 0; i < size; i++) {
      actions[i] = new Action("try { gw.ds." + name + "[" + i + "].$open = " + (i == id) + "; } catch(exception) {};");
    }
    return actions;
  }
}
