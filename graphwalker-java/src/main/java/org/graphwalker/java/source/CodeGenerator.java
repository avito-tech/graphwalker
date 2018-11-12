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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import org.apache.commons.collections4.set.ListOrderedSet;
import org.graphwalker.core.machine.Context;
import org.graphwalker.core.model.Argument;
import org.graphwalker.core.model.CodeTag;
import org.graphwalker.core.model.Edge.RuntimeEdge;
import org.graphwalker.core.model.TypePrefix;
import org.graphwalker.core.model.Vertex.RuntimeVertex;
import org.graphwalker.io.factory.ContextFactory;
import org.graphwalker.io.factory.ContextFactoryScanner;
import org.graphwalker.java.source.cache.Cache;
import org.graphwalker.java.source.cache.CacheEntry;
import org.graphwalker.java.source.cache.SimpleCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import static com.github.javaparser.ast.Modifier.DEFAULT;
import static com.github.javaparser.ast.Modifier.PUBLIC;
import static com.github.javaparser.ast.type.PrimitiveType.booleanType;
import static com.github.javaparser.ast.type.PrimitiveType.doubleType;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.EnumSet.noneOf;
import static java.util.EnumSet.of;
import static org.graphwalker.core.model.Model.RuntimeModel;
import static org.graphwalker.core.model.TypePrefix.BOOLEAN;
import static org.graphwalker.core.model.TypePrefix.NUMBER;
import static org.graphwalker.core.model.TypePrefix.STRING;
import static org.graphwalker.core.model.TypePrefix.VOID;

/**
 * @author Nils Olsson
 */
public final class CodeGenerator extends VoidVisitorAdapter<ChangeContext> {

  private static final Logger logger = LoggerFactory.getLogger(CodeGenerator.class);
  private static CodeGenerator generator = new CodeGenerator();

  private static class SearchForLinkedFileVisitor extends SimpleFileVisitor<Path> {

    private final SimpleCache cache;
    private final ListOrderedSet<Path> linkedFiles = new ListOrderedSet<>();

    public SearchForLinkedFileVisitor(Path output) {
      this.cache = new SimpleCache(output.resolve("link"));
    }

    /**
     * @return unmodifiable view of files to linked in a single context.
     */
    public List<Path> getLinkedFiles() {
      return linkedFiles.asList();
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      if (dir.toFile().getName().equals("link")) {
        return SKIP_SUBTREE;
      } else {
        return CONTINUE;
      }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
      if (!cache.contains(file) || isModified(file, cache)) {
        try {
          ContextFactory factory = ContextFactoryScanner.get(file);
          List<Context> contexts = factory.create(file);

          for (Context context : contexts) {
            for (RuntimeVertex vertex : context.getModel().getVertices()) {
              if (vertex.hasIndegrees() || vertex.hasOutdegrees()) {
                // if file contains INDEGREE/OUTDEGREE, mark it
                if (context.getNextElement() != null) {
                  linkedFiles.add(0, file);
                } else {
                  linkedFiles.add(file);
                }
                return CONTINUE;
              }
            }
          }
          // otherwise remove previously marked file
          linkedFiles.remove(file);
          cache.add(file, new CacheEntry(file.toFile().lastModified(), true));

        } catch (Throwable t) {
          logger.error(t.getMessage());
          cache.add(file, new CacheEntry(file.toFile().lastModified(), false));
        }
      }
      return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
      cache.add(file, new CacheEntry(file.toFile().lastModified(), false));
      return CONTINUE;
    }
  }

  private static class MergeLinkedFileVisitor extends SimpleFileVisitor<Path> {

    private final Path input, output;
    private final List<Path> linkedFiles;
    private final SimpleCache cache;

