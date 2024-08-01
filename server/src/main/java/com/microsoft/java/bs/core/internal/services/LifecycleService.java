// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.services;

import static com.microsoft.java.bs.core.Launcher.LOGGER;

import java.io.File;
import java.io.IOException;
import java.lang.Runtime.Version;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.microsoft.java.bs.core.Constants;
import com.microsoft.java.bs.core.internal.gradle.GradleApiConnector;
import com.microsoft.java.bs.core.internal.gradle.GradleBuildKind;
import com.microsoft.java.bs.core.internal.gradle.Utils;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.model.Preferences;
import com.microsoft.java.bs.core.internal.utils.JsonUtils;
import com.microsoft.java.bs.core.internal.utils.JavaUtils;
import com.microsoft.java.bs.core.internal.utils.TelemetryUtils;
import com.microsoft.java.bs.core.internal.utils.UriUtils;
import com.microsoft.java.bs.gradle.model.SupportedLanguages;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildServerCapabilities;
import ch.epfl.scala.bsp4j.CompileProvider;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.InitializeBuildResult;
import ch.epfl.scala.bsp4j.MessageType;
import ch.epfl.scala.bsp4j.ShowMessageParams;
import org.gradle.tooling.model.build.BuildEnvironment;

/**
 * Lifecycle service.
 */
public class LifecycleService {

  private Status status = Status.UNINITIALIZED;

  private final GradleApiConnector connector;

  private final PreferenceManager preferenceManager;

  private BuildClient client;

  /**
   * Constructor for {@link LifecycleService}.
   */
  public LifecycleService(GradleApiConnector connector, PreferenceManager preferenceManager) {
    this.connector = connector;
    this.preferenceManager = preferenceManager;
  }

  /**
   * Initialize the build server.
   */
  public InitializeBuildResult initializeServer(InitializeBuildParams params) {
    initializePreferenceManager(params);

    BuildServerCapabilities capabilities = initializeServerCapabilities();
    return new InitializeBuildResult(
        Constants.SERVER_NAME,
        Constants.SERVER_VERSION,
        Constants.BSP_VERSION,
        capabilities
    );
  }

  public void setClient(BuildClient client) {
    this.client = client;
  }

  void initializePreferenceManager(InitializeBuildParams params) {
    URI rootUri = UriUtils.getUriFromString(params.getRootUri());
    preferenceManager.setRootUri(rootUri);
    preferenceManager.setClientSupportedLanguages(params.getCapabilities().getLanguageIds());

    Preferences preferences = JsonUtils.toModel(params.getData(), Preferences.class);
    if (preferences == null) {
      // If no preferences are provided, use an empty preferences.
      preferences = new Preferences();
    }

    preferenceManager.setPreferences(preferences);

    setGradleJavaHome(rootUri);

  }

  private BuildServerCapabilities initializeServerCapabilities() {
    BuildServerCapabilities capabilities = new BuildServerCapabilities();
    capabilities.setResourcesProvider(true);
    capabilities.setOutputPathsProvider(true);
    capabilities.setDependencyModulesProvider(true);
    capabilities.setDependencySourcesProvider(true);
    capabilities.setCanReload(true);
    capabilities.setBuildTargetChangedProvider(true);
    capabilities.setCompileProvider(new CompileProvider(SupportedLanguages.allBspNames));
    return capabilities;
  }

  public void onBuildInitialized() {
    status = Status.INITIALIZED;
  }

  /**
   * Shutdown all Gradle connectors and mark the server status to shutdown.
   */
  public Object shutdown() {
    connector.shutdown();
    status = Status.SHUTDOWN;
    return null;
  }

  /**
   * Exit build server.
   */
  public void exit() {
    if (status == Status.SHUTDOWN) {
      System.exit(0);
    }

    System.exit(1);
  }

  enum Status {
    UNINITIALIZED,
    INITIALIZED,
    SHUTDOWN
  }

  /**
   * Finds and stores the compatible JDK path for executing gradle operations.
   */
  private void setGradleJavaHome(URI rootUri) {

    boolean isCompatible = connector.checkCompatibilityWithProbeBuild(rootUri);
    if (isCompatible) {
      // Default configuration is compatible, no need for extra work
      return;
    }

    BuildEnvironment buildEnv = connector.getBuildEnvironment(rootUri);

    String gradleVersion = getGradleVersion(rootUri, buildEnv);
    if (gradleVersion == null) {
      LOGGER.severe("Failed to find current gradle version.");
      return;
    }

    // Get gradle compatible jdk versions
    String latestCompatibleVersion = Utils.getLatestCompatibleJavaVersion(gradleVersion);
    String oldestCompatibleVersion = Utils.getOldestCompatibleJavaVersion();

    // Get compatible jdk
    File jdk = getGradleCompatibleJdk(buildEnv, latestCompatibleVersion, oldestCompatibleVersion);

    if (jdk != null) {
      preferenceManager.getPreferences().setGradleJavaHome(jdk.getAbsolutePath());
      if (client != null) {
        ShowMessageParams messageParams = new ShowMessageParams(
            MessageType.INFORMATION,
            String.format(
                "Default JDK wasn't compatible with current gradle version (" + gradleVersion + ")."
                + "Using \"%s\" instead.", jdk.getAbsolutePath()
            )
        );
        client.onBuildShowMessage(messageParams);
      }
    } else {
      if (client != null) {
        ShowMessageParams messageParams = new ShowMessageParams(
            MessageType.ERROR,
            "Failed to find a JDK compatible with current gradle version (" + gradleVersion + ")."
        );
        client.onBuildShowMessage(messageParams);
      }
    }

  }

