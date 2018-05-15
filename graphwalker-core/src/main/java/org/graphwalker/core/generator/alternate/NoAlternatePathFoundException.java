package org.graphwalker.core.generator.alternate;

public class NoAlternatePathFoundException extends RuntimeException {

  public NoAlternatePathFoundException(int index) {
    super("No alternate paths were found with index \"" + index + "\"");
  }
}
