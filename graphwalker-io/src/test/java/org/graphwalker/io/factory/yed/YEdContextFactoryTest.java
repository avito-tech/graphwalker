package org.graphwalker.io.factory.yed;

/*
 * #%L
 * GraphWalker Input/Output
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

import org.apache.commons.io.IOUtils;
import org.graphwalker.core.condition.VertexCoverage;
import org.graphwalker.core.generator.RandomPath;
import org.graphwalker.core.machine.Context;
import org.graphwalker.core.machine.Machine;
import org.graphwalker.core.machine.SimpleMachine;
import org.graphwalker.core.model.*;
import org.graphwalker.core.model.Edge.RuntimeEdge;
import org.graphwalker.core.model.Model.RuntimeModel;
import org.graphwalker.core.model.Vertex.RuntimeVertex;
import org.graphwalker.io.TestExecutionContext;
import org.graphwalker.io.common.ResourceUtils;
import org.graphwalker.io.factory.ContextFactory;
import org.graphwalker.io.factory.ContextFactoryException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.graphwalker.core.model.VertexStyle.Configuration;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;


/**
 * @author Nils Olsson
 */
public class YEdContextFactoryTest {

  @Test(expected = ContextFactoryException.class)
  public void fileDoesNotExistsOnFileSystem() throws IOException {
    new YEdContextFactory().create(Paths.get("graphml/LKHDIODSOSUBD.graphml"));
  }

  @Test
  public void shared() throws IOException {
    List<Context> sharedAContexts = new YEdContextFactory().create(Paths.get("graphml/SharedA.graphml"));
    assertNotNull(sharedAContexts);
    assertThat(sharedAContexts.size(), is(1));

    assertThat(sharedAContexts.get(0).getModel().getVertices().size(), is(2));
    assertThat(sharedAContexts.get(0).getModel().getEdges().size(), is(6));

    List<Context> sharedBContexts = new YEdContextFactory().create(Paths.get("graphml/SharedB.graphml"));
    assertNotNull(sharedBContexts);
    assertThat(sharedBContexts.size(), is(1));

    assertThat(sharedBContexts.get(0).getModel().getVertices().size(), is(2));
    assertThat(sharedBContexts.get(0).getModel().getEdges().size(), is(1));
  }

  @Test
  public void login() throws IOException {
    List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/Login.graphml"));
    assertNotNull(contexts);
    assertThat(contexts.size(), is(1));

    assertThat(contexts.get(0).getModel().findVertices("v_ClientNotRunning").size(), is(1));

    Vertex.RuntimeVertex vertex = contexts.get(0).getModel().findVertices("v_ClientNotRunning").get(0);
    assertNotNull(vertex);
    assertThat((String) vertex.getProperty("description"), is("Start the client process"));

    Edge.RuntimeEdge edge = contexts.get(0).getModel().findEdges("e_ValidPremiumCredentials").get(0);
    assertNotNull(edge);
    assertThat((String) edge.getProperty("description"), is("Log in a s Premium user, using valid credentials"));
  }

  @Test
  public void uc01() throws IOException {
    List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/UC01.graphml"));
    assertNotNull(contexts);
    assertThat(contexts.size(), is(1));
    Context context = contexts.get(0);

    // Since the model id the Model.RuntimeModel,the Start vertex is removed from the graph.
    assertThat(context.getModel().getVertices().size(), is(7)); // one of the vertices is the start vertex and that shouldn't be a part of the model
    assertThat(context.getModel().getEdges().size(), is(12));
    assertThat(context.getModel().findEdges("e_init").size(), is(1));
    assertThat(context.getModel().findEdges("e_AddBookToCart").size(), is(1));
    assertThat(context.getModel().findEdges("e_ClickBook").size(), is(1));
    assertThat(context.getModel().findEdges("e_EnterBaseURL").size(), is(1));
    assertThat(context.getModel().findEdges("e_SearchBook").size(), is(4));
    assertThat(context.getModel().findEdges("e_ShoppingCart").size(), is(3));
    assertThat(context.getModel().findEdges("e_StartBrowser").size(), is(1));
    assertNull(context.getModel().findEdges(""));

    assertThat(context.getModel().findVertices("v_BaseURL").size(), is(1));
    assertThat(context.getModel().findVertices("v_BookInformation").size(), is(1));
    assertThat(context.getModel().findVertices("v_BrowserStarted").size(), is(1));
    assertThat(context.getModel().findVertices("v_BrowserStopped").size(), is(1));
    assertThat(context.getModel().findVertices("v_OtherBoughtBooks").size(), is(1));
    assertThat(context.getModel().findVertices("v_SearchResult").size(), is(1));
    assertThat(context.getModel().findVertices("v_ShoppingCart").size(), is(1));

    assertThat(context.getModel().findEdges("e_init").get(0).getTargetVertex().getName(), is("v_BrowserStopped"));

    assertThat(context.getModel().findEdges("e_StartBrowser").get(0).getSourceVertex().getName(), is("v_BrowserStopped"));
    assertThat(context.getModel().findEdges("e_StartBrowser").get(0).getTargetVertex().getName(), is("v_BrowserStarted"));
  }

  @Test
  public void efsmWithReqtags1() throws IOException {
    List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/reqtags/ShoppingCart.graphml"));
    assertNotNull(contexts);
    assertThat(contexts.size(), is(1));

    assertThat("Number of vertices", contexts.get(0).getModel().getVertices().size(), is(6));
    assertThat("Number of edges", contexts.get(0).getModel().getEdges().size(), is(11));
    // TODO Fix req
    //Assert.assertThat("Number of requirements", context.getRequirements().size(), is(5));
  }

