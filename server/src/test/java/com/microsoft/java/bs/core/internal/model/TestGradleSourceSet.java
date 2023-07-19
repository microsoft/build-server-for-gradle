package com.microsoft.java.bs.core.internal.model;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.GradleProjectDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;

/**
 * Test implementation of {@link GradleSourceSet}.
 */
public class TestGradleSourceSet implements GradleSourceSet {
  private String projectName;

  private String projectPath;

  private File projectDir;

  private File rootDir;

  private String sourceSetName;

  private Set<File> sourceDirs;

  private Set<File> generatedSourceDirs;

  private File sourceOutputDir;

  private Set<File> resourceDirs;

  private File resourceOutputDir;

  private File javaHome;

  private String javaVersion;

  private Set<GradleModuleDependency> moduleDependencies;

  private Set<GradleProjectDependency> projectDependencies;

  /**
   * Construct a default Gradle source set for testing purpose.
   */
  public TestGradleSourceSet() {
    this.projectDir = new File("test");
    this.rootDir = new File("test");
    this.sourceSetName = "main";
    this.sourceDirs = Collections.emptySet();
    this.generatedSourceDirs = Collections.emptySet();
    this.resourceDirs = Collections.emptySet();
    this.moduleDependencies = Collections.emptySet();
    this.projectDependencies = Collections.emptySet();
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
}
