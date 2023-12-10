// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.gradle;

import static com.microsoft.java.bs.core.Launcher.LOGGER;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.gradle.tooling.BuildException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.model.build.BuildEnvironment;

import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.reporter.CompileProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.DefaultProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.TaskProgressReporter;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSets;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.StatusCode;

/**
 * Connect to Gradle Daemon via Gradle Tooling API.
 */
public class GradleApiConnector {
  private Map<File, GradleConnector> connectors;
  private PreferenceManager preferenceManager;

  public GradleApiConnector(PreferenceManager preferenceManager) {
    this.preferenceManager = preferenceManager;
    connectors = new HashMap<>();
  }

  /**
   * Get the Gradle version of the project.
   */
  public String getGradleVersion(URI projectUri) {
    try (ProjectConnection connection = getGradleConnector(projectUri).connect()) {
      BuildEnvironment model = connection
          .model(BuildEnvironment.class)
          .withArguments("--no-daemon")
          .get();
      return model.getGradle().getGradleVersion();
    } catch (BuildException e) {
      LOGGER.severe("Failed to get Gradle version: " + e.getMessage());
      return "";
    }
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
    DefaultProgressReporter defaultProgressReporter = new DefaultProgressReporter();
    TaskProgressReporter reporter = new TaskProgressReporter(defaultProgressReporter);
    String summary = "";
    StatusCode statusCode = StatusCode.OK;
    long startTime = System.currentTimeMillis();
    try (ProjectConnection connection = getGradleConnector(projectUri).connect()) {
      defaultProgressReporter.taskStarted(null, "Connect to Gradle Daemon", startTime);
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
      defaultProgressReporter.taskFinished(null, summary, startTime, statusCode);
      defaultProgressReporter.cleanUp();
    }
  }

  /**
   * Request Gradle daemon to run the tasks.
   */
  public StatusCode runTasks(String originId, URI projectUri, Set<String> tasks,
      Map<String, Set<BuildTargetIdentifier>> fullTaskPathMap) {
    // Don't issue a start progress update - the listener will pick that up automatically
    CompileProgressReporter compileReporter =
        new CompileProgressReporter(originId, fullTaskPathMap);

    final ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
    String summary = "BUILD SUCCESSFUL";
    StatusCode statusCode = StatusCode.OK;
    long startTime = System.currentTimeMillis();
    try (ProjectConnection connection = getGradleConnector(projectUri).connect();
        errorOut
    ) {
      TaskProgressReporter reporter = new TaskProgressReporter(compileReporter);
      String[] taskArr = tasks.stream().toArray(String[]::new);
      BuildLauncher launcher = Utils.getBuildLauncher(connection,
          preferenceManager.getPreferences());
      // TODO: consider to use outputstream to capture the output.
      launcher.addProgressListener(reporter, OperationType.TASK)
          .setStandardError(errorOut)
          .forTasks(taskArr)
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
      // The progress listener should send this but this covers Exceptions.
      for (String task : tasks) {
        compileReporter.taskFinished(task, summary, startTime, statusCode);
      }
      compileReporter.cleanUp();
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