  @Test
  public void efsmWithReqtags2() throws IOException {
    List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/reqtags/EFSM_with_REQTAGS.graphml"));
    assertNotNull(contexts);
    assertThat(contexts.size(), is(1));

    assertThat("Number of vertices", contexts.get(0).getModel().getVertices().size(), is(7));
    assertThat("Number of edges", contexts.get(0).getModel().getEdges().size(), is(19));
    // TODO Fix req
    //Assert.assertThat("Number of requirements", context.getRequirements().size(), is(6));
  }

  @Test
  public void guards() throws IOException {
    List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/Guards.graphml"));
    assertNotNull(contexts);
    assertThat(contexts.size(), is(1));

    assertThat("Number of vertices", contexts.get(0).getModel().getVertices().size(), is(2));
    assertThat("Number of edges", contexts.get(0).getModel().getEdges().size(), is(2));
  }

  @Test
  public void readInit() throws IOException {
    List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/init/init.graphml"));
    assertNotNull(contexts);
    assertThat(contexts.size(), is(1));
    assertThat(contexts.get(0).getModel().getActions().size(), is(1));
  }

  @Test
  public void handleFunction() throws IOException {
    List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/FunctionAsAction.graphml"));
    assertNotNull(contexts);
    assertThat(contexts.size(), is(1));
    RuntimeEdge edge = contexts.get(0).getModel().findEdges("init").get(0);
    assertThat(edge.getActions().size(), is(3));
    assertThat(edge.getActions().stream().map(Action::getScript).collect(joining()),
               is(" elements = [1,2,3]; value = 0; toString = function(){for(var i = 0;i<elements.length;i++){value+=elements[i]}return value};"));
  }

  @Test
  public void readLoginAndCrashModels() throws IOException {
    ContextFactory factory = new YEdContextFactory();
    List<Context> logins = factory.create(Paths.get("graphml/shared_state/Login.graphml"));
    List<Context> crashs = factory.create(Paths.get("graphml/shared_state/Crash.graphml"));
    for (RuntimeEdge edge : crashs.get(0).getModel().getEdges()) {
      assertNotNull(edge.getSourceVertex());
      assertNotNull(edge.getTargetVertex());
    }
  }

  @Test
  public void dryRun() throws IOException {
    List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/UC01.graphml"));
    assertNotNull(contexts);
    assertThat(contexts.size(), is(1));

    contexts.get(0).setPathGenerator(new RandomPath(new VertexCoverage(100)));
    Machine machine = new SimpleMachine(contexts);
    while (machine.hasNextStep()) {
      machine.getNextStep();
    }
  }

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void writeFile() throws IOException {
    Vertex v_Start = new Vertex().setName("Start").setId("n0");
    Vertex v_BrowserStarted = new Vertex().setName("v_BrowserStarted").setId("n1");
    Vertex v_BaseURL = new Vertex().setName("v_BaseURL").setId("n2");
    Vertex v_SearchResult = new Vertex().setName("v_SearchResult").setId("n3");
    Vertex v_BrowserStopped = new Vertex().setName("v_BrowserStopped").setId("n4");
    Vertex v_BookInformation = new Vertex().setName("v_BookInformation").setId("n5");
    Vertex v_OtherBoughtBooks = new Vertex().setName("v_OtherBoughtBooks").setId("n6");
    Vertex v_ShoppingCart = new Vertex().setName("v_ShoppingCart").setId("n7");

    Model model = new Model();
    model.addEdge(new Edge().setSourceVertex(v_Start).setTargetVertex(v_BrowserStopped).setName("e_init")).setId("e0");
    model.addEdge(new Edge().setSourceVertex(v_BrowserStarted).setTargetVertex(v_BaseURL).setName("e_EnterBaseURL")).setId("e1");
    model.addEdge(new Edge().setSourceVertex(v_BaseURL).setTargetVertex(v_SearchResult).setName("e_SearchBook")).setId("e2");
    model.addEdge(new Edge().setSourceVertex(v_BrowserStopped).setTargetVertex(v_BrowserStarted).setName("e_StartBrowser")).setId("e3");
    model.addEdge(new Edge().setSourceVertex(v_SearchResult).setTargetVertex(v_BookInformation).setName("e_ClickBook")).setId("e4");
    model.addEdge(new Edge().setSourceVertex(v_BookInformation).setTargetVertex(v_OtherBoughtBooks).setName("e_AddBookToCart")).setId("e5");
    model.addEdge(new Edge().setSourceVertex(v_OtherBoughtBooks).setTargetVertex(v_ShoppingCart).setName("e_ShoppingCart")).setId("e6");
    model.addEdge(new Edge().setSourceVertex(v_SearchResult).setTargetVertex(v_ShoppingCart).setName("e_ShoppingCart")).setId("e7");
    model.addEdge(new Edge().setSourceVertex(v_BookInformation).setTargetVertex(v_ShoppingCart).setName("e_ShoppingCart")).setId("e8");
    model.addEdge(new Edge().setSourceVertex(v_ShoppingCart).setTargetVertex(v_SearchResult).setName("e_SearchBook")).setId("e9");
    model.addEdge(new Edge().setSourceVertex(v_OtherBoughtBooks).setTargetVertex(v_SearchResult).setName("e_SearchBook")).setId("e10");
    model.addEdge(new Edge().setSourceVertex(v_BookInformation).setTargetVertex(v_SearchResult).setName("e_SearchBook")).setId("e11");

    Context writeContext = new TestExecutionContext().setModel(model.build());
    List<Context> writeContexts = new ArrayList<>();
    writeContexts.add(writeContext);

    // Write the graphml file
    Path tmpFolder = testFolder.getRoot().toPath();
    new YEdContextFactory().write(writeContexts, tmpFolder);

    // Read the graphml file
    List<Context> readCContexts = new YEdContextFactory().create(tmpFolder);

    // Compare
    assertThat(writeContext.getModel().getVertices().size() - 1, // The start vertex is removed automatically
               is(readCContexts.get(0).getModel().getVertices().size()));
    assertThat(writeContext.getModel().getEdges().size(),
               is(readCContexts.get(0).getModel().getEdges().size()));
  }

