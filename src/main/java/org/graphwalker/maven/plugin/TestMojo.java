package org.graphwalker.maven.plugin;

/*
 * #%L
 * GraphWalker Maven Plugin
 * %%
 * Copyright (C) 2011 - 2014 GraphWalker
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.codehaus.plexus.util.StringUtils;
import org.graphwalker.core.machine.Context;
import org.graphwalker.core.machine.ExecutionStatus;
import org.graphwalker.core.machine.Machine;
import org.graphwalker.java.test.*;
import org.graphwalker.java.report.XMLReportGenerator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.*;

/**
 * @author Nils Olsson
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE, lifecycle = "graphwalker")
public final class TestMojo extends DefaultMojoBase {

    @Parameter(property = "project.testClasspathElements")
    private List<String> classpathElements;

    @Parameter(defaultValue="${project.build.testOutputDirectory}")
    private File testClassesDirectory;

    @Parameter(defaultValue="${project.build.outputDirectory}")
    private File classesDirectory;

    @Parameter(defaultValue = "${project.build.directory}/graphwalker-reports")
    private File reportsDirectory;

    @Parameter(property = "maven.test.skip", defaultValue="false")
    private boolean mavenTestSkip;

    @Parameter(property = "skipTests", defaultValue = "false")
    private boolean skipTests;

    @Parameter(property = "graphwalker.test.skip", defaultValue = "false")
    private boolean graphwalkerTestSkip;

    @Parameter(property = "graphwalker.includes")
    private Set<String> includes;

    @Parameter(property = "graphwalker.excludes")
    private Set<String> excludes;

    @Parameter(property = "graphwalker.test", defaultValue = "*")
    private String test;

    @Parameter(property = "graphwalker.groups", defaultValue = "*")
    private String groups;

    protected List<String> getClasspathElements() {
        return classpathElements;
    }

    protected File getTestClassesDirectory() {
        return testClassesDirectory;
    }

    protected File getClassesDirectory() {
        return classesDirectory;
    }

    protected File getReportsDirectory() {
        return reportsDirectory;
    }

    protected boolean getSkipTests() {
        return mavenTestSkip || graphwalkerTestSkip || skipTests;
    }

    protected Set<String> getIncludes() {
        return includes;
    }

    protected Set<String> getExcludes() {
        return excludes;
    }

    protected String getTest() {
        if (System.getProperties().containsKey("test")) {
            return System.getProperty("test");
        }
        return test;
    }

    protected String getGroups() {
        if (System.getProperties().containsKey("groups")) {
            return System.getProperty("groups");
        }
        return groups;
    }

    protected ClassLoader createClassLoader() throws MojoExecutionException {
        try {
            return new URLClassLoader(convertToURL(getClasspathElements()), getClass().getClassLoader());
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Couldn''t create class loader");
        }
    }

    private URL[] convertToURL(List<String> elements) throws MalformedURLException {
        List<URL> urlList = new ArrayList<>();
        for (String element : elements) {
            urlList.add(new File(element).toURI().toURL());
        }
        return urlList.toArray(new URL[urlList.size()]);
    }

    protected ClassLoader switchClassLoader(ClassLoader newClassLoader) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(newClassLoader);
        return oldClassLoader;
    }

    protected Properties createProperties() {
        Properties properties = (Properties) System.getProperties().clone();
        properties.putAll((Properties) getMavenProject().getProperties().clone());
        properties.put("groups", groups);
        properties.put("test", test);
        properties.putAll((Properties) getSession().getUserProperties().clone());
        return properties;
    }

    protected Properties switchProperties(Properties properties) {
        Properties oldProperties = (Properties) System.getProperties().clone();
        System.setProperties(properties);
        return oldProperties;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!getSkipTests()) {
            ClassLoader classLoader = switchClassLoader(createClassLoader());
            Properties properties = switchProperties(createProperties());
            displayHeader();
            Configuration configuration = createConfiguration();
            TestExecutor executor = new TestExecutor(configuration);
            displayConfiguration(configuration, executor);
            executor.execute();
            displayResult(executor);
            switchProperties(properties);
            switchClassLoader(classLoader);
            reportResults(executor);
        }
    }

    private void displayHeader() {
        if (getLog().isInfoEnabled()) {
            getLog().info("------------------------------------------------------------------------");
            getLog().info("  _____             _   _ _ _     _ _                                   ");
            getLog().info(" |   __|___ ___ ___| |_| | | |___| | |_ ___ ___                         ");
            getLog().info(" |  |  |  _| .'| . |   | | | | .'| | '_| -_|  _|                        ");
            getLog().info(" |_____|_| |__,|  _|_|_|_____|__,|_|_,_|___|_|                          ");
            getLog().info("               |_|         ("+getVersion()+")                            ");
            getLog().info("------------------------------------------------------------------------");
        }
    }

    private String getVersion() {
        Properties properties = new Properties();
        InputStream inputStream = getClass().getResourceAsStream("/version.properties");
        if (null != inputStream) {
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                return "unknown";
            }
        }
        return properties.getProperty("graphwalker.version");
    }

    private Configuration createConfiguration() {
        Configuration configuration = new Configuration();
        if (StringUtils.isBlank(getTest())) {
            configuration.addInclude(getIncludes());
            configuration.addExclude(getExcludes());
        } else {
            for (String test: getTest().split(",")) {
                test = test.trim();
                if (StringUtils.isNotBlank(test)) {
                    if (test.startsWith("!")) {
                        test = test.substring(1);
                        if (StringUtils.isNotBlank(test)) {
                            configuration.addExclude(test);
                        }
                    } else {
                        configuration.addInclude(test);
                    }
                }
            }
        }
        configuration.setClassesDirectory(getClassesDirectory());
        configuration.setTestClassesDirectory(getTestClassesDirectory());
        configuration.setReportsDirectory(getReportsDirectory());
        for (String group: getGroups().split(",")) {
            configuration.addGroup(group.trim());
        }
        return configuration;
    }

    private void displayConfiguration(Configuration configuration, TestExecutor executor) {
        if (getLog().isInfoEnabled()) {
            getLog().info("Configuration:");
            getLog().info("    Include = " + configuration.getIncludes());
            getLog().info("    Exclude = " + configuration.getExcludes());
            getLog().info("     Groups = " + configuration.getGroups());
            getLog().info("");
            getLog().info("Tests:");
            if (executor.getMachines().isEmpty()) {
                getLog().info("  No tests found");
            } else {
                for (Machine machine: executor.getMachines()) {
                    //getLog().info("  ["+"]");
                    for (Context context: machine.getContexts()) {
                        getLog().info("    "
                            + context.getClass().getSimpleName()+"("
                            + context.getPathGenerator().getClass().getSimpleName()+", "
                            + context.getPathGenerator().getStopCondition().getClass().getSimpleName()+", \""
                            /*+ context.getPathGenerator().getStopCondition().getValue()*/+"\")");
                    }
                    getLog().info("");
                }
            }
            getLog().info("------------------------------------------------------------------------");
        }
    }

    private void displayResult(TestExecutor executor) {
        if (getLog().isInfoEnabled()) {
            getLog().info("------------------------------------------------------------------------");
            getLog().info("");
            getLog().info("Result :");
            getLog().info("");

            long tests = 0, completed = 0, incomplete = 0, failed = 0, notExecuted = 0;
            List<Context> failedExecutions = new ArrayList<>();
            for (Machine machine: executor.getMachines()) {
                for (Context context: machine.getContexts()) {
                    tests++;
                    switch (context.getExecutionStatus()) {
                        case COMPLETED: {
                            completed++;
                        }
                        break;
                        case FAILED: {
                            failed++;
                            failedExecutions.add(context);
                        }
                        break;
                        case NOT_EXECUTED: {
                            notExecuted++;
                        }
                        break;
                        case EXECUTING: {
                            incomplete++;
                        }
                    }
                }
            }
            if (!failedExecutions.isEmpty()) {
                getLog().info("Failed executions: ");
                for (Context context: failedExecutions) {
                    double fulfilment = context.getPathGenerator().getStopCondition().getFulfilment(context);
                    String pathGenerator = context.getPathGenerator().getClass().getSimpleName();
                    String stopCondition = context.getPathGenerator().getStopCondition().getClass().getSimpleName();
                    getLog().info(MessageFormat.format("  {0}({1}, {2}): {3}%", context.getClass().getSimpleName(), pathGenerator, stopCondition, Math.round(100*fulfilment)));
                }
                getLog().info("");
            }
            getLog().info(MessageFormat.format("Tests: {0}, Completed: {1}, Incomplete: {2}, Failed: {3}, Not Executed: {4}", tests, completed, incomplete, failed, notExecuted));
            getLog().info("");
        }
    }

    private void reportResults(TestExecutor executor) throws MojoExecutionException {
        boolean hasExceptions = false;
        XMLReportGenerator reporter = new XMLReportGenerator(getReportsDirectory(), getSession().getStartTime(), getSession().getSystemProperties());

        reporter.writeReport(executor);

        for (Machine machine: executor.getMachines()) {
            for (Context context: machine.getContexts()) {
                hasExceptions |= ExecutionStatus.FAILED.equals(context.getExecutionStatus());
            }
        }
        if (hasExceptions) {
            throw new MojoExecutionException(MessageFormat.format("There are test failures.\n\n Please refer to {0} for the individual test results.", getReportsDirectory().getAbsolutePath()));
        }
    }
}
