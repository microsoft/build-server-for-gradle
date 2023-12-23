// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.gradle;

import static com.microsoft.java.bs.core.Launcher.LOGGER;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import com.microsoft.java.bs.gradle.model.GradleIncludedBuild;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
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
  
  private List<GradleSourceSet> getIncludedBuildGradleSourceSets(File initScript, URI projectUri) {
    // add source sets for this project
    List<GradleSourceSet> allSourceSets = new ArrayList<>();
    GradleSourceSets gradleSourceSets = getGradleSourceSets(initScript, projectUri);
    allSourceSets.addAll(gradleSourceSets.getGradleSourceSets());
    // check included builds for more source sets to add
    for (GradleIncludedBuild includedBuild : gradleSourceSets.getGradleIncludedBuilds()) {
      URI includedProjectUri = includedBuild.getProjectDir().toURI();
      allSourceSets.addAll(getIncludedBuildGradleSourceSets(initScript, includedProjectUri));
    }
    return allSourceSets;
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
    List<GradleSourceSet> allSourceSets = getIncludedBuildGradleSourceSets(initScript, projectUri);

    return new DefaultGradleSourceSets(Collections.emptyList(), allSourceSets);
  }

  private GradleSourceSets getGradleSourceSets(File initScript, URI projectUri) {
    TaskProgressReporter reporter = new TaskProgressReporter(new DefaultProgressReporter());
    String summary = "";
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
        btIds.iterator().next()));
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
