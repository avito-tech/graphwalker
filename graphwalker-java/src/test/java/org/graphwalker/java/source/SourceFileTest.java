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

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SourceFileTest {

  @Test
  public void createSourceFileWithModelName() throws IOException {
    SourceFile sourceFile = new SourceFile(new ClassName("MyModel"), Paths.get("/model.json"));
    assertThat(sourceFile.getOutputPath(), is(Paths.get("/MyModel.java")));
    assertThat(sourceFile.getInputPath(), is(Paths.get("/model.json")));
    assertThat(sourceFile.getRelativePath(), is(Paths.get("model.json")));
    assertThat(sourceFile.getPackageName(), is(""));
    assertThat(sourceFile.getClassName(), is("MyModel"));
  }

  @Test
  public void createSourceFileWithOutputPath() throws IOException {
    SourceFile sourceFile = new SourceFile(Paths.get("/model.json"), Paths.get("/"), Paths.get("/output"));
    assertThat(sourceFile.getOutputPath(), is(Paths.get("/output/model.java")));
    assertThat(sourceFile.getInputPath(), is(Paths.get("/model.json")));
    assertThat(sourceFile.getRelativePath(), is(Paths.get("model.json")));
    assertThat(sourceFile.getPackageName(), is(""));
    assertThat(sourceFile.getClassName(), is("model"));
  }

  @Test
  public void createSourceFileWithModelNameAndOutputPath() throws IOException {
    SourceFile sourceFile = new SourceFile(new ClassName("MyModel"), Paths.get("/model.json"), Paths.get("/"), Paths.get("/output"));
    assertThat(sourceFile.getOutputPath(), is(Paths.get("/output/MyModel.java")));
    assertThat(sourceFile.getInputPath(), is(Paths.get("/model.json")));
    assertThat(sourceFile.getRelativePath(), is(Paths.get("model.json")));
    assertThat(sourceFile.getPackageName(), is(""));
    assertThat(sourceFile.getClassName(), is("MyModel"));
  }

  @Test
  public void createSourceFileWithNestedFolders() throws IOException {
    SourceFile sourceFile = new SourceFile(new ClassName("MyModel"), Paths.get("/company/path/model.json"), Paths.get("/"), Paths.get("/output"));
    assertThat(sourceFile.getOutputPath(), is(Paths.get("/output/company/path/MyModel.java")));
    assertThat(sourceFile.getInputPath(), is(Paths.get("/company/path/model.json")));
    assertThat(sourceFile.getRelativePath(), is(Paths.get("company/path/model.json")));
    assertThat(sourceFile.getPackageName(), is("company.path"));
    assertThat(sourceFile.getClassName(), is("MyModel"));
  }
}
