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

import org.gradle.tooling.BuildException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.model.build.BuildEnvironment;

import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.reporter.DefaultProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.ProgressReporter;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSets;

import ch.epfl.scala.bsp4j.BuildClient;
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
      throw new IllegalStateException(e);
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
