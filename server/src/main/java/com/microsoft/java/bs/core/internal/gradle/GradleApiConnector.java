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

import ch.epfl.scala.bsp4j.BuildClient;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.util.GradleVersion;

import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.model.GradleTestEntity;
import com.microsoft.java.bs.core.internal.reporter.CompileProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.DefaultProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.ProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.TaskProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.TestFinishReporter;
import com.microsoft.java.bs.core.internal.reporter.TestNameRecorder;
import com.microsoft.java.bs.core.internal.reporter.TestReportReporter;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.GradleTestTask;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSets;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.StatusCode;

/**
 * Connect to Gradle Daemon via Gradle Tooling API.
 */
public class GradleApiConnector {
  private final Map<File, GradleConnector> connectors;
  private final PreferenceManager preferenceManager;
  private BuildClient client;

  public GradleApiConnector(PreferenceManager preferenceManager) {
    this.preferenceManager = preferenceManager;
    connectors = new HashMap<>();
  }

  public void setClient(BuildClient client) {
    this.client = client;
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
   * @return an instance of {@link GradleSourceSets}
   */
  public GradleSourceSets getGradleSourceSets(URI projectUri) {
    File initScript = Utils.getInitScriptFile();
    if (!initScript.exists()) {
      throw new IllegalStateException("Failed to get init script file.");
    }
    TaskProgressReporter reporter = new TaskProgressReporter(new DefaultProgressReporter(client));
    String summary = "Source sets retrieved";
    StatusCode statusCode = StatusCode.OK;
    try (ProjectConnection connection = getGradleConnector(projectUri).connect()) {
      reporter.taskStarted("Connect to Gradle Daemon");
      ModelBuilder<GradleSourceSets> customModelBuilder = Utils.getModelBuilder(
          connection,
          preferenceManager.getPreferences(),
          GradleSourceSets.class
      );
      customModelBuilder.addProgressListener(reporter,
          OperationType.FILE_DOWNLOAD, OperationType.PROJECT_CONFIGURATION)
          .addArguments("--init-script", initScript.getAbsolutePath());
      if (Boolean.getBoolean("bsp.plugin.debug.enabled")) {
        customModelBuilder.addJvmArguments(
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
      }
      // since the model returned from Gradle TAPI is a wrapped object, here we re-construct it
      // via a copy constructor and return as a POJO.
      return new DefaultGradleSourceSets(customModelBuilder.get());
    } catch (GradleConnectionException | IllegalStateException e) {
      summary = e.getMessage();
      statusCode = StatusCode.ERROR;
      throw e;
    } finally {
      reporter.taskFinished(summary, statusCode);
    }
  }

  /**
   * Request Gradle daemon to run the tasks.
   */
  public StatusCode runTasks(URI projectUri, Set<BuildTargetIdentifier> btIds, String... tasks) {
    // Note: this might be anti-sepc, because the spec limits that one compile task report
    // can only have one build target id. While we aggregate all compile related tasks into one
    // Gradle call for the perf consideration. So, the build target id passed into the reporter
    // is not accurate.
    TaskProgressReporter reporter = new TaskProgressReporter(new CompileProgressReporter(
        btIds.iterator().next(), client));
    final ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
    String summary = "BUILD SUCCESSFUL";
    StatusCode statusCode = StatusCode.OK;
    try (ProjectConnection connection = getGradleConnector(projectUri).connect();
        errorOut;
    ) {
      reporter.taskStarted("Start to build: " + String.join(" ", tasks));
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
      summary = errorOut.toString();
      statusCode = StatusCode.ERROR;
    } finally {
      // If a build/taskStart notification has been sent,
      // the server must send build/taskFinish on completion of the same task.
      reporter.taskFinished(summary, statusCode);
    }

    return statusCode;
  }

  /**
   * request Gradle to return test classes.
   */
  public Map<BuildTargetIdentifier, List<GradleTestEntity>> getTestClasses(URI projectUri,
      Map<BuildTargetIdentifier, Set<GradleTestTask>> testTaskMap) {
 
    Map<BuildTargetIdentifier, List<GradleTestEntity>> results = new HashMap<>();
    DefaultProgressReporter reporter = new DefaultProgressReporter(client);

    reporter.taskStarted("Search for test classes");
    try (ProjectConnection connection = getGradleConnector(projectUri).connect()) {
      String gradleVersion = getGradleVersion(connection);
      // use --test-dry-run to discover tests.  Gradle version must be 8.3 or higher.
      if (GradleVersion.version(gradleVersion).compareTo(GradleVersion.version("8.3")) < 0) {
        reporter.taskFinished("Error searching for test classes: Gradle version "
            + gradleVersion + " must be >= 8.3", StatusCode.ERROR);
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
              DefaultProgressReporter taskReporter = new DefaultProgressReporter(client);
              taskReporter.taskStarted("Search for test classes in "
                  + gradleTestTask.getTaskPath());
              TestNameRecorder testNameRecorder = new TestNameRecorder();
              try {
                Utils.getTestLauncher(connection, preferenceManager.getPreferences())
                    .forTasks(gradleTestTask.getTaskPath())
                    .addArguments("--init-script", initScript.getAbsolutePath())
                    .addProgressListener(testNameRecorder, OperationType.TEST)
                    .run();
                taskReporter.taskFinished("Finished test classes search in " 
                    + gradleTestTask.getTaskPath(), StatusCode.OK);
              } catch (GradleConnectionException | IllegalStateException e) {
                String message = String.join("\n", ExceptionUtils.getRootCauseStackTraceList(e));
                taskReporter.taskFinished("Error searching for test classes in " 
                    + gradleTestTask.getTaskPath() + " "
                    + message, StatusCode.ERROR);
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
        reporter.taskFinished("Finished test classes search", StatusCode.OK);
      }
    } catch (GradleConnectionException | IllegalStateException e) {
      reporter.taskFinished("Error searching for test classes: "
          + e.getMessage(), StatusCode.ERROR);
    }

    return results;
  }

  /**
   * request Gradle to run test classes.
   */
  public StatusCode runTestClasses(URI projectUri,
      Map<BuildTargetIdentifier, Set<String>> testClassesMap) {

    StatusCode statusCode = StatusCode.OK;
    ProgressReporter reporter = new DefaultProgressReporter(client);
    reporter.taskStarted("Start tests");
    try (ProjectConnection connection = getGradleConnector(projectUri).connect()) {
      for (Map.Entry<BuildTargetIdentifier, Set<String>> entry : testClassesMap.entrySet()) {
        reporter.taskInProgress("Run tests for " + entry.getKey());
        TestReportReporter testReportReporter = new TestReportReporter(entry.getKey(), client);
        TestFinishReporter testFinishReporter = new TestFinishReporter(client);
        try {
          Utils.getTestLauncher(connection, preferenceManager.getPreferences())
              .withJvmTestClasses(entry.getValue())
              .addProgressListener(testReportReporter, OperationType.TEST)
              .addProgressListener(testFinishReporter, OperationType.TEST)
              .run();
        } catch (GradleConnectionException | IllegalStateException e) {
          testReportReporter.addException(e);
          statusCode = StatusCode.ERROR;
        } finally {
          testReportReporter.sendResult();
        }
      }
      reporter.taskFinished("Finished tests", StatusCode.OK);
    } catch (GradleConnectionException | IllegalStateException e) {
      reporter.taskFinished("Error running test classes: " + e.getMessage(), StatusCode.ERROR);
      statusCode = StatusCode.ERROR;
    }

    return statusCode;
  }

  public void shutdown() {
    connectors.values().forEach(GradleConnector::disconnect);
  }

  protected GradleConnector getGradleConnector(URI projectUri) {
    return getGradleConnector(new File(projectUri));
  }

  protected GradleConnector getGradleConnector(File project) {
    return connectors.computeIfAbsent(project,
        p -> Utils.getProjectConnector(p, preferenceManager.getPreferences()));
  }
}
