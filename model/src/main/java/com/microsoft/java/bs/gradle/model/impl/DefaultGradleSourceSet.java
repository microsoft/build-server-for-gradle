// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.BuildTargetDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;

/**
 * Default implementation of {@link GradleSourceSet}.
 */
public class DefaultGradleSourceSet implements GradleSourceSet {
  private static final long serialVersionUID = 1L;

  private String gradleVersion;

  private String displayName;

  private String projectName;

  private String projectPath;

  private File projectDir;

  private File rootDir;

  private String sourceSetName;

  private String classesTaskName;

  private Set<File> sourceDirs;

  private Set<File> generatedSourceDirs;

  private File sourceOutputDir;

  private Set<File> resourceDirs;

  private File resourceOutputDir;

  private List<File> compileClasspath;

  private Set<GradleModuleDependency> moduleDependencies;

  private Set<BuildTargetDependency> buildTargetDependencies;

  private boolean hasTests;

  private Map<String, Object> extensions;

  public DefaultGradleSourceSet() {}

  /**
   * Copy constructor.
   *
   * @param gradleSourceSet the source set to copy from.
   */
  public DefaultGradleSourceSet(GradleSourceSet gradleSourceSet) {
    this.gradleVersion = gradleSourceSet.getGradleVersion();
    this.displayName = gradleSourceSet.getDisplayName();
    this.projectName = gradleSourceSet.getProjectName();
    this.projectPath = gradleSourceSet.getProjectPath();
    this.projectDir = gradleSourceSet.getProjectDir();
    this.rootDir = gradleSourceSet.getRootDir();
    this.sourceSetName = gradleSourceSet.getSourceSetName();
    this.classesTaskName = gradleSourceSet.getClassesTaskName();
    this.sourceDirs = gradleSourceSet.getSourceDirs();
    this.generatedSourceDirs = gradleSourceSet.getGeneratedSourceDirs();
    this.sourceOutputDir = gradleSourceSet.getSourceOutputDir();
    this.resourceDirs = gradleSourceSet.getResourceDirs();
    this.resourceOutputDir = gradleSourceSet.getResourceOutputDir();
    this.compileClasspath = gradleSourceSet.getCompileClasspath();
    this.moduleDependencies = gradleSourceSet.getModuleDependencies().stream()
        .map(DefaultGradleModuleDependency::new).collect(Collectors.toSet());
    this.buildTargetDependencies = gradleSourceSet.getBuildTargetDependencies().stream()
        .map(DefaultBuildTargetDependency::new).collect(Collectors.toSet());
    this.hasTests = gradleSourceSet.hasTests();
    this.extensions = gradleSourceSet.getExtensions();
  }

  public String getGradleVersion() {
    return gradleVersion;
  }

  public void setGradleVersion(String gradleVersion) {
    this.gradleVersion = gradleVersion;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getProjectPath() {
    return projectPath;
  }

  public void setProjectPath(String projectPath) {
    this.projectPath = projectPath;
  }

  public File getProjectDir() {
    return projectDir;
  }

  public void setProjectDir(File projectDir) {
    this.projectDir = projectDir;
  }

  public File getRootDir() {
    return rootDir;
  }

  public void setRootDir(File rootDir) {
    this.rootDir = rootDir;
  }

  public String getSourceSetName() {
    return sourceSetName;
  }

  public void setSourceSetName(String sourceSetName) {
    this.sourceSetName = sourceSetName;
  }

  public String getClassesTaskName() {
    return classesTaskName;
  }

  public void setClassesTaskName(String classesTaskName) {
    this.classesTaskName = classesTaskName;
  }

  public Set<File> getSourceDirs() {
    return sourceDirs;
  }

  public void setSourceDirs(Set<File> sourceDirs) {
    this.sourceDirs = sourceDirs;
  }

  public Set<File> getGeneratedSourceDirs() {
    return generatedSourceDirs;
  }

  public void setGeneratedSourceDirs(Set<File> generatedSourceDirs) {
    this.generatedSourceDirs = generatedSourceDirs;
  }

  public File getSourceOutputDir() {
    return sourceOutputDir;
  }

  public void setSourceOutputDir(File sourceOutputDir) {
    this.sourceOutputDir = sourceOutputDir;
  }

  public Set<File> getResourceDirs() {
    return resourceDirs;
  }

  public void setResourceDirs(Set<File> resourceDirs) {
    this.resourceDirs = resourceDirs;
  }

  public File getResourceOutputDir() {
    return resourceOutputDir;
  }

  public void setResourceOutputDir(File resourceOutputDir) {
    this.resourceOutputDir = resourceOutputDir;
  }

  @Override
  public List<File> getCompileClasspath() {
    return compileClasspath;
  }

  public void setCompileClasspath(List<File> compileClasspath) {
    this.compileClasspath = compileClasspath;
  }

  public Set<GradleModuleDependency> getModuleDependencies() {
    return moduleDependencies;
  }

  public void setModuleDependencies(Set<GradleModuleDependency> moduleDependencies) {
    this.moduleDependencies = moduleDependencies;
  }

  public Set<BuildTargetDependency> getBuildTargetDependencies() {
    return buildTargetDependencies;
  }

  public void setBuildTargetDependencies(Set<BuildTargetDependency> buildTargetDependencies) {
    this.buildTargetDependencies = buildTargetDependencies;
  }

  public boolean hasTests() {
    return hasTests;
  }

  public void setHasTests(boolean hasTests) {
    this.hasTests = hasTests;
  }

  public Map<String, Object> getExtensions() {
    return extensions;
  }

  public void setExtensions(Map<String, Object> extensions) {
    this.extensions = extensions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(gradleVersion, displayName, projectName, projectPath,
        projectDir, rootDir, sourceSetName, classesTaskName, sourceDirs,
        generatedSourceDirs, sourceOutputDir, resourceDirs, resourceOutputDir,
        compileClasspath, moduleDependencies, buildTargetDependencies,
        hasTests, extensions);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DefaultGradleSourceSet other = (DefaultGradleSourceSet) obj;
    return Objects.equals(gradleVersion, other.gradleVersion)
        && Objects.equals(displayName, other.displayName)
        && Objects.equals(projectName, other.projectName)
        && Objects.equals(projectPath, other.projectPath)
        && Objects.equals(projectDir, other.projectDir)
        && Objects.equals(rootDir, other.rootDir)
        && Objects.equals(sourceSetName, other.sourceSetName)
        && Objects.equals(classesTaskName, other.classesTaskName)
        && Objects.equals(sourceDirs, other.sourceDirs)
        && Objects.equals(generatedSourceDirs, other.generatedSourceDirs)
        && Objects.equals(sourceOutputDir, other.sourceOutputDir)
        && Objects.equals(resourceDirs, other.resourceDirs)
        && Objects.equals(resourceOutputDir, other.resourceOutputDir)
        && Objects.equals(compileClasspath, other.compileClasspath)
        && Objects.equals(moduleDependencies, other.moduleDependencies)
        && Objects.equals(buildTargetDependencies, other.buildTargetDependencies)
        && hasTests == other.hasTests
        && Objects.equals(extensions, other.extensions);
  }
}
