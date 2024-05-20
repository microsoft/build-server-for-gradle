// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.gradle;

import static com.microsoft.java.bs.core.Launcher.LOGGER;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.TestLauncher;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.util.GradleVersion;

import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.model.GradleTestEntity;
import com.microsoft.java.bs.core.internal.reporter.AppRunReporter;
import com.microsoft.java.bs.core.internal.reporter.CompileProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.DefaultProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.ProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.TestNameRecorder;
import com.microsoft.java.bs.core.internal.reporter.TestReportReporter;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.GradleTestTask;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSets;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.StatusCode;

/**
 * Connect to Gradle Daemon via Gradle Tooling API.
 */
public class GradleApiConnector {
  private final Map<File, GradleConnector> connectors;
  private final PreferenceManager preferenceManager;

  public GradleApiConnector(PreferenceManager preferenceManager) {
    this.preferenceManager = preferenceManager;
    connectors = new HashMap<>();
  }

  /**
   * Get the Gradle version of the project.
   */
  public String getGradleVersion(URI projectUri) {
    try (ProjectConnection connection = getGradleConnector(projectUri).connect()) {
      return getGradleVersion(connection);
    } catch (BuildException e) {
      LOGGER.severe("Failed to get Gradle version: " + e.getMessage());
      return "";
    }
  }

  private String getGradleVersion(ProjectConnection connection) {
    BuildEnvironment model = connection
        .model(BuildEnvironment.class)
        .withArguments("--no-daemon")
        .get();
    return model.getGradle().getGradleVersion();
  }