  @Test
  public void customProperties_01() throws IOException {
    /*
     * node: 		  y
     * edge: 		  n
     * default: 	y
     * overRide: 	y
     * desc:		node: y edge: y
     */
    Context context = getContextFromGraphml("graphml/customProp/Issue128_Graph01.graphml");
    RuntimeModel model = context.getModel();

    for (RuntimeVertex vertex : model.getVertices()) {
      assertGeometryPropertiesAreOk(vertex);
      assertTrue("Node description is missing.", vertex.hasProperty("description"));
      assertEquals("The description", vertex.getProperty("description").toString());
      assertTrue("CustomNodeProperty is not on Node.", vertex.hasProperty("CustomNodeProperty"));
      assertEquals("OverRideNodeValue", vertex.getProperty("CustomNodeProperty").toString());
    }

    for (RuntimeEdge edge : model.getEdges()) {
      assertTrue("Edge description is missing.", edge.hasProperty("description"));
      assertEquals("The edge description", edge.getProperty("description").toString());
      assertFalse("CustomNodeProperty is erroneously on Edge.", edge.hasProperty("CustomNodeProperty"));
    }
  }

  @Test
  public void customProperties_02() throws IOException {
    /*
     * node: 		  n
     * edge: 		  y
     * default: 	n
     * overRide: 	n
     * desc:		node: n edge: n
     */
    Context context = getContextFromGraphml("graphml/customProp/Issue128_Graph02.graphml");
    RuntimeModel model = context.getModel();

    for (RuntimeVertex vertex : model.getVertices()) {
      assertGeometryPropertiesAreOk(vertex);
      assertFalse("CustomEdgeProperty is erroneously on Node.", vertex.hasProperty("CustomEdgeProperty"));
      assertFalse("Description is erroneously on Node.", vertex.hasProperty("description"));
    }

    for (RuntimeEdge edge : model.getEdges()) {
      assertTrue("CustomEdgeProperty is missing from Edge.", edge.hasProperty("CustomEdgeProperty"));
      assertEquals("", edge.getProperty("CustomEdgeProperty").toString());
      assertFalse("Description is erroneously on Edge.", edge.hasProperty("description"));
    }
  }

  @Test
  public void customProperties_03() throws IOException {
    /*
     * node: 		  y
     * edge: 		  y
     * default: 	y
     * overRide: 	n
     * desc:		node: n edge: n
     */
    Context context = getContextFromGraphml("graphml/customProp/Issue128_Graph03.graphml");
    RuntimeModel model = context.getModel();

    for (RuntimeVertex vertex : model.getVertices()) {
      assertGeometryPropertiesAreOk(vertex);
      assertTrue("CustomNodeProperty is missing from Node.", vertex.hasProperty("CustomNodeProperty"));
      assertEquals("DefaultNodeValue", vertex.getProperty("CustomNodeProperty").toString());
      assertFalse("Description is erroneously on Node.", vertex.hasProperty("description"));
      assertFalse("CustomEdgeProperty is erroneously on Node.", vertex.hasProperty("CustomEdgeProperty"));
    }

    for (RuntimeEdge edge : model.getEdges()) {
      assertTrue("CustomEdgeProperty is missing from  Edge.", edge.hasProperty("CustomEdgeProperty"));
      assertEquals("DefaultEdgeValue", edge.getProperty("CustomEdgeProperty").toString());
      assertFalse("Description is erroneously on Edge.", edge.hasProperty("description"));
      assertFalse("CustomNodeProperty is erroneously on Edge.", edge.hasProperty("CustomNodeProperty"));
    }
  }

  @Test
  public void customProperties_04() throws IOException {
    /*
     * node: 		  y
     * edge: 		  y
     * default: 	n
     * overRide: 	y
     * desc:		node: n edge: n
     */
    Context context = getContextFromGraphml("graphml/customProp/Issue128_Graph04.graphml");
    RuntimeModel model = context.getModel();

    for (RuntimeVertex vertex : model.getVertices()) {
      assertGeometryPropertiesAreOk(vertex);
      assertTrue("CustomNodeProperty is missing from Node.", vertex.hasProperty("CustomNodeProperty"));
      assertEquals("OverRideNodeValue", vertex.getProperty("CustomNodeProperty").toString());
      assertFalse("Description is erroneously on Node.", vertex.hasProperty("description"));
      assertFalse("CustomEdgeProperty is erroneously on Node.", vertex.hasProperty("CustomEdgeProperty"));
    }

    for (RuntimeEdge edge : model.getEdges()) {
      assertTrue("CustomEdgeProperty is missing from  Edge.", edge.hasProperty("CustomEdgeProperty"));
      assertEquals("OverRideEdgeValue", edge.getProperty("CustomEdgeProperty").toString());
      assertFalse("Description is erroneously on Edge.", edge.hasProperty("description"));
      assertFalse("CustomNodeProperty is erroneously on Edge.", edge.hasProperty("CustomNodeProperty"));
    }
  }