    public MergeLinkedFileVisitor(Path input, Path output, List<Path> linkedFiles) {
      this.input = input;
      this.output = output;
      this.linkedFiles = linkedFiles;
      this.cache = new SimpleCache(output);
      if (!linkedFiles.isEmpty()) {
        FactoryCodeGenerator.writeFactorySource(
          linkedFiles,
          new SourceFile(new ClassName("ContextFactory"), linkedFiles.get(0), input, output));
      }
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      Objects.requireNonNull(dir);
      Objects.requireNonNull(attrs);

      if (input.equals(dir) && !linkedFiles.isEmpty()) {
        Path entry = linkedFiles.get(0);
        try {
          ContextFactory factory = ContextFactoryScanner.get(entry);
          Context context = factory.create(linkedFiles);
          for (Path linkedFile : linkedFiles) {
            SourceFile sourceFile = new SourceFile(new ClassName(linkedFile), linkedFile, input, output);
            writeSource(context, sourceFile);
          }
          factory.write(singletonList(context), dir.resolve("link"));
          cache.add(entry, new CacheEntry(entry.toFile().lastModified(), true));
        } catch (Throwable t) {
          logger.error(t.getMessage());
          cache.add(entry, new CacheEntry(entry.toFile().lastModified(), false));
        }
      }
      return CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
      if ((!cache.contains(file) || isModified(file, cache)) && !linkedFiles.contains(file)) {
        try {
          ContextFactory factory = ContextFactoryScanner.get(file);
          List<Context> contexts = factory.create(file);
          for (Context context : contexts) {
            SourceFile sourceFile = new SourceFile(new ClassName(context.getModel().getName()), file, input, output);
            writeSource(context, sourceFile);
            cache.add(file, new CacheEntry(file.toFile().lastModified(), true));
          }
        } catch (Throwable t) {
          logger.error(t.getMessage());
          cache.add(file, new CacheEntry(file.toFile().lastModified(), false));
        }
      }
      return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
      cache.add(file, new CacheEntry(file.toFile().lastModified(), false));
      return CONTINUE;
    }
  }

  private static boolean isModified(Path file, Cache<Path, CacheEntry> cache) throws IOException {
    return !Files.getLastModifiedTime(file).equals(cache.get(file).getLastModifiedTime());
  }

  public static void generate(final Path input, final Path output) {
    SearchForLinkedFileVisitor searchForLinkedFileVisitor = new SearchForLinkedFileVisitor(output);
    try {
      Files.walkFileTree(input, searchForLinkedFileVisitor);
      Files.walkFileTree(input, new MergeLinkedFileVisitor(input, output, searchForLinkedFileVisitor.getLinkedFiles()));
    } catch (IOException e) {
      logger.error(e.getMessage());
      throw new CodeGeneratorException(e);
    }
  }

  private static void writeSource(Context context, SourceFile file) throws IOException {
    try {
      RuntimeModel model = context.getModel();
      String source = generator.generate(file, model);
      Files.createDirectories(file.getOutputPath().getParent());
      Files.write(file.getOutputPath(), source.getBytes(Charset.forName("UTF-8")), CREATE, TRUNCATE_EXISTING);
    } catch (Throwable t) {
      logger.error(t.getMessage());
      throw new CodeGeneratorException(t);
    }
  }

  public List<String> generate(String file) throws IOException {
    return generate(Paths.get(file));
  }

  public List<String> generate(File file) throws IOException {
    return generate(file.toPath());
  }

  public List<String> generate(Path path) throws IOException {
    ContextFactory factory = ContextFactoryScanner.get(path);
    List<Context> contexts = factory.create(path);
    List<String> sources = new ArrayList<>();
    for (Context context : contexts) {
      String sourceStr = "";
      SourceFile sourceFile = new SourceFile(new ClassName(context.getModel().getName()), path);
      sourceStr += generate(sourceFile, context.getModel());
      sources.add(sourceStr);
    }
    return sources;
  }

