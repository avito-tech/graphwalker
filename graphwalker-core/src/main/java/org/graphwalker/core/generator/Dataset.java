package org.graphwalker.core.generator;

import org.graphwalker.core.model.Action;

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
