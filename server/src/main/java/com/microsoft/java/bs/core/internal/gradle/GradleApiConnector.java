package com.microsoft.java.bs.core.internal.gradle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

import org.gradle.tooling.BuildException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.OperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.java.bs.core.internal.model.Preferences;
import com.microsoft.java.bs.core.internal.reporter.CompileProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.TaskProgressReporter;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.StatusCode;

/**
 * Connect to Gradle Daemon via Gradle Tooling API.
 */
public class GradleApiConnector {

  private static final Logger logger = LoggerFactory.getLogger(GradleApiConnector.class);

  Preferences preferences;

  public GradleApiConnector(Preferences preferences) {
    this.preferences = preferences;
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
    try (ProjectConnection connection = Utils.getProjectConnection(projectUri, preferences)) {
      ModelBuilder<GradleSourceSets> customModelBuilder = Utils.getModelBuilder(
          connection,
          preferences,
          GradleSourceSets.class
      );
      customModelBuilder.addArguments("--init-script", initScript.getAbsolutePath());
      return customModelBuilder.get();
    } catch (GradleConnectionException | IllegalStateException e) {
      // TODO: report the error to client via build server protocol
      throw e;
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
    try (ProjectConnection connection = Utils.getProjectConnection(projectUri, preferences);
        errorOut;
    ) {
      reporter.taskStarted("Start to build: " + String.join(" ", tasks));
      BuildLauncher launcher = Utils.getBuildLauncher(connection, preferences);
      // TODO: consider to use outputstream to capture the output.
      launcher.addProgressListener(reporter, OperationType.TASK)
          .setStandardError(errorOut)
          .forTasks(tasks)
          .run();
      reporter.taskFinished("BUILD SUCCESSFUL", StatusCode.OK);
    } catch (IOException e) {
      // caused by close the output stream, just simply log the error.
      logger.error(e.getMessage(), e);
    } catch (BuildException e) {
      reporter.taskFinished(errorOut.toString(), StatusCode.ERROR);
      return StatusCode.ERROR;
    }

    return StatusCode.OK;
  }
}
