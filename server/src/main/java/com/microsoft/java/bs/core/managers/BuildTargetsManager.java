package com.microsoft.java.bs.core.managers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTarget;
import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTargets;
import com.microsoft.java.bs.contrib.gradle.model.JdkPlatform;
import com.microsoft.java.bs.contrib.gradle.model.ProjectDependency;
import com.microsoft.java.bs.core.bsp.BuildServerStatus;
import com.microsoft.java.bs.core.contrib.BuildSupport;
import com.microsoft.java.bs.core.model.BuildTargetComponents;
import com.microsoft.java.bs.core.model.JvmBuildTargetExt;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetCapabilities;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.BuildTargetTag;

/**
 * Build target manager.
 */
public class BuildTargetsManager {

  private ConcurrentHashMap<BuildTargetIdentifier, BuildTargetComponents> cache;

  @Inject
  Set<BuildSupport> buildSupports;

  @Inject
  BuildServerStatus buildServerStatus;

  public BuildTargetsManager() {
    this.cache = new ConcurrentHashMap<>();
  }

  /**
   * Initialize the build target manager.
   */
  public void initialize() {
    for (BuildSupport buildSupport : buildSupports) {
      if (!buildSupport.applies()) {
        continue;
      }
      parseSourceSetEntries(buildSupport.getSourceSetEntries(buildServerStatus.getRootUri()));
    }
  }

  public void reset() {
    cache.clear();
    initialize();
  }

  public Collection<BuildTarget> getBuildTargets() {
    return cache.values().stream().map(BuildTargetComponents::getBuildTarget)
      .collect(Collectors.toList());
  }

  public BuildTargetComponents getComponents(BuildTargetIdentifier id) {
    return cache.get(id);
  }

  private void parseSourceSetEntries(JavaBuildTargets javaBuildTargets) {
    if (javaBuildTargets == null) {
      throw new IllegalStateException("Build Target is null when parsing source set entries.");
    }

    Map<String, BuildTargetIdentifier> projectNameToBuildTargetId = new HashMap<>();
    Map<BuildTargetIdentifier, Set<String>> projectDependencies = new HashMap<>();
    for (JavaBuildTarget javaBuildTarget : javaBuildTargets.getJavaBuildTargets()) {
      String sourceSetName = javaBuildTarget.getSourceSetName();
      URI uri = getBuildTargetUri(javaBuildTarget.getProjectDir().toURI(), sourceSetName);
      List<String> tags = getBuildTargetTags(sourceSetName);

      BuildTargetIdentifier btId = new BuildTargetIdentifier(uri.toString());
      BuildTarget bt = new BuildTarget(
          btId,
          tags,
          Arrays.asList("java"),
          Collections.emptyList(), //TODO: test depends on main
          new BuildTargetCapabilities(
            true,
            false,
            false,
            false
          )
      );
      bt.setBaseDirectory(javaBuildTarget.getRootDir().toURI().toString());
      JdkPlatform jdkPlatform = javaBuildTarget.getJdkPlatform();
      if (jdkPlatform != null) {
        JvmBuildTargetExt jvmBuildTarget = new JvmBuildTargetExt(
            jdkPlatform.getJavaHome().getAbsolutePath(),
            jdkPlatform.getJavaVersion()
        );
        jvmBuildTarget.setSourceLanguageLevel(jdkPlatform.getSourceLanguageLevel());
        jvmBuildTarget.setTargetBytecodeVersion(jdkPlatform.getTargetBytecodeVersion());
        bt.setDataKind("jvm");
        bt.setData(jvmBuildTarget);
      }
      BuildTargetComponents components = new BuildTargetComponents();
      components.setBuildTarget(bt);
      components.setModulePath(javaBuildTarget.getModulePath());
      components.setSourceDirs(javaBuildTarget.getSourceDirs());
      components.setSourceOutputDir(javaBuildTarget.getSourceOutputDir());
      components.setResourceDirs(javaBuildTarget.getResourceDirs());
      components.setResourceOutputDir(javaBuildTarget.getResourceOutputDirs());
      components.setApGeneratedDir(javaBuildTarget.getApGeneratedDir());
      components.setModuleDependencies(javaBuildTarget.getModuleDependencies());
      cache.put(btId, components);

      // first cache the relationship between project name and build target id,
      // then update the build target dependencies after the loop
      if ("main".equals(javaBuildTarget.getSourceSetName())) {
        projectNameToBuildTargetId.put(javaBuildTarget.getProjectName(), btId);
      }

      Set<String> dependencyProjectNames = javaBuildTarget.getProjectDependencies()
          .stream()
          .map(ProjectDependency::getProjectName)
          .collect(Collectors.toSet());
      projectDependencies.put(btId, dependencyProjectNames);
    }

    updateBuildTargetDependencies(projectNameToBuildTargetId, projectDependencies);
  }

  private void updateBuildTargetDependencies(
      Map<String, BuildTargetIdentifier> projectNameToBuildTargetId,
      Map<BuildTargetIdentifier, Set<String>> projectDependencies
  ) {
    for (Map.Entry<BuildTargetIdentifier, Set<String>> entry : projectDependencies.entrySet()) {
      Set<String> dependencyNames = entry.getValue();
      List<BuildTargetIdentifier> dependencies = dependencyNames.stream()
          .map(projectNameToBuildTargetId::get)
          .collect(Collectors.toList());
      BuildTargetComponents components = cache.get(entry.getKey());
      components.getBuildTarget().setDependencies(dependencies);
    }
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
