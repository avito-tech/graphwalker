package org.graphwalker.io.factory.yed;

/*-
 * #%L
 * GraphWalker Input/Output
 * %%
 * Copyright (C) 2005 - 2018 Avito
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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.graphwalker.core.model.CodeTag;
import org.graphwalker.dsl.yed.YEdEdgeParser;
import org.graphwalker.dsl.yed.YEdVertexParser;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static org.graphwalker.core.model.TypePrefix.STRING;

/**
 * @author Ivan Bonkin
 */
final class CodeTagParser {

  public CodeTag parse(YEdEdgeParser.VoidMethodContext ctx) {
    return new CodeTag(parseEdgeExpression(ctx));
  }

  public CodeTag parseIndegree(YEdVertexParser.VoidMethodContext ctx) {
    return new CodeTag(parseVertexExpression(ctx));
  }

  public CodeTag parse(YEdVertexParser.VoidMethodContext ctx) {
    // Treat root Void method as Boolean if node type is Vertex;
    // graphwalker-dsl allows non typed root methods for vertices
    CodeTag.VoidMethod voidMethod = (CodeTag.VoidMethod) parseVertexExpression(ctx);
    return new CodeTag(new CodeTag.BooleanMethod(voidMethod.getName(), voidMethod.getArguments()));
  }

  public CodeTag parse(YEdVertexParser.BooleanMethodContext ctx) {
    return new CodeTag(parseVertexExpression(ctx));
  }

  private CodeTag.Expression parseEdgeExpression(ParserRuleContext ctx) {
    List<CodeTag.Expression> arguments = new ArrayList<>();
    String methodName = null;

    if (ctx instanceof YEdEdgeParser.VoidMethodContext) {
      for (ParseTree signaturePart : ctx.children) {
        if (signaturePart instanceof YEdEdgeParser.MethodNameContext) {
          methodName = signaturePart.getText();
        }
        if (signaturePart instanceof YEdEdgeParser.ArgListContext) {
          arguments.addAll(parseMethodArgs((YEdEdgeParser.ArgListContext) signaturePart));
        }
      }
      return new CodeTag.VoidMethod(methodName, arguments);

    } else if (ctx instanceof YEdEdgeParser.StringExpressionContext) {
      for (ParseTree methodCtx : ctx.children) {
        if (methodCtx instanceof YEdEdgeParser.StringVariableContext) {
          return new CodeTag.Value<String>(methodCtx.getText().replaceAll("(^\")|(\"$)", ""));
        }
        if (methodCtx instanceof YEdEdgeParser.StringMethodContext) {
          methodName = parseEdgeMethod((YEdEdgeParser.StringMethodContext) methodCtx, arguments);
        }
      }
      return new CodeTag.StringMethod(methodName, arguments);

    } else if (ctx instanceof YEdEdgeParser.NumberExpressionContext) {
      for (ParseTree methodCtx : ctx.children) {
        if (methodCtx instanceof YEdEdgeParser.NumberVariableContext) {
          return new CodeTag.Value<Double>(parseDouble(methodCtx.getText()));
        }
        if (methodCtx instanceof YEdEdgeParser.NumberMethodContext) {
          methodName = parseEdgeMethod((YEdEdgeParser.NumberMethodContext) methodCtx, arguments);
        }
      }
      return new CodeTag.DoubleMethod(methodName, arguments);

    } else if (ctx instanceof YEdEdgeParser.BooleanExpressionContext) {
      for (ParseTree methodCtx : ctx.children) {
        if (methodCtx instanceof YEdEdgeParser.BooleanVariableContext) {
          return new CodeTag.Value<Boolean>(parseBoolean(methodCtx.getText()));
        }
        if (methodCtx instanceof YEdEdgeParser.BooleanMethodContext) {
          methodName = parseEdgeMethod((YEdEdgeParser.BooleanMethodContext) methodCtx, arguments);
        }
      }
      return new CodeTag.BooleanMethod(methodName, arguments);

    } else if (ctx instanceof YEdEdgeParser.DatasetVariableContext) {
      return new CodeTag.DatasetVariable<String>(trim(ctx.getChild(0).getText(), "${", "}"));

    } else if (ctx instanceof YEdEdgeParser.DatasetStringVariableContext) {
      return new CodeTag.TypedDatasetVariable<String>(trim(ctx.getChild(0).getText(), "\"${", "}\""), STRING);

    } else {
      throw new IllegalStateException("Can't parse rule " + ctx.getClass().getSimpleName());
    }
  }

  private String parseEdgeMethod(ParserRuleContext methodCtx, List<CodeTag.Expression> arguments) {
    String methodName = null;
    for (ParseTree signaturePart : methodCtx.children) {
      if (signaturePart instanceof YEdEdgeParser.MethodNameContext) {
        methodName = signaturePart.getText();
      }
      if (signaturePart instanceof YEdEdgeParser.ArgListContext) {
        arguments.addAll(parseMethodArgs((YEdEdgeParser.ArgListContext) signaturePart));
      }
    }
    if (methodName == null) {
      throw new NullPointerException(
        "No method name was found in \"" + methodCtx.getClass().getSimpleName() + "\" method");
    }
    return methodName;
  }

