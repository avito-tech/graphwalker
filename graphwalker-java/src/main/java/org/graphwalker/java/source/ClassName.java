package org.graphwalker.java.source;

/*
 * #%L
 * GraphWalker Java
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

import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Ivan Bonkin
 * @since 4.1.2
 */
public final class ClassName {

  private final String className;

  public ClassName(String className) {
    this.className = className.replace(' ', '_');
  }

  public ClassName(Path linkedFile) {
    this.className = FilenameUtils.removeExtension(linkedFile.getFileName().toString().replace(' ', '_'));
  }

  @Override
  public String toString() {
    return className;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other || other == null || getClass() != other.getClass()) return true;
    return Objects.equals(className, ((ClassName) other).className);
  }

  @Override
  public int hashCode() {
    return Objects.hash(className);
  }
}
