// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.managers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ch.epfl.scala.bsp4j.JvmBuildTarget;
import ch.epfl.scala.bsp4j.ScalaBuildTarget;
import ch.epfl.scala.bsp4j.ScalaPlatform;
import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.gradle.model.BuildTargetDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.JavaExtension;
import com.microsoft.java.bs.gradle.model.ScalaExtension;
import com.microsoft.java.bs.gradle.model.SupportedLanguages;
import com.microsoft.java.bs.gradle.model.impl.DefaultBuildTargetDependency;

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
    Map<BuildTargetDependency, BuildTargetIdentifier> dependencyToBuildTargetId = new HashMap<>();
    List<BuildTargetIdentifier> changedTargets = new LinkedList<>();
    for (GradleSourceSet sourceSet : gradleSourceSets.getGradleSourceSets()) {
      String sourceSetName = sourceSet.getSourceSetName();
      URI uri = getBuildTargetUri(sourceSet.getProjectDir().toURI(), sourceSetName);
      List<String> tags = getBuildTargetTags(sourceSet.hasTests());
      BuildTargetIdentifier btId = new BuildTargetIdentifier(uri.toString());
      List<String> languages = new LinkedList<>(sourceSet.getExtensions().keySet());
      BuildTargetCapabilities buildTargetCapabilities = new BuildTargetCapabilities();
      buildTargetCapabilities.setCanCompile(true);
      buildTargetCapabilities.setCanTest(true);
      BuildTarget bt = new BuildTarget(
          btId,
          tags,
          languages,
          Collections.emptyList(),
          buildTargetCapabilities
      );
      bt.setBaseDirectory(sourceSet.getRootDir().toURI().toString());
      bt.setDisplayName(sourceSet.getDisplayName());

      setBuildTarget(sourceSet, bt);

      GradleBuildTarget buildTarget = new GradleBuildTarget(bt, sourceSet);
      GradleBuildTarget existingTarget = cache.get(btId);
      // only compare the source set instance, which is the result
      // returned from the gradle plugin.
      if (existingTarget != null
          && !Objects.equals(existingTarget.getSourceSet(), buildTarget.getSourceSet())) {
        changedTargets.add(btId);
      }
      newCache.put(btId, buildTarget);
      // Store the relationship between the project/sourceset and the build target id.
      BuildTargetDependency dependency = new DefaultBuildTargetDependency(sourceSet);
      dependencyToBuildTargetId.put(dependency, btId);
    }
    updateBuildTargetDependencies(newCache.values(), dependencyToBuildTargetId);
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

  private void setBuildTarget(GradleSourceSet sourceSet, BuildTarget bt) {
    ScalaExtension scalaExtension = SupportedLanguages.SCALA.getExtension(sourceSet);
    JavaExtension javaExtension = SupportedLanguages.JAVA.getExtension(sourceSet);
    if (scalaExtension != null) {
      setScalaBuildTarget(sourceSet, scalaExtension, javaExtension, bt);
    } else if (javaExtension != null) {
      setJvmBuildTarget(sourceSet, javaExtension, bt);
    }
  }

  private JvmBuildTarget getJvmBuildTarget(GradleSourceSet sourceSet, JavaExtension javaExtension) {
    // See: https://build-server-protocol.github.io/docs/extensions/jvm#jvmbuildtarget
    return new JvmBuildTargetEx(
        javaExtension.getJavaHome() == null ? "" : javaExtension.getJavaHome().toURI().toString(),
        javaExtension.getJavaVersion() == null ? "" : javaExtension.getJavaVersion(),
        sourceSet.getGradleVersion() == null ? "" : sourceSet.getGradleVersion(),
        javaExtension.getSourceCompatibility() == null ? ""
            : javaExtension.getSourceCompatibility(),
        javaExtension.getTargetCompatibility() == null ? ""
            : javaExtension.getTargetCompatibility()
    );
  }

  private void setJvmBuildTarget(GradleSourceSet sourceSet, JavaExtension javaExtension,
      BuildTarget bt) {
    bt.setDataKind("jvm");
    bt.setData(getJvmBuildTarget(sourceSet, javaExtension));
  }

  private ScalaBuildTarget getScalaBuildTarget(GradleSourceSet sourceSet,
      ScalaExtension scalaExtension, JavaExtension javaExtension) {
    // See: https://build-server-protocol.github.io/docs/extensions/scala#scalabuildtarget
    JvmBuildTarget jvmBuildTarget = getJvmBuildTarget(sourceSet, javaExtension);
    List<String> scalaJars = scalaExtension.getScalaJars().stream()
          .map(file -> file.toURI().toString())
          .collect(Collectors.toList());
    ScalaBuildTarget scalaBuildTarget = new ScalaBuildTarget(
        scalaExtension.getScalaOrganization() == null ? "" : scalaExtension.getScalaOrganization(),
        scalaExtension.getScalaVersion() == null ? "" : scalaExtension.getScalaVersion(),
        scalaExtension.getScalaBinaryVersion() == null ? ""
            : scalaExtension.getScalaBinaryVersion(),
        ScalaPlatform.JVM,
        scalaJars
    );
    scalaBuildTarget.setJvmBuildTarget(jvmBuildTarget);
    return scalaBuildTarget;
  }

  private void setScalaBuildTarget(GradleSourceSet sourceSet, ScalaExtension scalaExtension,
      JavaExtension javaExtension, BuildTarget bt) {
    bt.setDataKind("scala");
    bt.setData(getScalaBuildTarget(sourceSet, scalaExtension, javaExtension));
  }

  /**
   * Iterate all the gradle build targets, and update their dependencies with
   * the help of 'build target to id' mapping.
   */
  private void updateBuildTargetDependencies(
      Collection<GradleBuildTarget> gradleBuildTargets,
      Map<BuildTargetDependency, BuildTargetIdentifier> dependencyToBuildTargetId
  ) {
    for (GradleBuildTarget gradleBuildTarget : gradleBuildTargets) {
      Set<BuildTargetDependency> buildTargetDependencies =
          gradleBuildTarget.getSourceSet().getBuildTargetDependencies();
      if (buildTargetDependencies != null) {
        List<BuildTargetIdentifier> btDependencies = buildTargetDependencies.stream()
            .map(dependencyToBuildTargetId::get)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        gradleBuildTarget.getBuildTarget().setDependencies(btDependencies);
      }
    }
  }
}
