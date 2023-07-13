package com.microsoft.java.bs.core.internal.services;

import java.net.URI;

import com.microsoft.java.bs.core.Constants;
import com.microsoft.java.bs.core.internal.gradle.GradleApiConnector;
import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

import ch.epfl.scala.bsp4j.BuildServerCapabilities;
import ch.epfl.scala.bsp4j.InitializeBuildResult;

/**
 * Lifecycle service.
 */
public class LifecycleService {

  private BuildTargetManager buildTargetManager;

  public LifecycleService(BuildTargetManager buildTargetManager) {
    this.buildTargetManager = buildTargetManager;
  }

  /**
   * Initialize the build server.
   */
  public InitializeBuildResult initializeServer(URI rootUri) {
    GradleApiConnector gradleConnector = new GradleApiConnector();
    GradleSourceSets sourceSets = gradleConnector.getGradleSourceSets(rootUri);
    buildTargetManager.store(sourceSets);
    BuildServerCapabilities capabilities = initializeServerCapabilities();
    return new InitializeBuildResult(
        Constants.SERVER_NAME,
        Constants.SERVER_VERSION,
        Constants.BSP_VERSION,
        capabilities
    );
  }

  private BuildServerCapabilities initializeServerCapabilities() {
    BuildServerCapabilities capabilities = new BuildServerCapabilities();
    // TODO: add more capabilities
    return capabilities;
  }
}