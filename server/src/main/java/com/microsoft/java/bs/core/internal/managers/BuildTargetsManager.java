package com.microsoft.java.bs.core.internal.managers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetCapabilities;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.BuildTargetTag;

/**
 * Build targets manager.
 */
public class BuildTargetsManager {

  public BuildTargetsManager() {
    this.cache = new ConcurrentHashMap<>();
  }

  private ConcurrentHashMap<BuildTargetIdentifier, GradleBuildTarget> cache;

  /**
   * Store the Gradle source sets.
   */
  public synchronized void store(GradleSourceSets gradleSourceSets) {
    for (GradleSourceSet sourceSet : gradleSourceSets.getGradleSourceSets()) {
      String sourceSetName = sourceSet.getSourceSetName();
      URI uri = getBuildTargetUri(sourceSet.getProjectDir().toURI(), sourceSetName);
      List<String> tags = getBuildTargetTags(sourceSetName);
      BuildTargetIdentifier btId = new BuildTargetIdentifier(uri.toString());
      BuildTarget bt = new BuildTarget(
          btId,
          tags,
          Arrays.asList("java"),
          Collections.emptyList(),
          new BuildTargetCapabilities(
            true /* canCompile */,
            false /* canTest */,
            false /* canRun */,
            false /* canDebug */
          )
      );
      bt.setBaseDirectory(sourceSet.getRootDir().toURI().toString());
      GradleBuildTarget buildTarget = new GradleBuildTarget(bt, sourceSet);
      cache.put(btId, buildTarget);
    }
  }

  public synchronized GradleBuildTarget getGradleBuildTarget(BuildTargetIdentifier buildTargetId) {
    return cache.get(buildTargetId);
  }

  public synchronized List<GradleBuildTarget> getAllGradleBuildTargets() {
    return new ArrayList<>(cache.values());
  }

  private URI getBuildTargetUri(URI projectUri, String sourceSetName) {
    return URI.create(projectUri.toString() + "?sourceset=" + sourceSetName);
  }

  private List<String> getBuildTargetTags(String sourceSetName) {
    List<String> tags = new ArrayList<>();
    if (sourceSetName.toLowerCase().contains(BuildTargetTag.TEST)) {
      tags.add(BuildTargetTag.TEST);
    }
    return tags;
  }
}
