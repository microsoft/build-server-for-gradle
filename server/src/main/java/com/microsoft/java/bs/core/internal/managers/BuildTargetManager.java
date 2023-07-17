package com.microsoft.java.bs.core.internal.managers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetCapabilities;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.BuildTargetTag;
import ch.epfl.scala.bsp4j.JvmBuildTarget;

/**
 * Build targets manager.
 */
public class BuildTargetManager {

  private volatile Map<BuildTargetIdentifier, GradleBuildTarget> cache;

  public BuildTargetManager() {
    this.cache = new HashMap<>();
  }

  /**
   * Store the Gradle source sets.
   */
  public void store(GradleSourceSets gradleSourceSets) {
    Map<BuildTargetIdentifier, GradleBuildTarget> newCache = new HashMap<>();
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

      // See: https://build-server-protocol.github.io/docs/extensions/jvm#jvmbuildtarget
      JvmBuildTarget jvmBuildTarget = new JvmBuildTarget(
          sourceSet.getJavaHome() == null ? "" : sourceSet.getJavaHome().toURI().toString(),
          sourceSet.getJavaVersion() == null ? "" : sourceSet.getJavaVersion()
      );
      bt.setDataKind("jvm");
      bt.setData(jvmBuildTarget);

      GradleBuildTarget buildTarget = new GradleBuildTarget(bt, sourceSet);
      newCache.put(btId, buildTarget);
    }
    this.cache = newCache;
  }

  public GradleBuildTarget getGradleBuildTarget(BuildTargetIdentifier buildTargetId) {
    return cache.get(buildTargetId);
  }

  public List<GradleBuildTarget> getAllGradleBuildTargets() {
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
