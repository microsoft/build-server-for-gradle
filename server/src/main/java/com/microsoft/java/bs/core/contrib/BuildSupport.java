package com.microsoft.java.bs.core.contrib;

import java.net.URI;
import java.util.List;

import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTargets;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.StatusCode;

/**
 * Build Support interface.
 */
public interface BuildSupport {
  default boolean applies() {
    return true;
  }

  /**
   * Returns the source set entries for the given project uri.
   *
   * @param projectUri the project uri.
   */
  JavaBuildTargets getSourceSetEntries(URI projectUri);

  /**
   * Build the given targets.
   *
   * @param targets the targets to build.
   * @return the status code of the build.
   */
  StatusCode build(List<BuildTargetIdentifier> targets);

  /**
   * Clean the given targets. Corresponds to the 'buildTarget/cleanCache' request.
   *
   * @param targets the targets to clean.
   */
  boolean cleanCache(List<BuildTargetIdentifier> targets);
}