  @Test
  public void customProperties_05() throws IOException {
    /*
     * node: 		  n
     * edge: 		  n
     * default: 	n
     * overRide: 	n
     * desc:		node: y edge: y
     */
    Context context = getContextFromGraphml("graphml/customProp/Issue128_Graph05.graphml");
    RuntimeModel model = context.getModel();

    for (RuntimeVertex vertex : model.getVertices()) {
      assertGeometryPropertiesAreOk(vertex);
      assertTrue("Node description is missing.", vertex.hasProperty("description"));
      assertEquals("Node Description", vertex.getProperty("description").toString());
      assertFalse("CustomNodeProperty is erroneously on Node.", vertex.hasProperty("CustomNodeProperty"));
      assertFalse("CustomEdgeProperty is erroneously on Node.", vertex.hasProperty("CustomEdgeProperty"));
    }

    for (RuntimeEdge edge : model.getEdges()) {
      assertTrue("Edge description is missing.", edge.hasProperty("description"));
      assertEquals("Edge Description", edge.getProperty("description").toString());
      assertFalse("CustomEdgeProperty is erroneously on  Edge.", edge.hasProperty("CustomEdgeProperty"));
      assertFalse("CustomNodeProperty is erroneously on Edge.", edge.hasProperty("CustomNodeProperty"));
    }
  }

  @Test
  public void customProperties_06() throws IOException {
    /*
     * node: 		  n
     * edge: 		  y
     * default: 	y
     * overRide: 	y
     * desc:		node: n edge: n
     */
    Context context = getContextFromGraphml("graphml/customProp/Issue128_Graph06.graphml");
    RuntimeModel model = context.getModel();

    for (RuntimeVertex vertex : model.getVertices()) {
      assertGeometryPropertiesAreOk(vertex);
      assertFalse("Description is erroneously on Node.", vertex.hasProperty("description"));
      assertFalse("CustomEdgeProperty is erroneously on Node.", vertex.hasProperty("CustomEdgeProperty"));
    }

    for (RuntimeEdge edge : model.getEdges()) {
      assertTrue("CustomEdgeProperty is missing from Edge.", edge.hasProperty("CustomEdgeProperty"));
      assertEquals("OverRideEdgeValue", edge.getProperty("CustomEdgeProperty").toString());
      assertFalse("Description is erroneously on Edge.", edge.hasProperty("description"));
    }
  }

  @Test
  public void customProperties_07() throws IOException {
    /*
     * Graph with a subgraph. Nodes and edges with 3 custom properties each.
     * Interestingly the node default values appear in yED but the <data key=xx> does not appear in the .graphml
     *
     * node: 		  y
     * edge: 		  y
     * default: 	y CNP 1 & CNP 3; CEP 1 & CEP 3
     * overRide: 	y CNP 2; CEP 2
     * desc:		  y
     * edge:      y
     */
    Context context = getContextFromGraphml("graphml/customProp/Issue128_Graph07.graphml");
    RuntimeModel model = context.getModel();

    List<RuntimeVertex> vertices = model.getVertices();
    assertThat(vertices.size(), is(3));

    for (RuntimeVertex vertex : vertices) {
      assertGeometryPropertiesAreOk(vertex);
      assertTrue("CustomNodeProperty 1 is missing from Node.", vertex.hasProperty("CNP 1"));
      assertEquals("Default Value 1", vertex.getProperty("CNP 1").toString());
      assertTrue("CustomNodeProperty 2 is missing from Node.", vertex.hasProperty("CNP 2"));
      assertEquals("OverRide 2", vertex.getProperty("CNP 2").toString());
      assertTrue("CustomNodeProperty 3 is missing from Node.", vertex.hasProperty("CNP 3"));
      assertEquals("OverRide 3", vertex.getProperty("CNP 3").toString());

      assertTrue("Description is missing from Node.", vertex.hasProperty("description"));
      assertEquals("Node Description", vertex.getProperty("description").toString());

      assertFalse("CustomEdgeProperty 1 is erroneously on Node.", vertex.hasProperty("CEP 1"));
      assertFalse("CustomEdgeProperty 2 is erroneously on Node.", vertex.hasProperty("CEP 2"));
      assertFalse("CustomEdgeProperty 3 is erroneously on Node.", vertex.hasProperty("CEP 3"));
    }

    List<RuntimeEdge> edges = model.getEdges();
    assertThat(edges.size(), is(2));

    for (RuntimeEdge edge : edges) {
      assertTrue("CustomEdgeProperty 1 is missing from Edge.", edge.hasProperty("CEP 1"));
      assertEquals("Default Value 1", edge.getProperty("CEP 1").toString());
      assertTrue("CustomEdgeProperty 2 is missing from Edge.", edge.hasProperty("CEP 2"));
      assertEquals("OverRide 2", edge.getProperty("CEP 2").toString());
      assertTrue("CustomEdgeProperty 3 is missing from Edge.", edge.hasProperty("CEP 3"));
      assertEquals("OverRide 3", edge.getProperty("CEP 3").toString());

      assertTrue("Description is missing from Edge.", edge.hasProperty("description"));
      assertEquals("Edge Description", edge.getProperty("description").toString());

      assertFalse("CustomNodeProperty 1 is erroneously on Node.", edge.hasProperty("CNP 1"));
      assertFalse("CustomNodeProperty 2 is erroneously on Node.", edge.hasProperty("CNP 2"));
      assertFalse("CustomNodeProperty 3 is erroneously on Node.", edge.hasProperty("CNP 3"));
    }
  }

