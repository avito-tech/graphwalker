package org.graphwalker.java.source;

/*
 * #%L
 * GraphWalker Java
 * %%
 * Copyright (C) 2005 - 2014 GraphWalker
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

import org.graphwalker.core.model.Edge.RuntimeEdge;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Vertex.RuntimeVertex;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import japa.parser.ast.body.MethodDeclaration;

import static org.graphwalker.core.model.Model.RuntimeModel;

/**
 * @author Nils Olsson
 */
public final class ChangeContext {

  private final RuntimeModel model;
  private final SourceFile sourceFile;
  private final Set<String> methodNames;
  private final Set<MethodDeclaration> methodDeclarations = new HashSet<>();

  public ChangeContext(RuntimeModel model, SourceFile sourceFile) {
    this.model = model;
    this.sourceFile = sourceFile;
    methodNames = extractMethodNames(model);
  }

  public Set<String> getMethodNames() {
    return methodNames;
  }

  public RuntimeModel getModel() {
    return model;
  }

  public void addMethodDeclaration(MethodDeclaration methodDeclaration) {
    methodDeclarations.add(methodDeclaration);
  }

  public Set<MethodDeclaration> getMethodDeclarations() {
    return methodDeclarations;
  }

  private Set<String> extractMethodNames(RuntimeModel model) {
    Set<String> methodNames = new HashSet<>();
    for (Element element : model.getElements()) {
      if (element instanceof RuntimeVertex) {
        if (element.hasName() && !"Start".equalsIgnoreCase(element.getName())) {
          if (isMatch(sourceFile, ((RuntimeVertex) element), model)) {
            methodNames.add(element.getName());
          }
        }
      } else if (element instanceof RuntimeEdge) {
        if (isMatch(sourceFile, ((RuntimeEdge) element).getTargetVertex(), model)) {
          methodNames.add(element.getName());
        }
      } else {
        throw new IllegalStateException("Class " + element.getClass().getSimpleName() + " was not recognized");
      }
    }
    return methodNames;
  }

  private static boolean isMatch(SourceFile sourceFile, RuntimeVertex vertex, RuntimeModel model) {
    if (vertex == null) {
      return false;
    }
    String groupName = vertex.getGroupName();
    if (groupName != null) {
      return Objects.equals(sourceFile.getClassName(), new ClassName(groupName))
        || Objects.equals(sourceFile.getPackageName(), "link");
    }
    return sourceFile.getClassName().equals(new ClassName(model.getName()));
  }

}
