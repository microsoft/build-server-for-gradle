package com.microsoft.java.bs.core.internal.gradle;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;

/**
 * Gradle Tooling API utils.
 */
public class Utils {
  private Utils() {}

  /**
   * The file name of the init script.
   */
  private static final String INIT_GRADLE_SCRIPT = "init.gradle";

  /**
   * Get the Daemon connection for the project.
   *
   * @param projectUri The project uri.
   */ 
  public static ProjectConnection getProjectConnection(URI projectUri) {
    return getProjectConnection(new File(projectUri));
  }

  /**
   * Get the Daemon connection for the project.
   *
   * @param project The project.
   */
  public static ProjectConnection getProjectConnection(File project) {
    GradleConnector connector = GradleConnector.newConnector()
        .forProjectDirectory(project);
    return connector.connect();
  }

  /**
   * Get the model builder for the given project connection.
   *
   * @param <T> The type of the model.
   * @param connection The project connection.
   * @param clazz The class of the model.
   */
  public static <T> ModelBuilder<T> getModelBuilder(ProjectConnection connection,
      Class<T> clazz) {
    ModelBuilder<T> modelBuilder = connection.model(clazz);
    return modelBuilder;
  }

  public static File getInitScriptFile() {
    return Paths.get(System.getProperty("plugin.dir"), INIT_GRADLE_SCRIPT).toFile();
  }
}
