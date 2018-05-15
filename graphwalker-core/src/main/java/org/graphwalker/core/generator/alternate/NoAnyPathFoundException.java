package org.graphwalker.core.generator.alternate;

public class NoAnyPathFoundException extends RuntimeException {

  public NoAnyPathFoundException() {
    super("No paths were found");
  }
}
