package com.microsoft.java.bs.core.internal.services;

import java.net.URI;
import java.util.Arrays;

import com.microsoft.java.bs.core.internal.gradle.GradleConnector;
import com.microsoft.java.bs.core.internal.managers.BuildTargetsManager;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

import ch.epfl.scala.bsp4j.BuildServerCapabilities;
import ch.epfl.scala.bsp4j.CompileProvider;
import ch.epfl.scala.bsp4j.InitializeBuildResult;

/**
 * Lifecycle service.
 */
public class LifecycleService {
  /**
   * Initialize the build server.
   */
  public InitializeBuildResult buildInitialize(URI projectUri, 
      BuildTargetsManager buildTargetsManager) {
    GradleSourceSets sourceSets = GradleConnector.getGradleSourceSets(projectUri);
    buildTargetsManager.store(sourceSets);
    BuildServerCapabilities capabilities = initializeServerCapabilities();
    return new InitializeBuildResult(
        "gradle-build-server",
        "0.1.0",
        "2.1.0-M4",
        capabilities
    );
  }

  private BuildServerCapabilities initializeServerCapabilities() {
    BuildServerCapabilities capabilities = new BuildServerCapabilities();
    // TODO: add more capabilities
    return capabilities;
  }
}
