// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.GradleProjectDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;

/**
 * Default implementation of {@link GradleSourceSet}.
 */
public class DefaultGradleSourceSet implements GradleSourceSet {
  private static final long serialVersionUID = 1L;

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

  private List<File> compileClasspath;

  private Set<File> resourceDirs;

  private File resourceOutputDir;

  private File javaHome;

  private String javaVersion;

  private String gradleVersion;

  private String sourceCompatibility;

  private String targetCompatibility;

  private Set<GradleModuleDependency> moduleDependencies;

  private Set<GradleProjectDependency> projectDependencies;

  private boolean hasTests;

  private boolean isJava;

  private List<String> javaCompilerArgs;

  private boolean isKotlin;

  private String kotlinLanguageVersion;

  private String kotlinApiVersion;

  private List<String> kotlincOptions;

  private List<String> kotlinAssociates;

  public DefaultGradleSourceSet() {}

  /**
   * Copy constructor.
   *
   * @param gradleSourceSet the source set to copy from.
   */
  public DefaultGradleSourceSet(GradleSourceSet gradleSourceSet) {
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
    this.compileClasspath = gradleSourceSet.getCompileClasspath();
    this.resourceDirs = gradleSourceSet.getResourceDirs();
    this.resourceOutputDir = gradleSourceSet.getResourceOutputDir();
    this.javaHome = gradleSourceSet.getJavaHome();
    this.javaVersion = gradleSourceSet.getJavaVersion();
    this.gradleVersion = gradleSourceSet.getGradleVersion();
    this.sourceCompatibility = gradleSourceSet.getSourceCompatibility();
    this.targetCompatibility = gradleSourceSet.getTargetCompatibility();
    this.moduleDependencies = gradleSourceSet.getModuleDependencies().stream()
        .map(DefaultGradleModuleDependency::new).collect(Collectors.toSet());
    this.projectDependencies = gradleSourceSet.getProjectDependencies().stream()
        .map(DefaultGradleProjectDependency::new).collect(Collectors.toSet());
    this.hasTests = gradleSourceSet.hasTests();
    this.isJava = gradleSourceSet.isJava();
    this.javaCompilerArgs = gradleSourceSet.getJavaCompilerArgs();
    this.isKotlin = gradleSourceSet.isKotlin();
    this.kotlinLanguageVersion = gradleSourceSet.getKotlinLanguageVersion();
    this.kotlinApiVersion = gradleSourceSet.getKotlinApiVersion();
    this.kotlincOptions = gradleSourceSet.getKotlincOptions();
    this.kotlinAssociates = gradleSourceSet.getKotlinAssociates();
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

  public List<File> getCompileClasspath() {
    return compileClasspath;
  }

  public void setCompileClasspath(List<File> compileClasspath) {
    this.compileClasspath = compileClasspath;
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

  public File getJavaHome() {
    return javaHome;
  }

  public void setJavaHome(File javaHome) {
    this.javaHome = javaHome;
  }

  public String getJavaVersion() {
    return javaVersion;
  }

  public void setJavaVersion(String javaVersion) {
    this.javaVersion = javaVersion;
  }

  public String getGradleVersion() {
    return gradleVersion;
  }

  public void setGradleVersion(String gradleVersion) {
    this.gradleVersion = gradleVersion;
  }

  public String getSourceCompatibility() {
    return sourceCompatibility;
  }

  public void setSourceCompatibility(String sourceCompatibility) {
    this.sourceCompatibility = sourceCompatibility;
  }

  public String getTargetCompatibility() {
    return targetCompatibility;
  }

  public void setTargetCompatibility(String targetCompatibility) {
    this.targetCompatibility = targetCompatibility;
  }

  public Set<GradleModuleDependency> getModuleDependencies() {
    return moduleDependencies;
  }

  public void setModuleDependencies(Set<GradleModuleDependency> moduleDependencies) {
    this.moduleDependencies = moduleDependencies;
  }

  public Set<GradleProjectDependency> getProjectDependencies() {
    return projectDependencies;
  }

  public void setProjectDependencies(Set<GradleProjectDependency> projectDependencies) {
    this.projectDependencies = projectDependencies;
  }

  public boolean hasTests() {
    return hasTests;
  }

  public void setHasTests(boolean hasTests) {
    this.hasTests = hasTests;
  }
  
  @Override
  public boolean isJava() {
    return isJava;
  }

  public void setIsJava(boolean isJava) {
    this.isJava = isJava;
  }

  public List<String> getJavaCompilerArgs() {
    return javaCompilerArgs;
  }

  public void setJavaCompilerArgs(List<String> javaCompilerArgs) {
    this.javaCompilerArgs = javaCompilerArgs;
  }
  
  @Override
  public boolean isKotlin() {
    return isJava;
  }

  public void setIsKotlin(boolean isKotlin) {
    this.isKotlin = isKotlin;
  }

  public String getKotlinLanguageVersion() {
    return kotlinLanguageVersion;
  }

  public void setKotlinLanguageVersion(String kotlinLanguageVersion) {
    this.kotlinLanguageVersion = kotlinLanguageVersion;
  }

  public String getKotlinApiVersion() {
    return kotlinApiVersion;
  }

  public void setKotlinApiVersion(String kotlinApiVersion) {
    this.kotlinApiVersion = kotlinApiVersion;
  }

  public List<String> getKotlincOptions() {
    return kotlincOptions;
  }

  public void setKotlincOptions(List<String> kotlincOptions) {
    this.kotlincOptions = kotlincOptions;
  }

  public List<String> getKotlinAssociates() {
    return kotlinAssociates;
  }

  public void setKotlinAssociates(List<String> kotlinAssociates) {
    this.kotlinAssociates = kotlinAssociates;
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayName, projectName, projectPath, projectDir,
        rootDir, sourceSetName, classesTaskName, sourceDirs,
        generatedSourceDirs, sourceOutputDir, compileClasspath,
        resourceDirs, resourceOutputDir, javaHome, javaVersion,
        gradleVersion, sourceCompatibility, targetCompatibility,
        moduleDependencies, projectDependencies, hasTests,
        isJava, javaCompilerArgs, isKotlin, kotlinLanguageVersion,
        kotlinApiVersion, kotlincOptions, kotlinAssociates);
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
    return Objects.equals(displayName, other.displayName)
        && Objects.equals(projectName, other.projectName)
        && Objects.equals(projectPath, other.projectPath)
        && Objects.equals(projectDir, other.projectDir)
        && Objects.equals(rootDir, other.rootDir)
        && Objects.equals(sourceSetName, other.sourceSetName)
        && Objects.equals(classesTaskName, other.classesTaskName)
        && Objects.equals(sourceDirs, other.sourceDirs)
        && Objects.equals(generatedSourceDirs, other.generatedSourceDirs)
        && Objects.equals(sourceOutputDir, other.sourceOutputDir)
        && Objects.equals(compileClasspath, other.compileClasspath)
        && Objects.equals(resourceDirs, other.resourceDirs)
        && Objects.equals(resourceOutputDir, other.resourceOutputDir)
        && Objects.equals(javaHome, other.javaHome)
        && Objects.equals(javaVersion, other.javaVersion)
        && Objects.equals(gradleVersion, other.gradleVersion)
        && Objects.equals(sourceCompatibility, other.sourceCompatibility)
        && Objects.equals(targetCompatibility, other.targetCompatibility)
        && Objects.equals(moduleDependencies, other.moduleDependencies)
        && Objects.equals(projectDependencies, other.projectDependencies)
        && hasTests == other.hasTests
        && isJava == other.isJava
        && Objects.equals(javaCompilerArgs, other.javaCompilerArgs)
        && isKotlin == other.isKotlin
        && Objects.equals(kotlinLanguageVersion, other.kotlinLanguageVersion)
        && Objects.equals(kotlinApiVersion, other.kotlinApiVersion)
        && Objects.equals(kotlincOptions, other.kotlincOptions)
        && Objects.equals(kotlinAssociates, other.kotlinAssociates);
  }
}
