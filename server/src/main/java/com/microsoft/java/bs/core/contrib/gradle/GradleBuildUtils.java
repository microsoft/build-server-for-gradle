package com.microsoft.java.bs.core.contrib.gradle;

import java.io.File;
import java.nio.file.Files;

import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;

import com.microsoft.java.bs.core.model.Preferences;

/**
 * Utilities for Gradle build.
 */
public class GradleBuildUtils {
  /**
   * The environment variable for Gradle home.
   */
  private static final String GRADLE_HOME = "GRADLE_HOME";

  /**
   * The environment variable for Gradle user home.
   */
  private static final String GRADLE_USER_HOME = "GRADLE_USER_HOME";

  private GradleBuildUtils() {}

  /**
   * Get the project connection for the given project.
   *
   * @param project The project.
   * @param preferences The preferences.
   */
  public static ProjectConnection getProjectConnection(File project, Preferences preferences) {
    GradleConnector connector = GradleConnector.newConnector()
        .forProjectDirectory(project);

    File gradleUserHome = getGradleUserHomeFile(preferences);
    if (gradleUserHome != null && gradleUserHome.exists()) {
      connector.useGradleUserHomeDir(gradleUserHome);
    }

    if (preferences.isGradleWrapperEnabled() && Files.exists(
        project.toPath().resolve("gradlew"))) {
      // TODO: need to validate the checksum
      connector.useBuildDistribution();
    } else if (preferences.getGradleVersion() != null) {
      connector.useGradleVersion(preferences.getGradleVersion());
    } else if (preferences.getGradleHome() != null) {
      File gradleHome = getGradleHome(preferences.getGradleHome());
      if (gradleHome != null && gradleHome.exists()) {
        connector.useInstallation(gradleHome);
      }
    }
    return connector.connect();
  }

  /**
   * Get the model builder for the given project connection.
   *
   * @param <T> The type of the model.
   * @param connection The project connection.
   * @param preferences The preferences.
   * @param clazz The class of the model.
   */
  public static <T> ModelBuilder<T> getModelBuilder(ProjectConnection connection,
      Preferences preferences, Class<T> clazz) {
    ModelBuilder<T> modelBuilder = connection.model(clazz);
    File gradleJavaHomeFile = getGradleJavaHomeFile(preferences);
    if (gradleJavaHomeFile != null && gradleJavaHomeFile.exists()) {
      modelBuilder.setJavaHome(gradleJavaHomeFile);
    }
    if (!preferences.getGradleJvmArguments().isEmpty()) {
      modelBuilder.addJvmArguments(preferences.getGradleJvmArguments());
    }
    if (!preferences.getGradleArguments().isEmpty()) {
      modelBuilder.addArguments(preferences.getGradleArguments());
    }
    return modelBuilder;
  }

  /**
   * Get the Build Launcher.
   *
   * @param connection The project connection.
   * @param preferences The preferences.
   */
  public static BuildLauncher getBuildLauncher(ProjectConnection connection,
      Preferences preferences) {
    BuildLauncher launcher = connection.newBuild();

    File gradleJavaHomeFile = getGradleJavaHomeFile(preferences);
    if (gradleJavaHomeFile != null && gradleJavaHomeFile.exists()) {
      launcher.setJavaHome(gradleJavaHomeFile);
    }
    if (!preferences.getGradleJvmArguments().isEmpty()) {
      launcher.addJvmArguments(preferences.getGradleJvmArguments());
    }
    if (!preferences.getGradleArguments().isEmpty()) {
      launcher.addArguments(preferences.getGradleArguments());
    }
    return launcher;
  }

  private static File getGradleUserHomeFile(Preferences preferences) {
    if (StringUtils.isNotBlank(preferences.getGradleUserHome())) {
      return new File(preferences.getGradleUserHome());
    } else {
      String gradleUserHome = System.getenv().get(GRADLE_USER_HOME);
      if (gradleUserHome == null) {
        gradleUserHome = System.getProperties().getProperty(GRADLE_USER_HOME);
      }
      if (gradleUserHome != null && !gradleUserHome.isEmpty()) {
        return new File(gradleUserHome);
      }
    }

    return null;
  }

  private static File getGradleHome(String gradleHome) {
    if (gradleHome != null && !gradleHome.isEmpty()) {
      return new File(gradleHome);
    }

    gradleHome = System.getenv().get(GRADLE_HOME);
    if (gradleHome == null || gradleHome.isEmpty()) {
      gradleHome = System.getProperties().getProperty(GRADLE_HOME);
    }
    if (gradleHome != null && !gradleHome.isEmpty()) {
      return new File(gradleHome);
    }

    return null;
  }

  private static File getGradleJavaHomeFile(Preferences preferences) {
    if (StringUtils.isNotBlank(preferences.getGradleJavaHome())) {
      File file = new File(preferences.getGradleJavaHome());
      if (file.isDirectory()) {
        return file;
      }
    }
    return null;
  }
}
