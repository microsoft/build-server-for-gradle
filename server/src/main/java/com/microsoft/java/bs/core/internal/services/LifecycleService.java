package com.microsoft.java.bs.core.internal.services;

import static com.microsoft.java.bs.core.Launcher.LOGGER;

import java.io.File;
import java.lang.Runtime.Version;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.java.bs.core.Constants;
import com.microsoft.java.bs.core.internal.gradle.GradleApiConnector;
import com.microsoft.java.bs.core.internal.gradle.GradleBuildKind;
import com.microsoft.java.bs.core.internal.gradle.Utils;
import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.model.Preferences;
import com.microsoft.java.bs.core.internal.utils.JsonUtils;
import com.microsoft.java.bs.core.internal.utils.UriUtils;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

import ch.epfl.scala.bsp4j.BuildServerCapabilities;
import ch.epfl.scala.bsp4j.CompileProvider;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.InitializeBuildResult;

/**
 * Lifecycle service.
 */
public class LifecycleService {

  private Status status = Status.UNINITIALIZED;

  private BuildTargetManager buildTargetManager;

  private PreferenceManager preferenceManager;

  public LifecycleService(BuildTargetManager buildTargetManager,
      PreferenceManager preferenceManager) {
    this.buildTargetManager = buildTargetManager;
    this.preferenceManager = preferenceManager;
  }

  /**
   * Initialize the build server.
   */
  public InitializeBuildResult initializeServer(InitializeBuildParams params) {
    initializePreferenceManager(params);
    updateBuildTargetManager();

    BuildServerCapabilities capabilities = initializeServerCapabilities();
    return new InitializeBuildResult(
        Constants.SERVER_NAME,
        Constants.SERVER_VERSION,
        Constants.BSP_VERSION,
        capabilities
    );
  }

  public Object reloadWorkspace() {
    updateBuildTargetManager();
    return null;
  }

  void initializePreferenceManager(InitializeBuildParams params) {
    URI rootUri = UriUtils.getUriFromString(params.getRootUri());
    preferenceManager.setRootUri(rootUri);

    Preferences preferences = JsonUtils.toModel(params.getData(), Preferences.class);
    if (preferences == null) {
      // If no preferences are provided, use an empty preferences.
      preferences = new Preferences();
    }

    updateGradleJavaHomeIfNecessary(rootUri, preferences);
    preferenceManager.setPreferences(preferences);
  }

  void updateBuildTargetManager() {
    GradleApiConnector gradleConnector = new GradleApiConnector(preferenceManager.getPreferences());
    GradleSourceSets sourceSets = gradleConnector.getGradleSourceSets(
          preferenceManager.getRootUri());
    buildTargetManager.store(sourceSets);
  }

  private BuildServerCapabilities initializeServerCapabilities() {
    BuildServerCapabilities capabilities = new BuildServerCapabilities();
    capabilities.setResourcesProvider(true);
    capabilities.setOutputPathsProvider(true);
    capabilities.setDependencyModulesProvider(true);
    capabilities.setCanReload(true);
    capabilities.setBuildTargetChangedProvider(true);
    capabilities.setCompileProvider(new CompileProvider(Arrays.asList("java")));
    return capabilities;
  }

  public void onBuildInitialized() {
    status = Status.INITIALIZED;
  }

  public Object shutdown() {
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
    SHUTDOWN;
  }

  /**
   * Try to update the Gradle Java home if:
   * <ul>
   *   <li>Gradle Java home is not set.</li>
   *   <li>A valid JDK can be found to launch Gradle.</li>
   * </ul>
   *
   * <p>The JDK installation path string will be set to {@link Preferences#gradleJavaHome}.
   */
  private void updateGradleJavaHomeIfNecessary(URI rootUri, Preferences preferences) {
    if (preferences.getJdks() == null || preferences.getJdks().isEmpty()) {
      return;
    }

    if (StringUtils.isBlank(preferences.getGradleJavaHome())) {
      String gradleVersion = "";
      GradleBuildKind buildKind = Utils.getEffectiveBuildKind(new File(rootUri), preferences);
      if (buildKind == GradleBuildKind.SPECIFIED_VERSION) {
        gradleVersion = preferences.getGradleVersion();
      } else {
        gradleVersion = Utils.getGradleVersion(rootUri);
      }

      if (StringUtils.isNotBlank(gradleVersion)) {
        String highestJavaVersion = Utils.getHighestCompatibleJavaVersion(gradleVersion);
        File jdkInstallation = getJdkToLaunchDaemon(preferences.getJdks(), highestJavaVersion);
        if (jdkInstallation != null) {
          preferences.setGradleJavaHome(jdkInstallation.getAbsolutePath());
        }
      }
    }
  }

  /**
   * Find the latest JDK but equal or lower than the {@code highestJavaVersion}.
   */
  static File getJdkToLaunchDaemon(Map<String, String> jdks, String highestJavaVersion) {
    if (StringUtils.isBlank(highestJavaVersion)) {
      return null;
    }

    Entry<String, String> selected = null;
    for (Entry<String, String> jdk : jdks.entrySet()) {
      String javaVersion = jdk.getKey();
      if (Version.parse(javaVersion).compareTo(Version.parse(highestJavaVersion)) <= 0
          && (selected == null || Version.parse(selected.getKey())
              .compareTo(Version.parse(javaVersion)) < 0)) {
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