  public String generate(SourceFile sourceFile, RuntimeModel model) {
    CompilationUnit compilationUnit = getCompilationUnit(sourceFile);
    ChangeContext changeContext = new ChangeContext(model, sourceFile);
    visit(compilationUnit, changeContext);
    removeMethods(compilationUnit, changeContext);
    generateMethods(compilationUnit, changeContext);
    return compilationUnit.toString();
  }

  private CompilationUnit getCompilationUnit(SourceFile sourceFile) {
    CompilationUnit compilationUnit;
    if (Files.exists(sourceFile.getOutputPath())) {
      try {
        compilationUnit = JavaParser.parse(sourceFile.getOutputPath().toFile());
      } catch (Throwable t) {
        logger.error(t.getMessage());
        throw new RuntimeException(t);
      }
    } else {
      compilationUnit = new CompilationUnit();
      compilationUnit.setComment(new LineComment(" Generated by GraphWalker (http://www.graphwalker.org)"));
      if (!"".equals(sourceFile.getPackageName())) {
        compilationUnit.setPackageDeclaration(createPackageDeclaration(sourceFile));
      }
      compilationUnit.setImports(new NodeList<>(
        new ImportDeclaration(new Name("org.graphwalker.java.annotation.Model"), false, false),
        new ImportDeclaration(new Name("org.graphwalker.java.annotation.Vertex"), false, false),
        new ImportDeclaration(new Name("org.graphwalker.java.annotation.Edge"), false, false),
        new ImportDeclaration(new Name("org.graphwalker.java.annotation.Dataset"), true, true)
      ));
      compilationUnit.addType(getInterfaceName(sourceFile));
    }
    return compilationUnit;
  }

  private void removeMethods(CompilationUnit compilationUnit, ChangeContext changeContext) {
    if (0 < changeContext.getMethodDeclarations().size()) {
      ClassOrInterfaceDeclaration body = (ClassOrInterfaceDeclaration) compilationUnit.getTypes().get(0);
      body.getMembers().removeAll(changeContext.getMethodDeclarations());
    }
  }