  /**
   * Finds the gradle version for the given project.
   * Returns {@code null} if failed to determine the gradle version.
   */
  private String getGradleVersion(URI rootUri, BuildEnvironment buildEnv) {

    Preferences preferences = preferenceManager.getPreferences();
    GradleBuildKind buildKind = Utils.getEffectiveBuildKind(new File(rootUri), preferences);

    // Send telemetry data for build kind
    Map<String, String> map = TelemetryUtils.getMetadataMap("buildKind", buildKind.name());
    LOGGER.log(Level.INFO, "Use build kind: " + buildKind.name(), map);

    // Determine gradle version
    String gradleVersion = buildKind.equals(GradleBuildKind.SPECIFIED_VERSION)
        ? preferences.getGradleVersion()
        : (buildEnv != null ? buildEnv.getGradle().getGradleVersion() : null);

    // Send telemetry data for gradle version
    if (gradleVersion != null) {
      map = TelemetryUtils.getMetadataMap("gradleVersion", gradleVersion);
      LOGGER.log(Level.INFO, "Gradle version: " + gradleVersion, map);
    }

    return gradleVersion;

  }

  /**
   * Finds a compatible JDK version.
   * Returns {@code null} if no compatible JDK can be found.
   */
  private File getGradleCompatibleJdk(
      BuildEnvironment buildEnv,
      String latestCompatibleVersion,
      String oldestCompatibleVersion
  ) {

    Preferences preferences = preferenceManager.getPreferences();
    String preferencesGradleJavaHome = preferences.getGradleJavaHome();

    // Determine gradle java home
    File gradleJavaHome = preferencesGradleJavaHome != null
        ? new File(preferencesGradleJavaHome)
        : (buildEnv != null ? buildEnv.getJava().getJavaHome() : null);

    if (latestCompatibleVersion.isEmpty()) {
      return null;
    }

    // Prefer gradle java home
    if (gradleJavaHome != null) {
      try {
        String gradleJavaHomeVersion = JavaUtils.getJavaVersionFromFile(gradleJavaHome);
        if (
            JavaUtils.isCompatible(
                gradleJavaHomeVersion,
                oldestCompatibleVersion,
                latestCompatibleVersion
            )
        ) {
          return gradleJavaHome;
        }
      } catch (IOException | IllegalArgumentException e) {
        LOGGER.severe("Invalid GradleJavaHome: " + e.getMessage());
      }
    }

    // Fallback to user java home
    Map<String, String> userJdks = preferences.getJdks();
    if (userJdks != null && !userJdks.isEmpty()) {
      return getLatestCompatibleJdk(
          preferences.getJdks(),
          oldestCompatibleVersion,
          latestCompatibleVersion
      );
    }

    return null;

  }

  /**
   * Finds the latest version of JDK from the given map of jdks
   * between {@code oldestCompatibleJavaVersion} and {@code latestCompatibleJavaVersion}.
   */
  static File getLatestCompatibleJdk(
      Map<String, String> jdks,
      String oldestCompatibleJavaVersion,
      String latestCompatibleJavaVersion
  ) {

    Entry<String, String> selected = null;
    for (Entry<String, String> jdk : jdks.entrySet()) {

      String javaVersion = jdk.getKey();
      boolean isHigherThanSelected = selected == null
          || Version.parse(selected.getKey()).feature() < Version.parse(javaVersion).feature();

      try {
        if (
            JavaUtils.isCompatible(
                javaVersion,
                oldestCompatibleJavaVersion,
                latestCompatibleJavaVersion
            ) && isHigherThanSelected
        ) {
          selected = jdk;
        }
      } catch (IllegalArgumentException e) {
        LOGGER.severe("Invalid JDK version: " + e.getMessage());
      }

    }

    if (selected != null) {
      try {
        return new File(new URI(selected.getValue()));
      } catch (URISyntaxException e) {
        LOGGER.severe("Invalid JDK URI: " + selected.getValue());
      }
    }

    return null;

  }

}
