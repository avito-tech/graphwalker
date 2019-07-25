package org.graphwalker.java.source;

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

import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class FactoryCodeGeneratorTest {

  @Test
  public void getMethodDeclarationTest() {
    SourceFile sourceFile = Mockito.mock(SourceFile.class);
    Mockito.doReturn(Paths.get("")).when(sourceFile).getBasePath();

    MethodDeclaration methodDeclaration = FactoryCodeGenerator.getMethodDeclaration(sourceFile, Paths.get("My  model.graphml"));

    assertThat("File with whitespace should be parsable",
      methodDeclaration, is(not(equalTo(null))));
    assertThat("Whitespace should be replaced with lower dash",
      methodDeclaration.getName().getIdentifier(), is(equalTo("get_My__model")));
  }
}