  private List<CodeTag.Expression> parseMethodArgs(YEdEdgeParser.ArgListContext signaturePart) {
    List<CodeTag.Expression> arguments = new ArrayList<>();
    for (ParseTree argCtx : signaturePart.children) {
      if (argCtx instanceof YEdEdgeParser.ArgumentContext) {
        for (ParseTree arg : ((YEdEdgeParser.ArgumentContext) argCtx).children) {
          arguments.add(parseEdgeExpression((ParserRuleContext) arg));
        }
      }
    }
    return arguments;
  }

  private CodeTag.Expression parseVertexExpression(ParserRuleContext ctx) {
    List<CodeTag.Expression> arguments = new ArrayList<>();
    String methodName = null;

    if (ctx instanceof YEdVertexParser.VoidMethodContext) {
      for (ParseTree signaturePart : ctx.children) {
        if (signaturePart instanceof YEdVertexParser.MethodNameContext) {
          methodName = signaturePart.getText();
        }
        if (signaturePart instanceof YEdVertexParser.ArgListContext) {
          arguments.addAll(parseMethodArgs((YEdVertexParser.ArgListContext) signaturePart));
        }
      }
      return new CodeTag.VoidMethod(methodName, arguments);

    } else if (ctx instanceof YEdVertexParser.StringExpressionContext) {
      for (ParseTree methodCtx : ctx.children) {
        if (methodCtx instanceof YEdVertexParser.StringVariableContext) {
          return new CodeTag.Value<String>(methodCtx.getText().replaceAll("(^\")|(\"$)", ""));
        }
        if (methodCtx instanceof YEdVertexParser.StringMethodContext) {
          methodName = parseVertexMethod((YEdVertexParser.StringMethodContext) methodCtx, arguments);
        }
      }
      return new CodeTag.StringMethod(methodName, arguments);

    } else if (ctx instanceof YEdVertexParser.NumberExpressionContext) {
      for (ParseTree methodCtx : ctx.children) {
        if (methodCtx instanceof YEdVertexParser.NumberVariableContext) {
          return new CodeTag.Value<Double>(parseDouble(methodCtx.getText()));
        }
        if (methodCtx instanceof YEdVertexParser.NumberMethodContext) {
          methodName = parseVertexMethod((YEdVertexParser.NumberMethodContext) methodCtx, arguments);
        }
      }
      return new CodeTag.DoubleMethod(methodName, arguments);

    } else if (ctx instanceof YEdVertexParser.BooleanExpressionContext) {
      for (ParseTree methodCtx : ctx.children) {
        if (methodCtx instanceof YEdVertexParser.BooleanVariableContext) {
          return new CodeTag.Value<Boolean>(parseBoolean(methodCtx.getText()));
        }
        if (methodCtx instanceof YEdVertexParser.BooleanMethodContext) {
          methodName = parseVertexMethod((YEdVertexParser.BooleanMethodContext) methodCtx, arguments);
        }
      }
      return new CodeTag.BooleanMethod(methodName, arguments);

    } else if (ctx instanceof YEdVertexParser.BooleanMethodContext) {
      for (ParseTree signaturePart : ((YEdVertexParser.BooleanMethodContext) ctx).children) {
        if (signaturePart instanceof YEdVertexParser.MethodNameContext) {
          methodName = signaturePart.getText();
        }
        if (signaturePart instanceof YEdVertexParser.ArgListContext) {
          arguments.addAll(parseMethodArgs((YEdVertexParser.ArgListContext) signaturePart));
        }
      }
      return new CodeTag.BooleanMethod(methodName, arguments);

    } else if (ctx instanceof YEdVertexParser.DatasetVariableContext) {
      return new CodeTag.TypedDatasetVariable<String>(trim(ctx.getChild(0).getText(), "\"${", "}\""), STRING);

    } else if (ctx instanceof YEdVertexParser.DatasetStringVariableContext) {
      return new CodeTag.DatasetVariable<String>(trim(ctx.getChild(0).getText(), "\"${", "}\""));

    } else {
      throw new IllegalStateException("Can't parse rule " + ctx.getClass().getSimpleName());
    }
  }

  private String parseVertexMethod(ParserRuleContext methodCtx, List<CodeTag.Expression> arguments) {
    String methodName = null;
    for (ParseTree signaturePart : methodCtx.children) {
      if (signaturePart instanceof YEdVertexParser.MethodNameContext) {
        methodName = signaturePart.getText();
      }
      if (signaturePart instanceof YEdVertexParser.ArgListContext) {
        arguments.addAll(parseMethodArgs((YEdVertexParser.ArgListContext) signaturePart));
      }
    }
    if (methodName == null) {
      throw new NullPointerException(
        "No method name was found in \"" + methodCtx.getClass().getSimpleName() + "\" method");
    }
    return methodName;
  }

  private List<CodeTag.Expression> parseMethodArgs(YEdVertexParser.ArgListContext signaturePart) {
    List<CodeTag.Expression> arguments = new ArrayList<>();
    for (ParseTree argCtx : signaturePart.children) {
      if (argCtx instanceof YEdVertexParser.ArgumentContext) {
        for (ParseTree arg : ((YEdVertexParser.ArgumentContext) argCtx).children) {
          arguments.add(parseVertexExpression((ParserRuleContext) arg));
        }
      }
    }
    return arguments;
  }

  private static String trim(String value, String prefix, String suffix) {
    int indexOfLast = value.lastIndexOf(suffix);
    String noSuffix = value.substring(0, indexOfLast);
    return noSuffix.substring(noSuffix.startsWith(prefix) ? prefix.length() : 0);
  }

}
