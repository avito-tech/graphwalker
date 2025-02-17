package org.graphwalker.maven.plugin;

/*
 * #%L
 * GraphWalker Maven Plugin
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

import org.apache.maven.model.Resource;
import org.graphwalker.java.source.CodeGenerator;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * @author Nils Olsson
 */
public abstract class GenerateMojoBase extends DefaultMojoBase {

  public GenerateMojoBase() {
  }

  protected abstract File getGeneratedSourcesDirectory();

  protected abstract GenerateMainMojo.YEd getYEd();

  protected void generate(List<Resource> resources) {
    YEd yEd = getYEd();
    Map<String, Object> options = new HashMap<>();
    if (null != yEd) {
      options.putAll(yEd.getProperties());
    }
    for (Resource resource : resources) {
      generate(resource, options);
    }
  }

  private void generate(Resource resource, Map<String, Object> options) {
    File baseDirectory = new File(resource.getDirectory());
    CodeGenerator.generate(baseDirectory.toPath(), getGeneratedSourcesDirectory().toPath(), options);
  }

  public static class YEd {

    /**
     * Set to false if you would like to skip reusing user defined styles in generated yEd file in /link directory and use default styling.
     */
    public boolean linkStyles;

    /**
     * Edge source arrow between different groups in generated /link file.
     */
    protected String crossGroupSourceArrow;

    protected String sameGroupSourceArrow;

    protected String crossGroupTargetArrow;

    protected String sameGroupTargetArrow;

    protected String crossGroupLineStyle;

    protected String sameGroupLineStyle;

    public Map<String, Object> getProperties() {
      Map<String, Object> map = new HashMap<>();
      map.put("linkStyles", linkStyles);
      map.put("crossGroupSourceArrow", crossGroupSourceArrow);
      map.put("sameGroupSourceArrow", sameGroupSourceArrow);
      map.put("crossGroupTargetArrow", crossGroupTargetArrow);
      map.put("sameGroupTargetArrow", sameGroupTargetArrow);
      map.put("crossGroupLineStyle", crossGroupLineStyle);
      map.put("sameGroupLineStyle", sameGroupLineStyle);
      map.values().removeIf(Objects::isNull);
      return map;
    }
  }
}