  /**
   * Get the source sets of the Gradle project.
   *
   * @param projectUri uri of the project
   * @param client connection to BSP client
   * @return an instance of {@link GradleSourceSets}
   */
  public GradleSourceSets getGradleSourceSets(URI projectUri, BuildClient client) {
    File initScript = Utils.getInitScriptFile();
    if (!initScript.exists()) {
      throw new IllegalStateException("Failed to get init script file.");
    }
    ProgressReporter reporter = new DefaultProgressReporter(client);
    ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
    try (ProjectConnection connection = getGradleConnector(projectUri).connect();
        errorOut) {
      ModelBuilder<GradleSourceSets> customModelBuilder = Utils.getModelBuilder(
          connection,
          preferenceManager.getPreferences(),
          GradleSourceSets.class
      );
      customModelBuilder.addProgressListener(reporter,
          OperationType.FILE_DOWNLOAD, OperationType.PROJECT_CONFIGURATION)
          .setStandardError(errorOut)
          .addArguments("--init-script", initScript.getAbsolutePath());
      if (Boolean.getBoolean("bsp.plugin.debug.enabled")) {
        customModelBuilder.addJvmArguments(
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
      }
      customModelBuilder.addJvmArguments("-Dbsp.gradle.supportedLanguages="
          + String.join(",", preferenceManager.getClientSupportedLanguages()));
      // since the model returned from Gradle TAPI is a wrapped object, here we re-construct it
      // via a copy constructor and return as a POJO.
      return new DefaultGradleSourceSets(customModelBuilder.get());
    } catch (GradleConnectionException | IllegalStateException | IOException e) {
      String summary = e.getMessage();
      if (errorOut.size() > 0) {
        summary += "\n" + errorOut;
      }
      reporter.sendError(summary);
      throw new IllegalStateException("Error retrieving sourcesets \n" + summary, e);
    }
  }

  /**
   * Request Gradle daemon to run the tasks.
   *
   * @param projectUri uri of the project
   * @param reporter reporter on feedback from Gradle
   * @param tasks tasks to run
   */
  public StatusCode runTasks(URI projectUri, ProgressReporter reporter, String... tasks) {
    // Don't issue a start progress update - the listener will pick that up automatically
    final ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
    StatusCode statusCode = StatusCode.OK;
    try (ProjectConnection connection = getGradleConnector(projectUri).connect();
        errorOut
    ) {
      BuildLauncher launcher = Utils.getBuildLauncher(connection,
          preferenceManager.getPreferences());
      // TODO: consider to use outputstream to capture the output.
      launcher.addProgressListener(reporter, OperationType.TASK)
          .setStandardError(errorOut)
          .forTasks(tasks)
          .run();
    } catch (IOException e) {
      // caused by close the output stream, just simply log the error.
      LOGGER.severe(e.getMessage());
    } catch (BuildException e) {
      String summary = e.getMessage();
      if (errorOut.size() > 0) {
        summary += "\n" + errorOut;
      }
      reporter.sendError(summary);
      statusCode = StatusCode.ERROR;
    }

    return statusCode;
  }

  /**
   * request Gradle to return test classes.
   */
  public Map<BuildTargetIdentifier, List<GradleTestEntity>> getTestClasses(URI projectUri,
      Map<BuildTargetIdentifier, Set<GradleTestTask>> testTaskMap, BuildClient client,
      CompileProgressReporter compileProgressReporter) {
 
    Map<BuildTargetIdentifier, List<GradleTestEntity>> results = new HashMap<>();
    DefaultProgressReporter reporter = new DefaultProgressReporter(client);

    try (ProjectConnection connection = getGradleConnector(projectUri).connect()) {
      String gradleVersion = getGradleVersion(connection);
      // use --test-dry-run to discover tests.  Gradle version must be 8.3 or higher.
      if (GradleVersion.version(gradleVersion).compareTo(GradleVersion.version("8.3")) < 0) {
        reporter.sendError("Error searching for test classes: Gradle version "
            + gradleVersion + " must be >= 8.3");
      } else {
        for (Map.Entry<BuildTargetIdentifier, Set<GradleTestTask>> entry :
            testTaskMap.entrySet()) {
          List<GradleTestEntity> gradleTestEntities = new LinkedList<>();
          for (GradleTestTask gradleTestTask : entry.getValue()) {
            // can't pass arguments to tasks e.g. "--test-dry-run"
            // so manipulate test task using init script.
            File initScript = Utils.createInitScriptFile("gradle.projectsLoaded {"
                + "   rootProject {"
                + "    tasks.getByPath('" + gradleTestTask.getTaskPath() + "')?.setDryRun(true)"
                + "   }"
                + " }");
            try {
              TestNameRecorder testNameRecorder = new TestNameRecorder();
              try {

                TestLauncher launcher = Utils
                    .getTestLauncher(connection, preferenceManager.getPreferences())
                    .forTasks(gradleTestTask.getTaskPath())
                    .addArguments("--init-script", initScript.getAbsolutePath())
                    .addProgressListener(testNameRecorder, OperationType.TEST)
                    .addProgressListener(reporter, OperationType.TASK);
                if (compileProgressReporter != null) {
                  launcher.addProgressListener(compileProgressReporter, OperationType.TASK);
                }
                launcher.run();
              } catch (GradleConnectionException | IllegalStateException e) {
                String message = String.join("\n", ExceptionUtils.getRootCauseStackTraceList(e));
                reporter.sendError("Error searching for test classes in " 
                    + gradleTestTask.getTaskPath() + " " + message);
              }
              Set<String> mainClasses = testNameRecorder.getMainClasses();
              GradleTestEntity gradleTestEntity = new GradleTestEntity(gradleTestTask, mainClasses);
              gradleTestEntities.add(gradleTestEntity);
            } finally {
              initScript.delete();
            }
          }

          results.put(entry.getKey(), gradleTestEntities);
        }
      }
    } catch (GradleConnectionException | IllegalStateException e) {
      reporter.sendError("Error searching for test classes: " + e.getMessage());
    }

    return results;
  }

  /**
   * request Gradle to run test classes.
   */
  public StatusCode runTestClasses(URI projectUri,
      Map<BuildTargetIdentifier, Set<String>> testClassesMap,
      BuildClient client, String originId,
      CompileProgressReporter compileProgressReporter) {

    StatusCode statusCode = StatusCode.OK;
    ProgressReporter reporter = new DefaultProgressReporter(client);
    try (ProjectConnection connection = getGradleConnector(projectUri).connect()) {
      for (Map.Entry<BuildTargetIdentifier, Set<String>> entry : testClassesMap.entrySet()) {
        TestReportReporter testReportReporter = new TestReportReporter(entry.getKey(),
            client, originId);
        try {
          TestLauncher launcher = Utils
              .getTestLauncher(connection, preferenceManager.getPreferences())
              .withJvmTestClasses(entry.getValue())
              .addProgressListener(testReportReporter, OperationType.TEST);
          if (compileProgressReporter != null) {
            launcher.addProgressListener(compileProgressReporter, OperationType.TASK);
          }
          launcher.run();
        } catch (GradleConnectionException | IllegalStateException e) {
          testReportReporter.addException(e);
          statusCode = StatusCode.ERROR;
        } finally {
          testReportReporter.sendResult();
        }
      }
    } catch (GradleConnectionException | IllegalStateException e) {
      reporter.sendError("Error running test classes: " + e.getMessage());
      statusCode = StatusCode.ERROR;
    }

    return statusCode;
  }

  /**
   * request Gradle to run main class.
   */
  public StatusCode runMainClass(URI projectUri, String projectPath, String sourceSetName,
      String className, Map<String, String> environmentVariables, List<String> jvmOptions,
      List<String> arguments, BuildClient client,
      CompileProgressReporter compileProgressReporter) {

    StatusCode statusCode = StatusCode.OK;
    String taskName = "buildServerRunApp";
    ProgressReporter reporter = new AppRunReporter(client, taskName);
    try (ProjectConnection connection = getGradleConnector(projectUri).connect()) {
      String execTask = "gradle.projectsEvaluated {\n"
          + "  def proj = rootProject.findProject('" + projectPath + "')\n"
          + "  if (proj != null) {\n"
          + "    proj.getTasks().create('" + taskName + "', JavaExec.class, {\n"
          + "      classpath = proj.sourceSets." + sourceSetName + ".runtimeClasspath\n"
          + "      mainClass = '" + className + "'\n";
      if (arguments != null && !arguments.isEmpty()) {
        String args = arguments.stream().collect(Collectors.joining("','", "['", "']"));
        execTask += "      args = " + args + "\n";
      }
      if (environmentVariables != null && !environmentVariables.isEmpty()) {
        String envVars = environmentVariables.entrySet().stream()
            .map(entry -> "'" + entry.getKey() + "':'" + entry.getValue() + "'")
            .collect(Collectors.joining(",", "[", "]"));
        execTask += "      environment = " + envVars + "\n";
      }
      if (jvmOptions != null && !jvmOptions.isEmpty()) {
        String jvmArgs = jvmOptions.stream().collect(Collectors.joining("','", "['", "']"));
        execTask += "      jvmArgs = " + jvmArgs + "\n";
      }
      execTask += "    })\n"
           + "  }\n"
           + "}\n";
      File initScript = Utils.createInitScriptFile(execTask);
      try {
        BuildLauncher launcher = Utils
            .getBuildLauncher(connection, preferenceManager.getPreferences())
            .forTasks(taskName)
            .addArguments("--init-script", initScript.getAbsolutePath())
            .addProgressListener(reporter, OperationType.TASK);
        if (compileProgressReporter != null) {
          launcher.addProgressListener(compileProgressReporter, OperationType.TASK);
        }
        launcher.run();
      } finally {
        initScript.delete();
      }
    } catch (GradleConnectionException | IllegalStateException e) {
      String message = String.join("\n", ExceptionUtils.getRootCauseStackTraceList(e));
      reporter.sendError("Error running main class: " + message);
      statusCode = StatusCode.ERROR;
    }

    return statusCode;
  }

  public void shutdown() {
    connectors.values().forEach(GradleConnector::disconnect);
  }

  private GradleConnector getGradleConnector(URI projectUri) {
    return getGradleConnector(new File(projectUri));
  }

  private GradleConnector getGradleConnector(File project) {
    return connectors.computeIfAbsent(project,
        p -> Utils.getProjectConnector(p, preferenceManager.getPreferences()));
  }
}
