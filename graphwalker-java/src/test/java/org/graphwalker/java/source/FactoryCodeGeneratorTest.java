package org.graphwalker.java.source;

import com.github.javaparser.ast.body.MethodDeclaration;

import org.junit.Test;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class FactoryCodeGeneratorTest {

  @Test
  public void getMethodDeclarationTest() {
    MethodDeclaration methodDeclaration = FactoryCodeGenerator.getMethodDeclaration(Paths.get("My  model.graphml"));

    assertThat("File with whitespace should be parsable",
      methodDeclaration, is(not(equalTo(null))));
    assertThat("Whitespace should be replaced with lower dash",
      methodDeclaration.getName().getIdentifier(), is(equalTo("get_My__model")));
  }
}