  private Context getContextFromGraphml(String relPathToFile) throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(ResourceUtils.getResourceAsStream(relPathToFile), writer, StandardCharsets.UTF_8);
    String grapmlStr = writer.toString();

    List<Context> contexts = new YEdContextFactory().create(grapmlStr);
    assertNotNull(contexts);
    assertThat(contexts.size(), is(1));
    return contexts.get(0);
  }

  private void assertGeometryPropertiesAreOk(RuntimeVertex vertex) {
    assertTrue("X is missing.", vertex.hasProperty("x"));
    assertTrue(Double.valueOf(vertex.getProperty("x").toString()) > 0);

    assertTrue("Y is missing.", vertex.hasProperty("y"));
    assertTrue(Double.valueOf(vertex.getProperty("y").toString()) > 0);
  }

  @Test
  public void readWeightValue() throws IOException {
	  List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/weight/model.graphml"));
	  Context context = contexts.get(0);
	  RuntimeModel model = context.getModel();
	  RuntimeEdge edge = model.getEdges().get(0);
    assertEquals(1.0, edge.getWeight(), 0);
  }
  
  @Test
  public void readDepencyValue() throws IOException {
	  List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/dependency/model.graphml"));
	  Context context = contexts.get(0);
	  RuntimeModel model = context.getModel();
	  RuntimeEdge edge = model.getEdges().get(0);
	  assertEquals(100, edge.getDependency().intValue()); 
  }
  
  @Test
  public void readDepencyNoValue() throws IOException {
	  List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/dependency/modelWithoutDependency.graphml"));
	  Context context = contexts.get(0);
	  RuntimeModel model = context.getModel();
	  RuntimeEdge edge = model.getEdges().get(0);
	  assertEquals(0, edge.getDependency().intValue()); 
  }
  
  
  @Test
  public void readDepencyValueWithGuard() throws IOException {
	  List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/dependency/modelwithguard.graphml"));
	  Context context = contexts.get(0);
	  RuntimeModel model = context.getModel();
	  RuntimeEdge edge = model.getEdges().get(0);
	  assertEquals(75, edge.getDependency().intValue()); 
  }
  
  @Test
  public void readDepencyValueWithAction() throws IOException {
	  List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/dependency/modelwithaction.graphml"));
	  Context context = contexts.get(0);
	  RuntimeModel model = context.getModel();
	  RuntimeEdge edge = model.getEdges().get(0);
	  assertEquals(65, edge.getDependency().intValue()); 
  }
  
  @Test
  public void writeDependency() throws IOException {
    Vertex v_Start = new Vertex().setName("Start").setId("n0");
    Vertex v_BrowserStarted = new Vertex().setName("v_BrowserStarted").setId("n1");

    Model model = new Model();
    model.addEdge(new Edge().setSourceVertex(v_Start).setTargetVertex(v_BrowserStarted).setName("e_init").setDependency(75)).setId("e0");

    Context writeContext = new TestExecutionContext().setModel(model.build());
    List<Context> writeContexts = new ArrayList<>();
    writeContexts.add(writeContext);

    // Write the graphml file
    Path tmpFolder = testFolder.getRoot().toPath();
    new YEdContextFactory().write(writeContexts, tmpFolder);

    // Read the graphml file
    List<Context> readCContexts = new YEdContextFactory().create(tmpFolder);

    Context context = readCContexts.get(0);
    RuntimeEdge edge = context.getModel().getEdges().get(0);
    assertEquals(75, edge.getDependency().intValue());
    
  }
  
  @Test
  public void writeDependencyAndAction() throws IOException {
    Vertex v_Start = new Vertex().setName("Start").setId("n0");
    Vertex v_BrowserStarted = new Vertex().setName("v_BrowserStarted").setId("n1");


    Model model = new Model();
    List<Action> actions = new ArrayList<>();
    actions.add(new Action("validLogin=false;rememberMe=false;"));
    model.addEdge(new Edge().setSourceVertex(v_Start).setTargetVertex(v_BrowserStarted).setName("e_init").setActions(actions).setDependency(75)).setId("e0");

    Context writeContext = new TestExecutionContext().setModel(model.build());
    List<Context> writeContexts = new ArrayList<>();
    writeContexts.add(writeContext);

    // Write the graphml file
    Path tmpFolder = testFolder.getRoot().toPath();
    new YEdContextFactory().write(writeContexts, tmpFolder);

    // Read the graphml file
    List<Context> readCContexts = new YEdContextFactory().create(tmpFolder);

    Context context = readCContexts.get(0);
    RuntimeEdge edge = context.getModel().getEdges().get(0);
    assertEquals(75, edge.getDependency().intValue());
    
  }
  
  @Test
  public void writeDependencyAndGuard() throws IOException {
    Vertex v_Start = new Vertex().setName("Start").setId("n0");
    Vertex v_BrowserStarted = new Vertex().setName("v_BrowserStarted").setId("n1");


    Model model = new Model();

    model.addEdge(new Edge().setSourceVertex(v_Start).setTargetVertex(v_BrowserStarted).setName("e_init").setGuard(new Guard("!neverWentThere")).setDependency(75)).setId("e0");

    Context writeContext = new TestExecutionContext().setModel(model.build());
    List<Context> writeContexts = new ArrayList<>();
    writeContexts.add(writeContext);

    // Write the graphml file
    Path tmpFolder = testFolder.getRoot().toPath();
    new YEdContextFactory().write(writeContexts, tmpFolder);

    // Read the graphml file
    List<Context> readCContexts = new YEdContextFactory().create(tmpFolder);

    Context context = readCContexts.get(0);
    RuntimeEdge edge = context.getModel().getEdges().get(0);
    assertEquals(75, edge.getDependency().intValue());

  }

  @Test
  public void readGrouped() throws IOException {
    List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/grouped/modelGrouped.graphml"));
    Context context = contexts.get(0);
    RuntimeModel model = context.getModel();

    List<RuntimeVertex> vertices = model.getVertices();

    assertThat(vertices, hasItem(hasProperty("groupName", equalTo("rossiya"))));
    assertThat(vertices, hasItem(hasProperty("groupName", equalTo("profile"))));
  }

  @Test
  public void writeGroup() throws IOException {
    Vertex v_Start = new Vertex().setName("Start").setId("n1::n0").setGroupName("rossiya");
    Vertex v_Rossiya = new Vertex().setName("v__rossiya").setId("n1::n1").setGroupName("rossiya");
    Vertex v_Profile = new Vertex().setName("v__profile").setId("n0::n0").setGroupName("profile");
    Vertex v_ProfileItemsOld = new Vertex().setName("v__profile__items__old").setId("n0::n1").setGroupName("profile");

    Model model = new Model();
    model.addEdge(new Edge().setSourceVertex(v_Start).setTargetVertex(v_Rossiya).setName("e_Start")).setId("n1::e1");
    model.addEdge(new Edge().setSourceVertex(v_Rossiya).setTargetVertex(v_Rossiya).setName("e_EdgeLoop")
      .setWeight(0.5).setDescription("Edge Loop")).setId("n1::e0");
    model.addEdge(new Edge().setSourceVertex(v_Rossiya).setTargetVertex(v_Profile).setName("e_Edge1")).setId("e0");
    model.addEdge(new Edge().setSourceVertex(v_Profile).setTargetVertex(v_Rossiya).setName("e_BackLink")
      .setDescription("Minor").setWeight(0.533)).setId("e2");
    model.addEdge(new Edge().setSourceVertex(v_Profile).setTargetVertex(v_ProfileItemsOld).setName("e_OpenItem")).setId("n0::e0");
    model.addEdge(new Edge().setSourceVertex(v_ProfileItemsOld).setTargetVertex(v_Rossiya).setName("e_BackLink")
      .setDescription("Major")).setId("e1");

    Context writeContext = new TestExecutionContext().setModel(model.build());
    List<Context> writeContexts = new ArrayList<>(singleton(writeContext));

    // Write the graphml file
    Path tmpFolder = testFolder.getRoot().toPath();
    new YEdContextFactory().writeToSeparateFiles(writeContexts.get(0), tmpFolder);

    // Read the graphml file
    Context readContext = new YEdContextFactory().create(
      Files.walk(tmpFolder).filter(Files::isRegularFile).collect(toList()));

    // Compare
    // The start vertex is removed automatically
    assertThat(readContext.getModel().getEdges(), hasItem(hasProperty("name", equalTo("e_BackLink$1"))));
    assertThat(readContext.getModel().getEdges(), hasItem(hasProperty("name", equalTo("e_BackLink$2"))));
    assertThat(readContext.getModel().getVertices().size(), is(4 - 1));
    assertThat(readContext.getModel().getEdges().size(), is(6));
    assertThat(readContext.getModel().getEdges(), hasItem(hasProperty("weight", equalTo(0.5))));
    assertThat(readContext.getModel().getEdges(), hasItem(hasProperty("weight", equalTo(0.533))));
    assertThat(readContext.getModel().getEdges(), hasItem(hasProperty("description", equalTo("Edge Loop"))));
  }

  @Test
  public void readCommentaryCodeTag() throws IOException {
    Context context = new YEdContextFactory().create(Paths.get("graphml/tags/code.graphml")).get(0);
    RuntimeModel model = context.getModel();
    List<RuntimeEdge> edges = model.getEdges();
    assertThat(edges.get(0).getCodeTag().toString(), equalTo("@code message(\"Hello World\");"));
    assertThat(edges.get(1).getCodeTag().toString(), equalTo("@code message((Number)id((Boolean)flag()));"));
    List<RuntimeVertex> vertices = model.getVertices();
    assertThat(vertices.get(0).getCodeTag().toString(), equalTo("@code (Boolean)message((String)prefix(\"pre\"), (String)suffix());"));
    assertThat(vertices.get(1).getCodeTag().toString(), equalTo("@code (Boolean)message((Number)convert(1.0));"));
  }

  @Test
  public void readDatasetSingleEdge() throws IOException {
    Context context = new YEdContextFactory().create(Paths.get("graphml/dataset/singleEdge.graphml")).get(0);
    RuntimeModel model = context.getModel();
    List<RuntimeEdge> edges = model.getEdges();
    assertThat("Should be init edge and two parametrized", edges, hasSize(3));
    assertThat("First row should have username=admin", edges, hasItem(hasProperty("arguments",
      hasItem(both(hasProperty("name", equalTo("username"))).and(hasProperty("value", equalTo("admin")))))
    ));
    assertThat("Second row should have username=root", edges, hasItem(hasProperty("arguments",
      hasItem(both(hasProperty("name", equalTo("username"))).and(hasProperty("value", equalTo("root")))))
    ));
  }

  @Test
  public void readDatasetMultiEdge() throws IOException {
    Context context = new YEdContextFactory().create(Paths.get("graphml/dataset/multiEdge.graphml")).get(0);
    RuntimeModel model = context.getModel();
    List<RuntimeEdge> edges = model.getEdges();
    assertThat("Should be init edge and four parametrized", edges, hasSize(5));
    assertThat("First row should have username=admin,password=pass", edges, hasItem(hasProperty("arguments",
      both(hasItem(both(hasProperty("name", equalTo("username"))).and(hasProperty("value", equalTo("admin")))))
        .and(hasItem(both(hasProperty("name", equalTo("password"))).and(hasProperty("value", equalTo("pass"))))))
    ));
    assertThat("Second row should have username=root,password=secret", edges, hasItem(hasProperty("arguments",
      both(hasItem(both(hasProperty("name", equalTo("username"))).and(hasProperty("value", equalTo("root")))))
        .and(hasItem(both(hasProperty("name", equalTo("password"))).and(hasProperty("value", equalTo("secret")))))
    )));
  }

  @Test
  public void readDatasetCodeTagSingleEdge() throws IOException {
    Context context = new YEdContextFactory().create(Paths.get("graphml/dataset/codeTagSingleEdge.graphml")).get(0);
    RuntimeModel model = context.getModel();
    List<RuntimeEdge> edges = model.getEdges();
    assertThat("Should be init edge and two parametrized", edges, hasSize(3));
    assertThat("First row should have username=admin,password=pass", edges, hasItem(hasProperty("arguments",
      both(hasItem(both(hasProperty("name", equalTo("username"))).and(hasProperty("value", equalTo("admin")))))
        .and(hasItem(both(hasProperty("name", equalTo("password"))).and(hasProperty("value", equalTo("pass"))))))
    ));
    assertThat("Second row should have username=root,password=secret", edges, hasItem(hasProperty("arguments",
      both(hasItem(both(hasProperty("name", equalTo("username"))).and(hasProperty("value", equalTo("root")))))
        .and(hasItem(both(hasProperty("name", equalTo("password"))).and(hasProperty("value", equalTo("secret")))))
    )));
    assertThat("Edges should be correctly initialized", edges,
      hasItem(hasProperty("codeTag", hasToString("@code fill(username, password);")))
    );
  }

  @Test
  public void readDatasetCodeTagMultiEdge() throws IOException {
    Context context = new YEdContextFactory().create(Paths.get("graphml/dataset/codeTagMultiEdge.graphml")).get(0);
    RuntimeModel model = context.getModel();
    List<RuntimeEdge> edges = model.getEdges();
    List<RuntimeVertex> vertices = model.getVertices();
    assertThat("Should be init edge and four parametrized", edges, hasSize(5));
    assertThat("First row should have username=admin,password=pass", edges, hasItem(hasProperty("arguments",
      both(hasItem(both(hasProperty("name", equalTo("username"))).and(hasProperty("value", equalTo("admin")))))
        .and(hasItem(both(hasProperty("name", equalTo("password"))).and(hasProperty("value", equalTo("true"))))))
    ));
    assertThat("Second row should have username=root,password=secret", edges, hasItem(hasProperty("arguments",
      both(hasItem(both(hasProperty("name", equalTo("username"))).and(hasProperty("value", equalTo("root")))))
        .and(hasItem(both(hasProperty("name", equalTo("password"))).and(hasProperty("value", equalTo("false")))))
    )));
    assertThat("Edges should be correctly initialized", edges, allOf(
      hasItem(hasProperty("codeTag", hasToString("@code fillUser(username);"))),
      hasItem(hasProperty("codeTag", hasToString("@code fillPass(password);")))
    ));
    assertThat("Vertices should be correctly initialized", vertices,
      hasItem(hasProperty("codeTag", hasToString("@code (Boolean)isAlert(username);")))
    );
  }

  @Test
  public void readStyledVertices() throws IOException {
    Context context = new YEdContextFactory().create(Paths.get("graphml/style/vertexStyle.graphml")).get(0);
    RuntimeModel model = context.getModel();
    List<RuntimeVertex> vertices = model.getVertices();
    assertThat("Should be five vertices", vertices, hasSize(5));
    assertThat("First vertex should be orange with yellow", vertices, hasItem(hasProperty("style", allOf(
      hasProperty("fill", allOf(
        hasProperty("color", hasProperty("value", equalTo("#FF9900"))),
        hasProperty("color2", hasProperty("value", equalTo("#FFFF00")))
      )),
      hasProperty("configuration", equalTo(new Configuration("BevelNode2"))
    )))));
  }

  @Test
  public void readOvergrouped() {
    Context context = new YEdContextFactory().create(asList(
      Paths.get("graphml/overgrouped/a/modelGrouped.graphml"),
      Paths.get("graphml/overgrouped/a/modelLinked.graphml"),
      Paths.get("graphml/overgrouped/b/outerLinked.graphml")
    ));
    RuntimeModel model = context.getModel();

    List<RuntimeVertex> vertices = model.getVertices();

    assertThat(vertices, allOf(
      hasItem(both(hasProperty("groupName", equalTo("modelGrouped"))).and(hasProperty("overGroup", equalTo("a")))),
      hasItem(both(hasProperty("groupName", equalTo("modelLinked"))).and(hasProperty("overGroup", equalTo("a")))),
      hasItem(both(hasProperty("groupName", equalTo("outerLinked"))).and(hasProperty("overGroup", equalTo("b"))))
    ));
  }

  @Test
  public void readOverGrouped() throws IOException {
    List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/grouped/modelOvergrouped.graphml"));
    Context context = contexts.get(0);
    RuntimeModel model = context.getModel();

    List<RuntimeVertex> vertices = model.getVertices();

    assertThat(vertices, hasSize(1));
    assertThat(vertices.get(0), hasProperty("name", equalTo("v_Started")));
    assertThat(vertices.get(0), hasProperty("groupName", equalTo("group")));
    assertThat(vertices.get(0), hasProperty("overGroup", equalTo("overgroup")));
  }

  @Test
  public void readHeavyOverGrouped() throws IOException {
    List<Context> contexts = new YEdContextFactory().create(Paths.get("graphml/grouped/modelHeavyOvergrouped.graphml"));
    Context context = contexts.get(0);
    RuntimeModel model = context.getModel();

    List<RuntimeVertex> vertices = model.getVertices();
    for (int i = 0; i < vertices.size(); i++) {
      RuntimeVertex v = vertices.get(i);
      System.out.format("%d. %s :: %s :: %s\n", i+1, v.getOverGroup(), v.getGroupName(), v.getName());
    }

    assertThat(vertices, hasSize(9));
    assertThat(vertices, hasItem(allOf(
      hasProperty("name", equalTo("v_AA1")),
      hasProperty("groupName", equalTo("A")),
      hasProperty("overGroup", equalTo("overgroup"))
    )));
    assertThat(vertices, hasItem(allOf(
      hasProperty("name", equalTo("v_AA2")),
      hasProperty("groupName", equalTo("A")),
      hasProperty("overGroup", equalTo("overgroup"))
    )));
    assertThat(vertices, hasItem(allOf(
      hasProperty("name", equalTo("v_AB1")),
      hasProperty("groupName", equalTo("B")),
      hasProperty("overGroup", equalTo("overgroup"))
    )));
    assertThat(vertices, hasItem(allOf(
      hasProperty("name", equalTo("v_AB2")),
      hasProperty("groupName", equalTo("B")),
      hasProperty("overGroup", equalTo("overgroup"))
    )));
    assertThat(vertices, hasItem(allOf(
      hasProperty("name", equalTo("v_BC1")),
      hasProperty("groupName", equalTo("C")),
      hasProperty("overGroup", equalTo("another"))
    )));
    assertThat(vertices, hasItem(allOf(
      hasProperty("name", equalTo("v_BC2")),
      hasProperty("groupName", equalTo("C")),
      hasProperty("overGroup", equalTo("another"))
    )));
    assertThat(vertices, hasItem(allOf(
      hasProperty("name", equalTo("v_Started")),
      hasProperty("groupName", nullValue()),
      hasProperty("overGroup", nullValue())
    )));
    assertThat(vertices, hasItem(allOf(
      hasProperty("name", equalTo("v_XX1")),
      hasProperty("groupName", nullValue()),
      hasProperty("overGroup", nullValue())
    )));
    assertThat(vertices, hasItem(allOf(
      hasProperty("name", equalTo("v_BX1")),
      hasProperty("groupName", equalTo("another")),
      hasProperty("overGroup", equalTo("another"))
    )));
  }

  @Test
  public void readStyledEdges() throws IOException {
    Context context = new YEdContextFactory().create(Paths.get("graphml/style/edgeStyle.graphml")).get(0);
    RuntimeModel model = context.getModel();
    List<RuntimeEdge> edges = model.getEdges();
    assertThat("Should be four edges", edges, hasSize(4));
    assertThat("Should be purple bold edge", edges, hasItem(hasProperty("style", allOf(
      hasProperty("color", hasProperty("value", equalTo("#FF00FF"))),
      hasProperty("width", hasProperty("value", equalTo(7.0))),
      hasProperty("type", hasProperty("value", equalTo("line")))
    ))));
    assertThat("Should be black bold edge", edges, hasItem(hasProperty("style", allOf(
      hasProperty("color", hasProperty("value", equalTo("#000000"))),
      hasProperty("width", hasProperty("value", equalTo(7.0))),
      hasProperty("type", hasProperty("value", equalTo("line")))
    ))));
    assertThat("Should be black dashed edge", edges, hasItem(hasProperty("style", allOf(
      hasProperty("color", hasProperty("value", equalTo("#000000"))),
      hasProperty("width", hasProperty("value", equalTo(5.0))),
      hasProperty("type", hasProperty("value", equalTo("dashed")))
    ))));
    assertThat("Should be red edge", edges, hasItem(hasProperty("style", allOf(
      hasProperty("color", hasProperty("value", equalTo("#FF0000"))),
      hasProperty("width", hasProperty("value", equalTo(1.0))),
      hasProperty("type", hasProperty("value", equalTo("line")))
    ))));
  }
}
