// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.gradle;

import static com.microsoft.java.bs.core.Launcher.LOGGER;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSets;
import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.TestLauncher;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.util.GradleVersion;

import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.reporter.CompileProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.DefaultProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.ProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.TestReportReporter;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.actions.GetSourceSetsAction;

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
   * @param client     connection to BSP client
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
      BuildActionExecuter<GradleSourceSets> buildExecutor =
          Utils.getBuildActionExecuter(connection, preferenceManager.getPreferences(),
            new GetSourceSetsAction());
      buildExecutor.addProgressListener(reporter,
              OperationType.FILE_DOWNLOAD, OperationType.PROJECT_CONFIGURATION)
          .setStandardError(errorOut)
          .addArguments("--init-script", initScript.getAbsolutePath());
      if (Boolean.getBoolean("bsp.plugin.debug.enabled")) {
        buildExecutor.addJvmArguments(
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
      }
      buildExecutor.addJvmArguments("-Dbsp.gradle.supportedLanguages="
          + String.join(",", preferenceManager.getClientSupportedLanguages()));
      // since the model returned from Gradle TAPI is a wrapped object, here we re-construct it
      // via a copy constructor and return as a POJO.
      return new DefaultGradleSourceSets(buildExecutor.run());
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
   * @param reporter   reporter on feedback from Gradle
   * @param tasks      tasks to run
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
   * request Gradle to run tests.
   */
  public StatusCode runTests(URI projectUri,
      Map<BuildTargetIdentifier, Map<String, Set<String>>> testClassesMethodsMap,
      List<String> jvmOptions,
      List<String> args,
      Map<String, String> envVars,
      BuildClient client, String originId,
      CompileProgressReporter compileProgressReporter) {

    StatusCode statusCode = StatusCode.OK;
    ProgressReporter reporter = new DefaultProgressReporter(client);
    try (ProjectConnection connection = getGradleConnector(projectUri).connect()) {
      String gradleVersion = getGradleVersion(connection);
      if (GradleVersion.version(gradleVersion).compareTo(GradleVersion.version("2.6")) < 0) {
        reporter.sendError("Error running test classes: Gradle version "
            + gradleVersion + " must be >= 2.6");
      } else if (envVars != null && !envVars.isEmpty()
          && GradleVersion.version(gradleVersion).compareTo(GradleVersion.version("3.5")) < 0) {
        reporter.sendError("Error running test classes With Environment Variables: Gradle version "
            + gradleVersion + " must be >= 3.5");
      } else {
        for (Map.Entry<BuildTargetIdentifier, Map<String, Set<String>>> entry :
            testClassesMethodsMap.entrySet()) {
          TestReportReporter testReportReporter = new TestReportReporter(entry.getKey(),
              client, originId);
          final ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
          try (errorOut) {
            TestLauncher launcher = Utils
                .getTestLauncher(connection, preferenceManager.getPreferences())
                .setStandardError(errorOut)
                .addProgressListener(testReportReporter, OperationType.TEST);
            if (compileProgressReporter != null) {
              launcher.addProgressListener(compileProgressReporter, OperationType.TASK);
            }
            for (Map.Entry<String, Set<String>> classesMethods : entry.getValue().entrySet()) {
              if (classesMethods.getValue() != null && !classesMethods.getValue().isEmpty()) {
                launcher.withJvmTestMethods(classesMethods.getKey() + '*',
                    classesMethods.getValue());
              } else {
                launcher.withJvmTestClasses(classesMethods.getKey() + '*');
              }
            }
            launcher.withArguments(args);
            launcher.setJvmArguments(jvmOptions);
            // env vars requires Gradle >= 3.5
            if (envVars != null) {
              // Running Gradle tests on Windows seems to require the `SystemRoot` env var
              // Otherwise Windows complains "Unrecognized Windows Sockets error: 10106"
              // Assumption is that current env vars plus specified env vars are all wanted.
              Map<String, String> allEnvVars = new HashMap<>(System.getenv());
              allEnvVars.putAll(envVars);
              launcher.setEnvironmentVariables(allEnvVars);
            }
            launcher.run();
          } catch (IOException e) {
            // caused by close the output stream, just simply log the error.
            LOGGER.severe(e.getMessage());
          } catch (GradleConnectionException | IllegalStateException e) {
            String message = String.join("\n", ExceptionUtils.getRootCauseStackTraceList(e));
            if (errorOut.size() > 0) {
              message = message + '\n' + errorOut;
            }
            testReportReporter.addException(message);
            statusCode = StatusCode.ERROR;
          } finally {
            testReportReporter.sendResult();
          }
        }
      }
    } catch (GradleConnectionException | IllegalStateException e) {
      reporter.sendError("Error running test classes: " + e.getMessage());
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
