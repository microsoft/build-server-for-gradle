package com.microsoft.java.bs.core.internal.services;

import java.net.URI;

import com.microsoft.java.bs.core.Constants;
import com.microsoft.java.bs.core.internal.gradle.GradleApiConnector;
import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.model.Preferences;
import com.microsoft.java.bs.core.internal.utils.JsonUtils;
import com.microsoft.java.bs.core.internal.utils.UriUtils;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

import ch.epfl.scala.bsp4j.BuildServerCapabilities;
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
    Preferences preferences = JsonUtils.toModel(params.getData(), Preferences.class);
    if (preferences == null) {
      // If no preferences are provided, use an empty preferences.
      preferences = new Preferences();
    }
    preferenceManager.setPreferences(preferences);

    URI rootUri = UriUtils.getUriFromString(params.getRootUri());
    preferenceManager.setRootUri(rootUri);
  }

  void updateBuildTargetManager() {
    GradleApiConnector gradleConnector = new GradleApiConnector(preferenceManager.getPreferences());
    GradleSourceSets sourceSets = gradleConnector.getGradleSourceSets(
          preferenceManager.getRootUri());
    buildTargetManager.store(sourceSets);
  }

  private BuildServerCapabilities initializeServerCapabilities() {
    BuildServerCapabilities capabilities = new BuildServerCapabilities();
    // TODO: add more capabilities
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
}
