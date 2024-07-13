// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.gradle;


import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;
import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.ConfigurableLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.TestLauncher;
import org.gradle.util.GradleVersion;

import com.microsoft.java.bs.core.Launcher;
import com.microsoft.java.bs.core.internal.model.Preferences;

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
   * The environment variable for Gradle home.
   */
  private static final String GRADLE_HOME = "GRADLE_HOME";

  /**
   * The environment variable for Gradle user home.
   */
  private static final String GRADLE_USER_HOME = "GRADLE_USER_HOME";

  /**
   * Get the Gradle connector for the project.
   *
   * @param projectUri The project uri.
   */
  public static GradleConnector getProjectConnector(URI projectUri,
      Preferences preferences) {
    return getProjectConnector(new File(projectUri), preferences);
  }

  /**
   * Get the Gradle connector for the project.
   *
   * @param project The project.
   */
  public static GradleConnector getProjectConnector(File project, Preferences preferences) {
    GradleConnector connector = GradleConnector.newConnector()
        .forProjectDirectory(project);

    File gradleUserHome = getGradleUserHomeFile(preferences.getGradleUserHome());
    if (gradleUserHome != null && gradleUserHome.exists()) {
      connector.useGradleUserHomeDir(gradleUserHome);
    }

    switch (getEffectiveBuildKind(project, preferences)) {
      case SPECIFIED_VERSION:
        connector.useGradleVersion(preferences.getGradleVersion());
        break;
      case SPECIFIED_INSTALLATION:
        connector.useInstallation(getGradleHome(preferences.getGradleHome()));
        break;
      default:
        connector.useBuildDistribution();
        break;
    }

    return connector;
  }

  /**
   * Get the build action executer for the given project connection.
   *
   * @param <T> the result type
   * @param connection The project connection.
   * @param preferences The preferences.
   * @param action The build action.
   */
  public static <T> BuildActionExecuter<T> getBuildActionExecuter(ProjectConnection connection,
      Preferences preferences, BuildAction<T> action) {
    return setLauncherProperties(connection.action(action), preferences);
  }

  /**
   * Get the Build Launcher.
   *
   * @param connection The project connection.
   * @param preferences The preferences.
   */
  public static BuildLauncher getBuildLauncher(ProjectConnection connection,
      Preferences preferences) {
    return setLauncherProperties(connection.newBuild(), preferences);
  }

  /**
   * Get the Test Launcher.
   *
   * @param connection The project connection.
   * @param preferences The preferences.
   */
  public static TestLauncher getTestLauncher(ProjectConnection connection,
      Preferences preferences) {
    return setLauncherProperties(connection.newTestLauncher(), preferences);
  }

  /**
   * Set the Launcher properties.
   *
   * @param launcher The launcher.
   * @param preferences The preferences.
   */
  public static <T extends ConfigurableLauncher<T>> T setLauncherProperties(T launcher,
      Preferences preferences) {

    File gradleJavaHomeFile = getGradleJavaHomeFile(preferences.getGradleJavaHome());
    if (gradleJavaHomeFile != null && gradleJavaHomeFile.exists()) {
      launcher.setJavaHome(gradleJavaHomeFile);
    }

    List<String> gradleJvmArguments = preferences.getGradleJvmArguments();
    if (gradleJvmArguments != null && !gradleJvmArguments.isEmpty()) {
      launcher.setJvmArguments(gradleJvmArguments);
    }

    List<String> gradleArguments = preferences.getGradleArguments();
    if (gradleArguments != null && !gradleArguments.isEmpty()) {
      launcher.withArguments(gradleArguments);
    }
    return launcher;
  }

  /**
   * Get the latest compatible Java version for the current Gradle version, according
   * to <a href="https://docs.gradle.org/current/userguide/compatibility.html">
   * compatibility matrix</a>
   *
   * <p>If a compatible Java versions is not found, an empty string will be returned.
   */
  public static String getLatestCompatibleJavaVersion(String gradleVersion) {
    GradleVersion version = GradleVersion.version(gradleVersion);
    if (version.compareTo(GradleVersion.version("8.8")) >= 0) {
      return "22";
    } else if (version.compareTo(GradleVersion.version("8.5")) >= 0) {
      return "21";
    } else if (version.compareTo(GradleVersion.version("8.3")) >= 0) {
      return "20";
    } else if (version.compareTo(GradleVersion.version("7.6")) >= 0) {
      return "19";
    } else if (version.compareTo(GradleVersion.version("7.5")) >= 0) {
      return "18";
    } else if (version.compareTo(GradleVersion.version("7.3")) >= 0) {
      return "17";
    } else if (version.compareTo(GradleVersion.version("7.0")) >= 0) {
      return "16";
    } else if (version.compareTo(GradleVersion.version("6.7")) >= 0) {
      return "15";
    } else if (version.compareTo(GradleVersion.version("6.3")) >= 0) {
      return "14";
    } else if (version.compareTo(GradleVersion.version("6.0")) >= 0) {
      return "13";
    } else if (version.compareTo(GradleVersion.version("5.4")) >= 0) {
      return "12";
    } else if (version.compareTo(GradleVersion.version("5.0")) >= 0) {
      return "11";
    } else if (version.compareTo(GradleVersion.version("4.7")) >= 0) {
      return "10";
    } else if (version.compareTo(GradleVersion.version("4.3")) >= 0) {
      return "9";
    } else if (version.compareTo(GradleVersion.version("2.0")) >= 0) {
      return "1.8";
    }

    return "";
  }

  /**
   * Get the oldest compatible Java version for the current Gradle version.
   */
  public static String getOldestCompatibleJavaVersion() {
    return "1.8";
  }

  public static File getInitScriptFile() {
    return Paths.get(System.getProperty(Launcher.PROP_PLUGIN_DIR), INIT_GRADLE_SCRIPT).toFile();
  }

  static File getGradleUserHomeFile(String gradleUserHome) {
    if (StringUtils.isNotBlank(gradleUserHome)) {
      return new File(gradleUserHome);
    }

    return getFileFromEnvOrProperty(GRADLE_USER_HOME);
  }

  /**
   * Infer the Gradle Home.
   * TODO: simplify the method.
   */
  static File getGradleHome(String gradleHome) {
    File gradleHomeFolder = null;
    if (StringUtils.isNotBlank(gradleHome)) {
      gradleHomeFolder = new File(gradleHome);
    } else {
      // find if there is a gradle executable in PATH
      String path = System.getenv("PATH");
      if (StringUtils.isNotBlank(path)) {
        for (String p : path.split(File.pathSeparator)) {
          File gradle = new File(p, "gradle");
          if (gradle.exists() && gradle.isFile()) {
            File gradleBinFolder = gradle.getParentFile();
            if (gradleBinFolder != null && gradleBinFolder.isDirectory()
                && gradleBinFolder.getName().equals("bin")) {
              File gradleLibFolder = new File(gradleBinFolder.getParent(), "lib");
              if (gradleLibFolder.isDirectory()) {
                File[] files = gradleLibFolder.listFiles();
                if (files != null) {
                  Optional<File> gradleLauncherJar = Arrays.stream(files)
                      .filter(file -> file.isFile() && file.getName().startsWith("gradle-launcher-")
                          && file.getName().endsWith(".jar"))
                      .findFirst();
                  if (gradleLauncherJar.isPresent()) {
                    gradleHomeFolder = gradleBinFolder.getParentFile();
                    break;
                  }
                }
              }
            }
          }
        }
      }
    }

    if (gradleHomeFolder == null) {
      gradleHomeFolder = getFileFromEnvOrProperty(GRADLE_HOME);
    }

    if (gradleHomeFolder != null && gradleHomeFolder.isDirectory()) {
      return gradleHomeFolder;
    }

    return null;
  }

  /**
   * Get the path specified by the key from environment variables or system properties.
   * If the path is not empty, an <code>File</code> instance will be returned.
   * Otherwise, <code>null</code> will be returned.
   */
  static File getFileFromEnvOrProperty(String key) {
    String value = System.getenv().get(key);
    if (StringUtils.isBlank(value)) {
      value = System.getProperties().getProperty(key);
    }
    if (StringUtils.isNotBlank(value)) {
      return new File(value);
    }

    return null;
  }

  static File getGradleJavaHomeFile(String gradleJavaHome) {
    if (StringUtils.isNotBlank(gradleJavaHome)) {
      File file = new File(gradleJavaHome);
      if (file.isDirectory()) {
        return file;
      }
    }
    return null;
  }

  /**
   * Get the effective Gradle build kind according to the preferences.
   *
   * @param projectRoot Root path of the project.
   * @param preferences The preferences.
   */
  public static GradleBuildKind getEffectiveBuildKind(File projectRoot, Preferences preferences) {
    if (preferences.isWrapperEnabled()) {
      File wrapperProperties = Paths.get(projectRoot.getAbsolutePath(), "gradle", "wrapper",
          "gradle-wrapper.properties").toFile();
      if (wrapperProperties.exists()) {
        return GradleBuildKind.WRAPPER;
      }
    }

    if (StringUtils.isNotBlank(preferences.getGradleVersion())) {
      return GradleBuildKind.SPECIFIED_VERSION;
    }

    File gradleHome = getGradleHome(preferences.getGradleHome());
    if (gradleHome != null && gradleHome.exists()) {
      return GradleBuildKind.SPECIFIED_INSTALLATION;
    }

    return GradleBuildKind.TAPI;
  }
}
