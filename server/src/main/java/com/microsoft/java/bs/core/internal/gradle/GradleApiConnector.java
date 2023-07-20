package com.microsoft.java.bs.core.internal.gradle;

import java.io.File;
import java.net.URI;

import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;

import com.microsoft.java.bs.gradle.model.GradleSourceSets;

/**
 * Connect to Gradle Daemon via Gradle Tooling API.
 */
public class GradleApiConnector {

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
    try (ProjectConnection connection = Utils.getProjectConnection(projectUri)) {
      ModelBuilder<GradleSourceSets> customModelBuilder = Utils.getModelBuilder(
          connection,
          GradleSourceSets.class
      );
      customModelBuilder.addArguments("--init-script", initScript.getAbsolutePath());
      return customModelBuilder.get();
    } catch (GradleConnectionException | IllegalStateException e) {
      // TODO: report the error to client via build server protocol
      throw e;
    }
  }
}
