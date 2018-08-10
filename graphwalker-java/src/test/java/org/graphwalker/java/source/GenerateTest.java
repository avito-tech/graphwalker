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

import org.graphwalker.io.common.ResourceUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.graphwalker.java.utils.OccurrencesOfString.occurrencesOfString;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author Nils Olsson
 */
public class GenerateTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void generate() throws IOException {
    List<String> sources = new CodeGenerator().generate("/org/graphwalker/java/annotation/MyModel.graphml");
    Assert.assertThat(sources.size(), is(1));
    Assert.assertThat(sources.get(0), containsString("package org.graphwalker.java.annotation;"));
    Assert.assertThat(sources.get(0), containsString("edge12"));
    Assert.assertThat(sources.get(0), containsString("vertex2"));
    Assert.assertThat(sources.get(0), not(containsString("SHARED")));
  }

  @Test
  public void generatePathWithSpace() throws IOException {
    List<String> sources = new CodeGenerator().generate("/org/graphwalker/java/path with space/MyModel.graphml");
    Assert.assertThat(sources.size(), is(1));
    Assert.assertThat(sources.get(0), containsString("edge12"));
    Assert.assertThat(sources.get(0), containsString("vertex2"));
    Assert.assertThat(sources.get(0), not(containsString("SHARED")));
    Assert.assertThat(sources.get(0), containsString("package org.graphwalker.java.path_with_space;"));
  }

  @Test
  public void generateMultiModelFile() throws IOException {
    List<String> sources = new CodeGenerator().generate("/org/graphwalker/java/test/PetClinic.json");
    Assert.assertThat(sources.size(), is(5));

    Assert.assertThat(sources.get(0), containsString("public interface FindOwners {"));
    Assert.assertThat(sources.get(0), occurrencesOfString("@Vertex()", 3));
    Assert.assertThat(sources.get(0), occurrencesOfString("@Edge()", 3));
    Assert.assertThat(sources.get(0), stringContainsInOrder(Arrays.asList(
      "boolean v_FindOwners();",
      "boolean v_NewOwner();",
      "void e_AddOwner();",
      "void e_FindOwners();",
      "void e_Search();"
    )));

    Assert.assertThat(sources.get(1), containsString("public interface NewOwner {"));
    Assert.assertThat(sources.get(1), occurrencesOfString("@Vertex()", 3));
    Assert.assertThat(sources.get(1), occurrencesOfString("@Edge()", 2));
    Assert.assertThat(sources.get(1), stringContainsInOrder(Arrays.asList(
      "boolean v_IncorrectData();",
      "boolean v_NewOwner();",
      "boolean v_OwnerInformation();",
      "void e_CorrectData();",
      "void e_IncorrectData();"
    )));

    Assert.assertThat(sources.get(2), containsString("public interface OwnerInformation {"));
    Assert.assertThat(sources.get(2), occurrencesOfString("@Vertex()", 5));
    Assert.assertThat(sources.get(2), occurrencesOfString("@Edge()", 9));
    Assert.assertThat(sources.get(2), stringContainsInOrder(Arrays.asList(
      "boolean v_FindOwners();",
      "boolean v_NewPet();",
      "boolean v_NewVisit();",
      "boolean v_OwnerInformation();",
      "boolean v_Pet();",
      "void e_AddNewPet();",
      "void e_AddPetFailed();",
      "void e_AddPetSuccessfully();",
      "void e_AddVisit();",
      "void e_EditPet();",
      "void e_FindOwners();",
      "void e_UpdatePet();",
      "void e_VisitAddedFailed();",
      "void e_VisitAddedSuccessfully();"
    )));

    Assert.assertThat(sources.get(3), containsString("public interface PetClinic {"));
    Assert.assertThat(sources.get(3), occurrencesOfString("@Vertex()", 3));
    Assert.assertThat(sources.get(3), occurrencesOfString("@Edge()", 4));
    Assert.assertThat(sources.get(3), stringContainsInOrder(Arrays.asList(
      "boolean v_FindOwners();",
      "boolean v_HomePage();",
      "void e_FindOwners();",
      "void e_HomePage();",
      "void e_StartBrowser();",
      "void e_Veterinarians();"
    )));

    Assert.assertThat(sources.get(4), containsString("public interface Veterinarians {"));
    Assert.assertThat(sources.get(4), occurrencesOfString("@Vertex()", 2));
    Assert.assertThat(sources.get(4), occurrencesOfString("@Edge()", 1));
    Assert.assertThat(sources.get(4), stringContainsInOrder(Arrays.asList(
      "boolean v_SearchResult();",
      "boolean v_Veterinarians();",
      "void e_Search();"
    )));
  }

  static String readFile(Path path, Charset encoding)
    throws IOException {
    byte[] encoded = Files.readAllBytes(path);
    return new String(encoded, encoding);
  }

  @Test
  public void generateAndWriteFromGraphml() throws IOException {
    Path tmpFolder = testFolder.getRoot().toPath();

    new CodeGenerator().generate(
      ResourceUtils.getResourceAsFile("/CodeGenerator/graphml/").toPath(), tmpFolder
    );

    File cacheFile = new File(tmpFolder + "/cache.json");
    String jsonString = readFile(cacheFile.toPath(), UTF_8);

    // If test runs on windows, replace all double back slashes with single forward slash
    jsonString = jsonString.replaceAll("\\\\+", "/");

    jsonString = jsonString.replaceFirst(
      "\\{.*/CodeGenerator/graphml/org/graphwalker/java/graphml/MyModel\\.graphml",
      "{\"CodeGenerator/graphml/org/graphwalker/java/graphml/MyModel.graphml"
    );
    jsonString = jsonString.replaceFirst(
      "\"modified\":[0-9]+,",
      "\"modified\":123123123,"
    );
    JSONObject data = new JSONObject(jsonString);

    String expected = "{\"CodeGenerator/graphml/org/graphwalker/java/graphml/MyModel.graphml\":{modified:123123123,generated:true}}";
    JSONAssert.assertEquals(expected, data, false);

    File sourceFile = new File(tmpFolder + "/org/graphwalker/java/graphml/MyModel.java");
    Assert.assertThat(sourceFile.exists(), is(true));

    String content = readFile(sourceFile.toPath(), UTF_8);
    Assert.assertThat(content, equalTo(
      "// Generated by GraphWalker (http://www.graphwalker.org)\n" +
        "package org.graphwalker.java.graphml;\n" +
        "\n" +
        "import org.graphwalker.java.annotation.Model;\n" +
        "import org.graphwalker.java.annotation.Vertex;\n" +
        "import org.graphwalker.java.annotation.Edge;\n" +
        "\n" +
        "@Model(file = \"org/graphwalker/java/graphml/MyModel.graphml\")\n" +
        "public interface MyModel {\n" +
        "\n" +
        "    @Vertex(value = \"Comment 1\")\n" +
        "    boolean vertex1();\n" +
        "\n" +
        "    @Vertex(value = \"Comment \\\"2\\\"\\\\\")\n" +
        "    boolean vertex2();\n" +
        "\n" +
        "    @Edge(value = \"Comment\\n1-2\")\n" +
        "    void edge12();\n" +
        "}\n"
    ));
  }

  @Test
  public void generateAndWriteFromLinkedGraphml() throws IOException {
    Path tmpFolder = testFolder.getRoot().toPath();

    CodeGenerator.generate(
      ResourceUtils.getResourceAsFile("/CodeGenerator/linked_graphml/").toPath(), tmpFolder
    );

    File cacheFile = new File(tmpFolder + "/cache.json");
    String jsonString = readFile(cacheFile.toPath(), UTF_8);

    // If test runs on windows, replace all double back slashes with single forward slash
    jsonString = jsonString.replaceAll("\\\\+", "/");

    jsonString = jsonString.replaceFirst(
      "\\{.*/CodeGenerator/linked_graphml/org/graphwalker/java/graphml/MyModel Part(\\d)\\.graphml",
      "{\"CodeGenerator/linked_graphml/org/graphwalker/java/graphml/MyModel Part$1.graphml"
    );
    jsonString = jsonString.replaceFirst(
      "\"modified\":[0-9]+,",
      "\"modified\":123123123,"
    );
    JSONObject data = new JSONObject(jsonString);

    String expected = "{\"CodeGenerator/linked_graphml/org/graphwalker/java/graphml/MyModel Part1.graphml\":{modified:123123123,generated:true}}";
    JSONAssert.assertEquals(expected, data, false);

    File sourceFile = new File(tmpFolder + "/org/graphwalker/java/graphml/MyModel_Part1.java");
    Assert.assertThat(sourceFile.exists(), is(true));

    String content = readFile(sourceFile.toPath(), UTF_8);
    Assert.assertThat(content, equalTo(
      "// Generated by GraphWalker (http://www.graphwalker.org)\n" +
        "package org.graphwalker.java.graphml;\n" +
        "\n" +
        "import org.graphwalker.java.annotation.Model;\n" +
        "import org.graphwalker.java.annotation.Vertex;\n" +
        "import org.graphwalker.java.annotation.Edge;\n" +
        "\n" +
        "@Model(file = \"org/graphwalker/java/graphml/MyModel Part1.graphml\")\n" +
        "public interface MyModel_Part1 {\n" +
        "\n" +
        "    @Vertex(value = \"Comment 1\")\n" +
        "    boolean vertex1();\n" +
        "\n" +
        "    @Edge(value = \"comment\")\n" +
        "    void edge21();\n" +
        "\n" +
        "    @Edge(value = \"\")\n" +
        "    void startEdge();\n" +
        "}\n"
    ));

    sourceFile = new File(tmpFolder + "/org/graphwalker/java/graphml/MyModel_Part2.java");
    Assert.assertThat(sourceFile.exists(), is(true));

    content = readFile(sourceFile.toPath(), UTF_8);
    Assert.assertThat(content, equalTo(
      "// Generated by GraphWalker (http://www.graphwalker.org)\n" +
        "package org.graphwalker.java.graphml;\n" +
        "\n" +
        "import org.graphwalker.java.annotation.Model;\n" +
        "import org.graphwalker.java.annotation.Vertex;\n" +
        "import org.graphwalker.java.annotation.Edge;\n" +
        "\n" +
        "@Model(file = \"org/graphwalker/java/graphml/MyModel Part2.graphml\")\n" +
        "public interface MyModel_Part2 {\n" +
        "\n" +
        "    @Vertex(value = \"Comment \\\"2\\\"\\\\\")\n" +
        "    boolean vertex2();\n" +
        "\n" +
        "    @Vertex(value = \"Comment 3\")\n" +
        "    boolean vertex3();\n" +
        "\n" +
        "    @Edge(value = \"\")\n" +
        "    void edge12();\n" +
        "\n" +
        "    @Edge(value = \"\")\n" +
        "    void edge23();\n" +
        "\n" +
        "    @Edge(value = \"\")\n" +
        "    void edge32();\n" +
        "}\n"
    ));
  }

  @Test
  public void incorrectGraphml() throws IOException {
    Path tmpFolder = testFolder.getRoot().toPath();

    new CodeGenerator().generate(
      ResourceUtils.getResourceAsFile("/CodeGenerator/incorrect_graphml/").toPath(), tmpFolder
    );

    File cacheFile = new File(tmpFolder + "/cache.json");
    String jsonString = readFile(cacheFile.toPath(), UTF_8);

    // If test runs on windows, replace all double back slashes with single forward slash
    jsonString = jsonString.replaceAll("\\\\+", "/");

    jsonString = jsonString.replaceFirst(
      "\\{.*/CodeGenerator/incorrect_graphml/org/graphwalker/java/graphml/MyModel\\.graphml",
      "{\"CodeGenerator/incorrect_graphml/org/graphwalker/java/graphml/MyModel.graphml"
    );
    jsonString = jsonString.replaceFirst(
      "\"modified\":[0-9]+,",
      "\"modified\":123123123,"
    );
    JSONObject data = new JSONObject(jsonString);

    String expected = "{\"CodeGenerator/incorrect_graphml/org/graphwalker/java/graphml/MyModel.graphml\":{modified:123123123,generated:false}}";
    JSONAssert.assertEquals(expected, data, false);

    File sourceDir = new File(tmpFolder + "/org");
    Assert.assertThat(sourceDir.exists(), is(false));

    File sourceFile = new File(tmpFolder + "/org/graphwalker/java/graphml/MyModel.java");
    Assert.assertThat(sourceFile.exists(), is(false));
  }


  @Test
  public void generateDefaultMethods() throws IOException {
    Path tmpFolder = testFolder.getRoot().toPath();

    new CodeGenerator().generate(
      ResourceUtils.getResourceAsFile("/CodeGenerator/code_tag/").toPath(), tmpFolder
    );

    File sourceFile = new File(tmpFolder + "/org/graphwalker/java/graphml/MyModel.java");
    Assert.assertThat(sourceFile.exists(), is(true));

    String content = readFile(sourceFile.toPath(), UTF_8);
    Assert.assertThat(content, equalTo(
      "// Generated by GraphWalker (http://www.graphwalker.org)\n" +
        "package org.graphwalker.java.graphml;\n" +
        "\n" +
        "import org.graphwalker.java.annotation.Model;\n" +
        "import org.graphwalker.java.annotation.Vertex;\n" +
        "import org.graphwalker.java.annotation.Edge;\n" +
        "\n" +
        "@Model(file = \"org/graphwalker/java/graphml/MyModel.graphml\")\n" +
        "public interface MyModel {\n" +
        "\n" +
        "    @Vertex(value = \"@code isBrowserStarted(\\\"Firefox\\\")\\n            browser started\")\n" +
        "    default boolean v_BrowserStarted() {\n" +
        "        return isBrowserStarted(\"Firefox\");\n" +
        "    }\n" +
        "\n" +
        "    @Edge(value = \"@code runBrowser(\\\"Firefox\\\")\")\n" +
        "    default void e_init() {\n" +
        "        runBrowser(\"Firefox\");\n" +
        "    }\n" +
        "\n" +
        "    @Edge(value = \"@code get(\\\"https://www.avito.ru\\\");\\n            Home page navigation\")\n" +
        "    default void e_navigate() {\n" +
        "        get(\"https://www.avito.ru\");\n" +
        "    }\n" +
        "\n" +
        "    boolean isBrowserStarted(java.lang.String arg0);\n" +
        "\n" +
        "    void get(java.lang.String arg0);\n" +
        "\n" +
        "    void runBrowser(java.lang.String arg0);\n" +
        "\n" +
        "    @Vertex(value = \"page opened\")\n" +
        "    boolean v_OpenHome();\n" +
        "}\n"
    ));
  }
}
