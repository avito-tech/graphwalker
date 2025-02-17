package org.graphwalker.yed;

/*
 * #%L
 * GraphWalker text parser
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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.graphwalker.dsl.yed.YEdEdgeParser;
import org.graphwalker.dsl.yed.YEdLabelLexer;
import org.graphwalker.dsl.yed.YEdVertexParser;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;


/**
 * @author Nils Olsson
 */
public class GrammarTest {

  private List<String> vertices = Arrays.asList(
    "",
    " ",
    "word1",
    "word1 BLOCKED",
    "word1\nBLOCKED",
    "word1 INIT: x=y;",
    "word1 INIT:x=y;",
    "word1 INIT: x=y;z=0;",
    "word1\n INIT: x=y;",
    "word1 REQTAG:UC02 3.4.1",
    "word1 REQTAG:UC02 3.4.1, UC02 3.4.2",
    "word1\nREQTAG:UC02 3.4.1",
    "word1\nREQTAG:UC02 3.4.1, UC02 3.4.2",
    "word1.word2",
    "word1;word2",
    "word1.word2;word3",
    "word1;word2.word3",
    "word1.word2.word3",
    "word1.word2.word3;word1.word2.word3;word1.word2.word3",
    "word1 // comment",
    "word1\n// my one line comment\nBLOCKED",
    "SHARED:A1",
    "SHARED :A2",
    "SHARED : A3",
    " SHARED: A4",
    "REQTAG=R1",
    "REQTAG = R1",
    "REQTAG= R1,R2 , R3, R4",
    "OUTDEGREE : e_SideBar;\nINDEGREE : e_ClickSources;",
    "INDEGREE: e_SideBar /* @code call(\"start\", (String)getValue(\"v\")) */;",
    "INDEGREE: e_SideBar /* @code (Boolean)call(\"start\", (String)getValue(\"v\")) */;",
    "INDEGREE: /* comment */ e_SideBar;",
    "INDEGREE: e_ClickBtn /* comment 2 */;",
    "INDEGREE: /* comment 1 */ e_SideBar, e_ClickBtn /* comment 2 */ [loggedIn == true];\nOUTDEGREE: e_ClickSources;",
    "INDEGREE: /* comment 1 */ e_BackLink$1 weight=0.1, /* comment 2 */ weight=0.9 e_BackLink$2;",
    "INDEGREE: e_ClickBtn /* @code run()\nComment 1 */;",
    "v_Vertex /* @code (Boolean)run(); */",
    "v_Vertex\n/* commentary */\nOUTDEGREE: e_ChooseSaleFilter, e_ChooseRentFilter;\nSET: filters = {}, authorized = true;"
  );

  private List<String> edges = Arrays.asList(
    "",
    " ",
    "word1",
    "word1[x=>y]",
    "word1\n[x=>y]",
    "word1/x=y;",
    "word1\n/x=y;",
    "word1[x=>y]/x=y;",
    "word1\n[x=>y]\n/x=y;",
    "word1.word2",
    "word1;word2",
    "word1.word2;word3",
    "word1;word2.word3",
    "word1.word2.word3",
    "word1.word2.word3;word1.word2.word3;word1.word2.word3",
    "word1 // comment",
    "word1\n// my one line comment\n[x>y]",
    "word1 / value = \"ett tu tre\";",
    "weight = 1",
    "word1 weight=0.1 // test this too",
    "word1 weight=1.0",
    "REQTAG=R1",
    "REQTAG = R1",
    "REQTAG= R1,R2 , R3, R4",
    "word1 weight=0.3",
    "word1\nweight=0.3",
    "word1\nweight=0.33333",
    "word1\nweight=.3",
    "word1\nweight=0",
    "WORD1\nweight=1",
    "word1[x=>y]/x=y;\nweight=0.3",
    "word1\nWEIGHT=0.33333",
    "word1 dependency=1",
    "word1\ndependency=1",
    "word1\ndependency=0",
    "word1[x=>y]/x=y;\ndependency=0.3",
    "word1\nDEPENDENCY=2",
    "e_Init / DropUrl=\"https://a.b.c.org/x/y/z/items.aspx\";urlInfo=\"http://data/node\";REST=true;",
    "init / elements = [1,2,3]; value = 0; toString = function(){for(var i = 0; i<elements.length;i++){value+=elements[i]}return value};",
    "e_Edge /* @code call() */",
    "e_Edge /* @code call(\"start\") */",
    "e_Edge /* @code call(123) */",
    "e_Edge /* @code call(\"start\", 123) */",
    "e_Edge /* @code call(\"start\", (String)getValue(\"v\")) */",
    "e_Edge /* @code call(true) */",
    "e_Edge /* text 1 */",
    "e_Edge /* @code call(true); comment 1 */",
    "e_Edge /* @code call((Boolean)call(true)); 1 */",
    "e_Edge /* @code call((String)getUrl()); *** Text *** */",
    "e_Edge /* @code call((String)getUrl())\n *** Text *** */",
    "e_ShowLessFilters /filters.itemType = \"sale\", filters.full = false;\n/* Press [More] */",
    "e_Edge /filters.itemType = \"sale\", filters.full = false;\n/* @code run(); Press button */",
    "e_EnterInvalidKey/incorrect=incorrect+1;",
    "e_ShowLessFilters /filters.itemType={a:2, b: 3};\n/* Press [More] */",
    "<html>e_Edge<br/>/* Text */ \n<table>\n<tr><th bgcolor=\"lime\">label</th></tr>\n<tr><td bgcolor=\"lime\">\"Car\"</td></tr>\n</table></html>",
    "e_Click [typeof gw != 'undefined' && (((gw || {}).ds || {}).e_ClickClick || {})[0].$open]",
    "e_Click / var gw = gw || {ds: {}};",
    "e_Click / value = {user: 'root'};",
    "e_Click / gw.ds.e_FillCredentials = [{user: \"admin\", $open: false}, {user: \"root\", $open: false}];",
    "e_Click {user: \"admin\", id: 1, active: true}",
    "<html>e_Edge<br/>/* @code drive(\"${vehicle}\"); Text */ \n<table>\n<tr><th>vehicle</th></tr>\n<tr><td>\"Car\"</td></tr>\n</table></html>"
  );

