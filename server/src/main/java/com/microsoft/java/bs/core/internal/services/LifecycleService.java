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

import org.apache.commons.lang3.StringUtils;

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

    File jdk = getSuitableJdk(rootUri);
    if (jdk != null) {
      preferences.setGradleJavaHome(jdk.getAbsolutePath());
    }
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
   * Finds a suitable version of JDK to use for gradle operations.
   * Returns {@code null} if no suitable JDK can be found.
   */
  private File getSuitableJdk(URI rootUri) {

    Preferences preferences = preferenceManager.getPreferences();
    String filePath = preferences.getGradleJavaHome();
    File gradleJavaHome;
    if (StringUtils.isNotEmpty(filePath)) {
      gradleJavaHome = new File(filePath);
    } else {
      gradleJavaHome = connector.getGradleJavaHome(rootUri);
    }

    GradleBuildKind buildKind = Utils.getEffectiveBuildKind(new File(rootUri), preferences);
    Map<String, String> map = TelemetryUtils.getMetadataMap("buildKind", buildKind.name());
    LOGGER.log(Level.INFO, "Use build kind: " + buildKind.name(), map);

    String gradleVersion;
    if (buildKind == GradleBuildKind.SPECIFIED_VERSION) {
      gradleVersion = preferences.getGradleVersion();
    } else {
      gradleVersion = connector.getGradleVersion(rootUri);
    }

    if (StringUtils.isEmpty(gradleVersion)) {
      return null;
    }

    map = TelemetryUtils.getMetadataMap("gradleVersion", gradleVersion);
    LOGGER.log(Level.INFO, "Gradle version: " + gradleVersion, map);

    String latestCompatibleVersion = Utils.getLatestCompatibleJavaVersion(gradleVersion);
    String oldestCompatibleVersion = Utils.getOldestCompatibleJavaVersion();

    if (StringUtils.isNotBlank(latestCompatibleVersion)) {

      // Use GradleJavaHome if compatible
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
        } catch (IOException e) {
          LOGGER.severe("Invalid GradleJavaHome: " + e.getMessage());
        }

      }

      // Pick a compatible JDK from the JDKs available in Preferences
      if (preferences.getJdks() != null && preferences.getJdks().isEmpty()) {
        File selectedJdk = getLatestCompatibleJdk(
            preferences.getJdks(),
            oldestCompatibleVersion,
            latestCompatibleVersion
        );
        if (selectedJdk != null) {
          return selectedJdk;
        }
      }

    }

    // Notify client, for no compatible JDK can be found
    if (client != null) {
      ShowMessageParams messageParams = new ShowMessageParams(
          MessageType.ERROR,
          "Failed to find a JDK compatible with current gradle version "
              + "(" + gradleVersion + "). Compatible JDK versions include ("
              + oldestCompatibleVersion + " - " + latestCompatibleVersion + ")"
      );
      client.onBuildShowMessage(messageParams);
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
      Boolean isSelectedVersionHigher =
          selected == null
          || Version.parse(selected.getKey()).feature()
          < Version.parse(javaVersion).feature();
      if (
          JavaUtils.isCompatible(
              javaVersion,
              oldestCompatibleJavaVersion,
              latestCompatibleJavaVersion
          ) && isSelectedVersionHigher
      ) {
        selected = jdk;
      }
    }

    if (selected == null) {
      return null;
    }

    try {
      return new File(new URI(selected.getValue()));
    } catch (URISyntaxException e) {
      LOGGER.severe("Invalid JDK URI: " + selected.getValue());
      return null;
    }

  }

}
