// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.managers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.gradle.model.GradleProjectDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetCapabilities;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.BuildTargetTag;
import ch.epfl.scala.bsp4j.extended.JvmBuildTargetEx;

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
   *
   * @return A list containing identifiers of changed build targets.
   */
  public List<BuildTargetIdentifier> store(GradleSourceSets gradleSourceSets) {
    Map<BuildTargetIdentifier, GradleBuildTarget> newCache = new HashMap<>();
    Map<String, BuildTargetIdentifier> projectPathToBuildTargetId = new HashMap<>();
    List<BuildTargetIdentifier> changedTargets = new LinkedList<>();
    for (GradleSourceSet sourceSet : gradleSourceSets.getGradleSourceSets()) {
      String sourceSetName = sourceSet.getSourceSetName();
      URI uri = getBuildTargetUri(sourceSet.getProjectDir().toURI(), sourceSetName);
      List<String> tags = getBuildTargetTags(sourceSet.hasTests());
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
      bt.setDisplayName(sourceSet.getDisplayName());

      setJvmBuildTarget(sourceSet, bt);

      GradleBuildTarget buildTarget = new GradleBuildTarget(bt, sourceSet);
      GradleBuildTarget existingTarget = cache.get(btId);
      // only compare the source set instance, which is the result
      // returned from the gradle plugin.
      if (existingTarget != null
          && !Objects.equals(existingTarget.getSourceSet(), buildTarget.getSourceSet())) {
        changedTargets.add(btId);
      }
      newCache.put(btId, buildTarget);
      // Store the relationship between the project path and the build target id.
      // 'test' and other source sets are ignored.
      if ("main".equals(sourceSet.getSourceSetName())) {
        projectPathToBuildTargetId.put(sourceSet.getProjectPath(), btId);
      }
    }
    updateBuildTargetDependencies(newCache.values(), projectPathToBuildTargetId);
    this.cache = newCache;
    return changedTargets;
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

  private List<String> getBuildTargetTags(boolean hasTests) {
    List<String> tags = new ArrayList<>();
    if (hasTests) {
      tags.add(BuildTargetTag.TEST);
    }
    return tags;
  }

  private void setJvmBuildTarget(GradleSourceSet sourceSet, BuildTarget bt) {
    // See: https://build-server-protocol.github.io/docs/extensions/jvm#jvmbuildtarget
    JvmBuildTargetEx jvmBuildTarget = new JvmBuildTargetEx(
        sourceSet.getJavaHome() == null ? "" : sourceSet.getJavaHome().toURI().toString(),
        sourceSet.getJavaVersion() == null ? "" : sourceSet.getJavaVersion(),
        sourceSet.getGradleVersion() == null ? "" : sourceSet.getGradleVersion(),
        sourceSet.getSourceCompatibility() == null ? "" : sourceSet.getSourceCompatibility(),
        sourceSet.getTargetCompatibility() == null ? "" : sourceSet.getTargetCompatibility()
    );
    bt.setDataKind("jvm");
    bt.setData(jvmBuildTarget);
  }

  /**
   * Iterate all the gradle build targets, and update their dependencies with
   * the help of 'project path to id' mapping.
   */
  private void updateBuildTargetDependencies(
      Collection<GradleBuildTarget> gradleBuildTargets,
      Map<String, BuildTargetIdentifier> projectPathToBuildTargetId
  ) {
    for (GradleBuildTarget gradleBuildTarget : gradleBuildTargets) {
      Set<GradleProjectDependency> projectDependencies =
          gradleBuildTarget.getSourceSet().getProjectDependencies();
      if (projectDependencies != null) {
        List<BuildTargetIdentifier> btDependencies = projectDependencies.stream()
            .map(projectDependency -> {
              String path = projectDependency.getProjectPath();
              return projectPathToBuildTargetId.get(path);
            })
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        gradleBuildTarget.getBuildTarget().setDependencies(btDependencies);
      }
    }
  }
}