  private void generateMethods(CompilationUnit compilationUnit, ChangeContext changeContext) {
    ClassOrInterfaceDeclaration body = (ClassOrInterfaceDeclaration) compilationUnit.getTypes().get(0);
    Set<MethodDeclaration>
      abstractNodeMethods = new HashSet<>(),
      defaultNodeMethods = new HashSet<>(),
      codeTagMethods = new HashSet<>();
    for (String methodName : changeContext.getMethodNames()) {
      if (isValidMethodName(methodName)) {
        List<RuntimeVertex> vertices = changeContext.getModel().findVertices(methodName);
        NodeList<AnnotationExpr> annotations = new NodeList<>();
        Type type;
        CodeTag codeTag;
        List<Argument> argumentRow = null;
        if (vertices != null) {
          String description = getOrElse(vertices, unquote(RuntimeVertex::getDescription), "");
          codeTag = getOrElse(vertices, RuntimeVertex::getCodeTag, null);
          NodeList<MemberValuePair> memberValuePairs = new NodeList<>(new MemberValuePair("value", new StringLiteralExpr(description)));
          annotations.add(new NormalAnnotationExpr(new Name("Vertex"), memberValuePairs));
          type = booleanType();
        } else {
          List<RuntimeEdge> edges = changeContext.getModel().findEdges(methodName);
          if (edges != null) {
            String description = getOrElse(edges, unquote(RuntimeEdge::getDescription), "");
            codeTag = getOrElse(edges, RuntimeEdge::getCodeTag, null);
            NodeList<MemberValuePair> memberValuePairs = new NodeList<>(new MemberValuePair("value", new StringLiteralExpr(description)));
            annotations.add(new NormalAnnotationExpr(new Name("Edge"), memberValuePairs));
            List<List<Argument>> arguments = getOrElse(edges, RuntimeEdge::getArguments, emptyList());
            for (int i = 0; i < arguments.size(); i++) {
              argumentRow = arguments.get(i);
              NodeList<Expression> rowFields = new NodeList<>();
              for (Argument argument : argumentRow) {
                NodeList<MemberValuePair> valueAnnotation = new NodeList<>();
                valueAnnotation.add(new MemberValuePair("name", new StringLiteralExpr(argument.getName())));
                valueAnnotation.add(new MemberValuePair("value", new StringLiteralExpr(argument.getValue())));
                rowFields.add(new NormalAnnotationExpr(new Name("Value"), valueAnnotation));
              }
              NodeList<MemberValuePair> valueAnnotations = new NodeList<>(new MemberValuePair("value", new ArrayInitializerExpr(rowFields)));
              annotations.add(new NormalAnnotationExpr(new Name("Row"), valueAnnotations));
            }
            type = new VoidType();
          } else {
            throw new IllegalStateException("No vertices or edges were found for method: \"" + methodName + "\"");
          }
        }
        if (codeTag != null) {
          List<MethodDeclaration> extractedMethods = extractMethods(codeTag.getMethod());
          codeTagMethods.addAll(extractedMethods);
          String stmtStart = type.isVoidType() ? "" : "return ";
          Statement statement = JavaParser.parseStatement(stmtStart + codeTag.getMethod().asJavaMethodCall());
          BlockStmt methodCall = new BlockStmt(new NodeList<>(statement));
          defaultNodeMethods.add(
            new MethodDeclaration(of(DEFAULT), type, methodName)
              .setAnnotations(annotations)
              .setBody(methodCall));
        } else {
          MethodDeclaration methodDeclaration = new MethodDeclaration(noneOf(Modifier.class), type, methodName)
            .setBody(null)
            .setAnnotations(annotations);
          if (argumentRow != null) {
            NodeList<Parameter> parameters = new NodeList<>();
            for (Argument argument : argumentRow) {
              Type argumentType = typePrefixToAstType(argument.getType());
              parameters.add(new Parameter(argumentType, new SimpleName(argument.getName())));
            }
            methodDeclaration.setParameters(parameters);
          }
          abstractNodeMethods.add(methodDeclaration);
        }
      }
    }
    addSortedMethods(body, defaultNodeMethods);
    addSortedMethods(body, codeTagMethods);
    addSortedMethods(body, abstractNodeMethods);
  }

  private static void addSortedMethods(ClassOrInterfaceDeclaration body, Set<MethodDeclaration> methodDeclarations) {
    Comparator<MethodDeclaration> inAlphabeticalOrder = comparing(MethodDeclaration::getDeclarationAsString);
    TreeSet<MethodDeclaration> sortedByName = new TreeSet<>(inAlphabeticalOrder);
    for (MethodDeclaration declaration : methodDeclarations) {
      if (!sortedByName.add(declaration)) {
        throw new IllegalStateException("Method \"" + declaration + "\" id already defined with another return type");
      }
    }
    sortedByName.forEach(body::addMember);
  }

  private static Type typePrefixToAstType(TypePrefix typePrefix) {
    switch (typePrefix) {
      case VOID:
        return new VoidType();
      case NUMBER:
        return doubleType();
      case STRING:
        return JavaParser.parseClassOrInterfaceType("java.lang.String");
      case BOOLEAN:
        return booleanType();
      default:
        throw new IllegalStateException("Can not match TypePrefix \"" + typePrefix + "\"");
    }
  }

  private static Type valueToAstType(CodeTag.Value<?> valueExpression) {
    Object expressionResult = ((CodeTag.Value) valueExpression).result();
    if (expressionResult instanceof String) {
      return typePrefixToAstType(STRING);
    } else if (expressionResult instanceof Double) {
      return typePrefixToAstType(NUMBER);
    } else if (expressionResult instanceof Boolean) {
      return typePrefixToAstType(BOOLEAN);
    } else if (expressionResult instanceof Void) {
      return typePrefixToAstType(VOID);
    }
    throw new IllegalStateException("Can not match Value expression \"" + valueExpression + "\"");
  }

