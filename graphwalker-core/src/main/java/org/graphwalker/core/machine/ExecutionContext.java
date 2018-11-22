package org.graphwalker.core.machine;

/*
 * #%L
 * GraphWalker Core
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

import org.graphwalker.core.algorithm.Algorithm;
import org.graphwalker.core.generator.PathGenerator;
import org.graphwalker.core.model.Action;
import org.graphwalker.core.model.Argument;
import org.graphwalker.core.model.Builder;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Model;
import org.graphwalker.core.model.Requirement;
import org.graphwalker.core.statistics.Profiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import jdk.internal.dynalink.beans.StaticClass;

import static java.util.stream.Collectors.joining;
import static org.graphwalker.core.common.Objects.isNotNull;
import static org.graphwalker.core.model.Edge.RuntimeEdge;
import static org.graphwalker.core.model.Model.RuntimeModel;

/**
 * <h1>ExecutionContext</h1>
 * The ExecutionContext ties a model and a path generator together.
 * </p>
 * The context not only connects a model with a path generator, it also keeps track of
 * the execution of the model when traversing it, and it's history. Also, the model has an
 * internal code and data scoop, which the context also is responsible for running,
 * </p>
 *
 * @author Nils Olsson
 */
public abstract class ExecutionContext extends SimpleScriptContext implements Context {

  private static final Logger LOG = LoggerFactory.getLogger(ExecutionContext.class);

  private final static String DEFAULT_SCRIPT_LANGUAGE = "JavaScript";
  private static final Class[] NO_PARAMETERS = new Class[0];
  private ScriptEngine scriptEngine;

  private RuntimeModel model;
  private PathGenerator pathGenerator;
  private Profiler profiler;
  private ExecutionStatus executionStatus = ExecutionStatus.NOT_EXECUTED;
  private Element currentElement;
  private Element nextElement;
  private Element lastElement;

  private final Map<Class<? extends Algorithm>, Object> algorithms = new HashMap<>();

  private final Map<Requirement, RequirementStatus> requirements = new HashMap<>();

  public void wait(Callable<Boolean> condition) throws Exception {
    if (!condition.call()) {
      throw new VertexConditionException();
    }
  }