  @Test
  public void testVertexParser() {
    for (String vertex : vertices) {
      CharStream inputStream = CharStreams.fromString(vertex);
      YEdLabelLexer lexer = new YEdLabelLexer(inputStream);
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      YEdVertexParser parser = new YEdVertexParser(tokens);
      parser.parse();
      Assert.assertThat("Parse syntax errors in vertex " + vertex, parser.getNumberOfSyntaxErrors(), is(0));
    }
  }

  @Test
  public void testEdgeParser() {
    for (String edge : edges) {
      CharStream inputStream = CharStreams.fromString(edge);
      YEdLabelLexer lexer = new YEdLabelLexer(inputStream);
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      YEdEdgeParser parser = new YEdEdgeParser(tokens);
      parser.parse();
      Assert.assertThat("Parse syntax errors in edge " + edge, parser.getNumberOfSyntaxErrors(), is(0));
    }
  }

//  @Test
//  public void tableEdgeValuesParser() {
//    CharStream inputStream = CharStreams.fromString(
//      "<html> e_FillCredentials /* @code fill(\"${username}\", \"${password}\", ${readonly}); Fill credentials */ \n" +
//      "<table>\n" +
//      "<tr> \n" +
//      "  <th bgcolor=\"lime\">id</th> \n" +
//      "  <th bgcolor=\"yellow\">password</th>\n" +
//      "  <th bgcolor=\"orange\">readonly</th>\n" +
//      "</tr>\n" +
//      "<tr>\n" +
//      "  <td bgcolor=\"lime\">1</td>\n" +
//      "  <td bgcolor=\"yellow\">pass</td>\n" +
//      "  <td bgcolor=\"orange\">true</td>\n" +
//      "</tr>\n" +
//      "</table>\n" +
//      "</html>");
//    YEdLabelLexer lexer = new YEdLabelLexer(inputStream);
//    CommonTokenStream tokens = new CommonTokenStream(lexer);
//    YEdEdgeParser parser = new YEdEdgeParser(tokens);
//    YEdEdgeParser.ParseContext parseCtx = parser.parse();
//    Assert.assertThat("Parse syntax errors occurred", parser.getNumberOfSyntaxErrors(), is(0));
//    Assert.assertThat("Numeric values reading error", parser.dataset().htmlTable().tableRow(1).tableBodyCell(0).numericValue(), is(not(null)));
//    Assert.assertThat("String values reading error", parser.dataset().htmlTable().tableRow(1).tableBodyCell(1).stringValue(), is(not(null)));
//    Assert.assertThat("Boolean values reading error", parser.dataset().htmlTable().tableRow(1).tableBodyCell(2).booleanValue(), is(not(null)));
//  }
}