  private static <T extends CodeTag.AbstractMethod> List<MethodDeclaration> extractMethods(T method) {
    List<MethodDeclaration> methods = new ArrayList<>();
    MethodDeclaration methodDeclaration = new MethodDeclaration(
      noneOf(Modifier.class),
      typePrefixToAstType(method.getTypePrefix()),
      method.getName());
    methodDeclaration.setBody(null);
    NodeList<Parameter> parameters = new NodeList<>();

    for (CodeTag.Expression<?> expression : method.getArguments()) {
      if (expression instanceof CodeTag.AbstractMethod) {
        List<MethodDeclaration> nestedMethods = extractMethods((CodeTag.AbstractMethod) expression);
        methods.addAll(nestedMethods);
        parameters.add(new Parameter(
          nestedMethods.get(0).getType(),
          "arg" + parameters.size()));

      } else if (expression instanceof CodeTag.Value) {
        parameters.add(new Parameter(
          valueToAstType((CodeTag.Value) expression),
          "arg" + parameters.size()));
      }
    }
    methods.add(0, methodDeclaration.setParameters(parameters));
    return methods;
  }

  private static <T> Function<T, String> unquote(Function<T, String> extractor) {
    return extractor.andThen(s -> s.replaceAll("\"", "\\\\\""));
  }

  private static <T, V> V getOrElse(List<T> elements, Function<T, V> extractor, V defaultValue) {
    V result = elements.isEmpty() ? defaultValue : extractor.apply(elements.iterator().next());
    return result != null ? result : defaultValue;
  }

  static boolean isValidMethodName(String name) {
    if (null == name || name.isEmpty()) {
      return false;
    }
    boolean valid = true;
    for (int i = 0; i < name.length(); i++) {
      if (0 == i) {
        valid &= Character.isJavaIdentifierStart(name.charAt(i));
      } else {
        valid &= Character.isJavaIdentifierPart(name.charAt(i));
      }
    }
    return valid;
  }

  static String toValidMethodOrClassName(String name) {
    String firstSymbol = Character.toString(Character.isJavaIdentifierStart(name.charAt(0)) ? name.charAt(0) : Character.MIN_VALUE);
    StringBuilder sb = new StringBuilder(firstSymbol);
    for (int i = 1; i < name.length(); i++) {
      char ch = name.charAt(i);
      if (Character.isJavaIdentifierPart(ch)) {
        sb.append(ch);
      }
    }
    return sb.toString();
  }

  public void visit(MethodDeclaration methodDeclaration, ChangeContext changeContext) {
    if (changeContext.getMethodNames().contains(methodDeclaration.getName())) {
      changeContext.getMethodNames().remove(methodDeclaration.getName());
    } else {
      changeContext.addMethodDeclaration(methodDeclaration);
    }
  }

  private PackageDeclaration createPackageDeclaration(SourceFile sourceFile) {
    return new PackageDeclaration(new Name(sourceFile.getPackageName()));
  }

  private ClassOrInterfaceDeclaration getInterfaceName(SourceFile sourceFile) {
    ClassOrInterfaceDeclaration classOrInterfaceDeclaration = new ClassOrInterfaceDeclaration(of(PUBLIC), false, sourceFile.getClassName().toString());
    NodeList<MemberValuePair> memberValuePairs = new NodeList<>();
    memberValuePairs.add(new MemberValuePair("file", new StringLiteralExpr(sourceFile.getRelativePath().toString().replace(File.separator, "/"))));
    NodeList<AnnotationExpr> annotations = new NodeList<>();
    annotations.add(new NormalAnnotationExpr(new Name("Model"), memberValuePairs));
    classOrInterfaceDeclaration.setAnnotations(annotations);
    classOrInterfaceDeclaration.setInterface(true);
    return classOrInterfaceDeclaration;
  }
}