  public ExecutionContext() {
    ScriptEngine engine = getEngineByName();
    engine.setContext(this);
    String script = "var Callable = Java.type(\"java.util.concurrent.Callable\");";
    Compilable compiler = (Compilable) engine;
    Map<String, Object> groups = new HashMap<>(groups());
    groups.put(null, this);
    for (Map.Entry<String, Object> group : groups.entrySet()) {
      String groupName = group.getKey();
      Object groupImpl = group.getValue();
      String bindingName = groupName != null ? groupName : "impl";
      for (Method method : groupImpl.getClass().getMethods()) {
        String arguments = "";
        for (int i = 0; i < method.getParameterTypes().length; i++) {
          if (i > 0) {
            arguments += ",";
          }
          arguments += Character.toChars(65 + i)[0];
        }

        String functionName = getFunctionName(method.getName(), groupName);
        if (method.getName().startsWith("v_")) {
          script += "var " + functionName + "Callable = Java.extend(Callable, {" +
            "  call: function() {" +
            "    return " + bindingName + "." + method.getName() + "(" + arguments + ");" +
            "  }" +
            "});";
          script += "function " + functionName + "(" + arguments;
          script += ") { return impl.wait(new " + functionName + "Callable()); };";
        } else {
          script += "function " + functionName + "(" + arguments;
          script += ") { return " + bindingName + "." + method.getName() + "(" + arguments + ");};";
        }
      }
    }
    try {
      CompiledScript compiledScript = compiler.compile(script);
      Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
      bindings.put("impl", this);
      groups.remove(null);
      groups.forEach(bindings::put);
      compiledScript.eval(bindings);
      scriptEngine = compiledScript.getEngine();
    } catch (ScriptException e) {
      LOG.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  protected Map<String, Object> groups() {
    Map<String, Object> groups = new HashMap<>();
    String packageName = this.getClass().getPackage().getName();
    do {
      try {
        String fullyQualified = packageName.isEmpty()
          ? "ContextFactory"
          : packageName + ".ContextFactory";
        Class<?> contextFactory = Class.forName(fullyQualified);
        if (!contextFactory.isAssignableFrom(this.getClass())) {
          LOG.trace("Current test model does not use ContextFactory");
          break;
        }
        Method[] declaredMethods = contextFactory.getDeclaredMethods();
        if (declaredMethods.length == 0) {
          LOG.warn("Test model is represented by \"ContextFactory\" without factory methods");
        }
        for (Method method : declaredMethods) {
          Object suppliedInstance = method.invoke(this);
          groups.put(method.getReturnType().getSimpleName(), suppliedInstance);
        }
        break;
      } catch (ClassNotFoundException e) {
        // no ContextFactory in the package
      } catch (IllegalAccessException | InvocationTargetException e) {
        LOG.error("Can not execute method \"" + "\" of ContextFactory");
      }
      packageName = substringBeforeLast(packageName, '.');
    } while (packageName != null);

    return !groups.isEmpty() ? groups : Collections.singletonMap(null, this);
  }

  public String getFunctionName(String methodName, String groupName) {
    return groupName != null ? groupName + "$" + methodName : methodName;
  }

  private ScriptEngine getEngineByName() {
    ScriptEngine engine = new ScriptEngineManager(null).getEngineByName(DEFAULT_SCRIPT_LANGUAGE);
    if (null == engine) {
      throw new MachineException("Failed to create ScriptEngine");
    }
    return engine;
  }

  public ExecutionContext(Model model, PathGenerator pathGenerator) {
    this(model.build(), pathGenerator);
  }

  public ExecutionContext(RuntimeModel model, PathGenerator pathGenerator) {
    this();
    setModel(model);
    setPathGenerator(pathGenerator);
  }

  @Override
  public ScriptEngine getScriptEngine() {
    return scriptEngine;
  }

  @Override
  public RuntimeModel getModel() {
    return model;
  }

  @Override
  public Context setModel(RuntimeModel model) {
    this.model = model;
    addRequirements(model);
    return this;
  }

  private void addRequirements(RuntimeModel model) {
    requirements.clear();
    for (Requirement requirement : model.getRequirements()) {
      requirements.put(requirement, RequirementStatus.NOT_COVERED);
    }
    for (Element element : model.getElements()) {
      for (Requirement requirement : element.getRequirements()) {
        requirements.put(requirement, RequirementStatus.NOT_COVERED);
      }
    }
  }

  @Override
  public Profiler getProfiler() {
    return profiler;
  }

  @Override
  public Context setProfiler(Profiler profiler) {
    this.profiler = profiler;
    this.profiler.addContext(this);
    return this;
  }

  @Override
  public PathGenerator getPathGenerator() {
    return pathGenerator;
  }

  @Override
  public Context setPathGenerator(PathGenerator pathGenerator) {
    this.pathGenerator = pathGenerator;
    if (isNotNull(pathGenerator)) {
      this.pathGenerator.setContext(this);
    }
    return this;
  }

  @Override
  public ExecutionStatus getExecutionStatus() {
    return executionStatus;
  }

  @Override
  public Context setExecutionStatus(ExecutionStatus executionStatus) {
    this.executionStatus = executionStatus;
    return this;
  }

  @Override
  public Element getLastElement() {
    return lastElement;
  }

  @Override
  public Element getCurrentElement() {
    return currentElement;
  }

  @Override
  public Context setCurrentElement(Element element) {
    this.lastElement = this.currentElement;
    this.currentElement = element;
    this.nextElement = null;
    return this;
  }

  @Override
  public Element getNextElement() {
    return nextElement;
  }

  @Override
  public Context setNextElement(Builder<? extends Element> nextElement) {
    setNextElement(nextElement.build());
    return this;
  }

  @Override
  public Context setNextElement(Element nextElement) {
    this.nextElement = nextElement;
    this.currentElement = null;
    return this;
  }

  @Override
  public Context setRequirementStatus(Requirement requirement, RequirementStatus requirementStatus) {
    requirements.put(requirement, requirementStatus);
    return this;
  }

  @Override
  public List<Requirement> getRequirements() {
    return new ArrayList<>(requirements.keySet());
  }

  @Override
  public List<Requirement> getRequirements(RequirementStatus status) {
    List<Requirement> filteredRequirements = new ArrayList<>();
    for (Requirement requirement : requirements.keySet()) {
      if (status.equals(requirements.get(requirement))) {
        filteredRequirements.add(requirement);
      }
    }
    return filteredRequirements;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <A extends Algorithm> A getAlgorithm(Class<A> clazz) {
    if (!algorithms.containsKey(clazz)) {
      try {
        Constructor<? extends Algorithm> constructor = clazz.getConstructor(Context.class);
        algorithms.put(clazz, constructor.newInstance(this));
      } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
        LOG.error(e.getMessage());
        throw new MachineException(this, e);
      }
    }
    return (A) algorithms.get(clazz);
  }

  @Override
  public <E> List<E> filter(Collection<E> elements) {
    List<E> filteredElements = new ArrayList<>();
    if (isNotNull(elements)) {
      for (E element : elements) {
        if (element instanceof RuntimeEdge) {
          RuntimeEdge edge = (RuntimeEdge) element;
          if (isAvailable(edge)) {
            filteredElements.add(element);
          }
        } else {
          filteredElements.add(element);
        }
      }
    }
    return filteredElements;
  }

  @Override
  public boolean isAvailable(RuntimeEdge edge) {
    if (edge.hasGuard()) {
      LOG.debug("Execute: '{}' in model: '{}'", edge.getGuard().getScript(), getModel().getName());
      try {
        return (Boolean) getScriptEngine().eval(edge.getGuard().getScript());
      } catch (ScriptException e) {
        LOG.error(e.getMessage());
        throw new MachineException(this, e);
      }
    }
    return true;
  }

  @Override
  public void execute(Action action) {
    LOG.debug("Execute: '{}' in model: '{}'", action.getScript(), getModel().getName());
    try {
      getScriptEngine().eval(action.getScript());
    } catch (ScriptException e) {
      LOG.error(e.getMessage());
      throw new MachineException(this, e);
    }
  }

  @Override
  public void execute(String methodName, String groupName, List<Argument> arguments) {
    LOG.debug("Execute: '{}' in model: '{}'", methodName, getModel().getName());
    try {
      Object impl = this;
      String functionName = methodName;
      if (groups().containsKey(groupName)) {
        functionName = getFunctionName(methodName, groupName);
        impl = groups().get(groupName);
      }
      // provoke a NoSuchMethodException exception if the method doesn't exist
      Class[] parameterTypes = arguments != null
        ? arguments.stream().map(arg -> arg.getType().getTypeClass()).toArray(Class[]::new)
        : NO_PARAMETERS;
      impl.getClass().getMethod(methodName, parameterTypes);
      String commaSeparatedValues = arguments != null
        ? arguments.stream().map(Argument::getQuotedValue).collect(joining(",", "(", ")"))
        : "()";
      String implPrefix = methodName.startsWith("v_") && parameterTypes.length > 0 ? "impl." : "";
      getScriptEngine().eval(implPrefix + functionName + commaSeparatedValues);
    } catch (NoSuchMethodException e) {
      // ignore, method is not defined in the execution context
    } catch (Throwable t) {
      LOG.error(t.getMessage());
      throw new MachineException(this, t);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, String> getKeys() {
    Map<String, String> keys = new HashMap<>();
    List<String> methods = new ArrayList<>();
    for (Object impl : groups().values()) {
      for (Method method : impl.getClass().getMethods()) {
        methods.add(method.getName());
      }
    }
    if (getBindings(ENGINE_SCOPE).containsKey("nashorn.global")) {
      Map<String, Object> global = (Map<String, Object>) getBindings(ENGINE_SCOPE).get("nashorn.global");
      for (String key : global.keySet()) {
        if (isVariable(key, methods)) {
          if (global.get(key) instanceof Double) {
            keys.put(key, Long.toString(Math.round((double) global.get(key))));
          } else if (!(global.get(key) instanceof StaticClass)) {
            keys.put(key, global.get(key).toString());
          }
        }
      }
    } else {
      for (String key : getBindings(ENGINE_SCOPE).keySet()) {
        if (isVariable(key, methods)) {
          Object value = getBindings(ENGINE_SCOPE).get(key);
          if (value instanceof Double) {
            keys.put(key, Long.toString(Math.round((double) value)));
          } else {
            keys.put(key, value.toString());
          }
        }
      }
    }
    return keys;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getAttribute(String name) {
    if (getBindings(ENGINE_SCOPE).containsKey("nashorn.global")) {
      Map<String, Object> attributes = (Map<String, Object>) getBindings(ENGINE_SCOPE).get("nashorn.global");
      return attributes.get(name);
    } else {
      return super.getAttribute(name);
    }
  }

  @SuppressWarnings("unchecked")
  public void setAttribute(String name, Object value) {
    if (getBindings(ENGINE_SCOPE).containsKey("nashorn.global")) {
      Map<String, Object> attributes = (Map<String, Object>) getBindings(ENGINE_SCOPE).get("nashorn.global");
      attributes.put(name, value);
    } else {
      super.setAttribute(name, value, ENGINE_SCOPE);
    }
  }

  private boolean isVariable(String key, List<String> methods) {
    return !"impl".equals(key) && !methods.contains(key) && !"print".equals(key) && !"println".equals(key);
  }

  private String substringBeforeLast(String string, char delimiter) {
    int index = string.lastIndexOf(delimiter);
    return index == -1 ? null : string.substring(0, index);
  }
}
