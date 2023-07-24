package com.microsoft.java.bs.core.internal.gradle;

import java.io.File;
import java.net.URI;

import org.gradle.tooling.BuildException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;

import com.microsoft.java.bs.core.internal.model.Preferences;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

import ch.epfl.scala.bsp4j.StatusCode;

/**
 * Connect to Gradle Daemon via Gradle Tooling API.
 */
public class GradleApiConnector {

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
  public StatusCode runTasks(URI projectUri, String... tasks) {
    try (ProjectConnection connection = Utils.getProjectConnection(projectUri, preferences)) {
      BuildLauncher launcher = Utils.getBuildLauncher(connection, preferences);
      launcher.forTasks(tasks);
      launcher.run();
    } catch (BuildException e) {
      // TODO: report the error to client via build server protocol
      return StatusCode.ERROR;
    }

    return StatusCode.OK;
  }
}
