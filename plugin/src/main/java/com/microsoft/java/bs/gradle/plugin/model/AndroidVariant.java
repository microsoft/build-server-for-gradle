package com.microsoft.java.bs.gradle.plugin.model;

import com.microsoft.java.bs.gradle.model.BuildTargetDependency;
import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.LanguageExtension;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class AndroidVariant {

  private String gradleVersion;
  private String displayName;
  private String projectName;
  private String projectPath;
  private File projectDir;
  private File rootDir;
  private String variantName;
  private String classesTaskName;
  private String cleanTaskName;
  private Set<File> taskNames;
  private Set<File> sourceDirs;
  private Set<File> generatedSourceDirs;
  private File sourceOutputDir;
  private Set<File> resourceDirs;
  private File resourceOutputDir;
  private Set<File> compileClasspath;
  private Set<GradleModuleDependency> moduleDependencies;
  private Set<BuildTargetDependency> buildTargetDependencies;
  private boolean hasTests;
  private Map<String, LanguageExtension> extensions;

  public AndroidVariant() {
  }

  public AndroidVariant(
      String gradleVersion,
      String displayName,
      String projectName,
      String projectPath,
      File projectDir,
      File rootDir,
      String variantName,
      String classesTaskName,
      String cleanTaskName,
      Set<File> taskNames,
      Set<File> sourceDirs,
      Set<File> generatedSourceDirs,
      File sourceOutputDir,
      Set<File> resourceDirs,
      File resourceOutputDir,
      Set<File> compileClasspath,
      Set<GradleModuleDependency> moduleDependencies,
      Set<BuildTargetDependency> buildTargetDependencies,
      boolean hasTests,
      Map<String, LanguageExtension> extensions
  ) {
    this.gradleVersion = gradleVersion;
    this.displayName = displayName;
    this.projectName = projectName;
    this.projectPath = projectPath;
    this.projectDir = projectDir;
    this.rootDir = rootDir;
    this.variantName = variantName;
    this.classesTaskName = classesTaskName;
    this.cleanTaskName = cleanTaskName;
    this.taskNames = taskNames;
    this.sourceDirs = sourceDirs;
    this.generatedSourceDirs = generatedSourceDirs;
    this.sourceOutputDir = sourceOutputDir;
    this.resourceDirs = resourceDirs;
    this.resourceOutputDir = resourceOutputDir;
    this.compileClasspath = compileClasspath;
    this.moduleDependencies = moduleDependencies;
    this.buildTargetDependencies = buildTargetDependencies;
    this.hasTests = hasTests;
    this.extensions = extensions;
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

  public String getVariantName() {
    return variantName;
  }

  public void setVariantName(String variantName) {
    this.variantName = variantName;
  }

  public String getClassesTaskName() {
    return classesTaskName;
  }

  public void setClassesTaskName(String classesTaskName) {
    this.classesTaskName = classesTaskName;
  }

  public String getCleanTaskName() {
    return cleanTaskName;
  }

  public void setCleanTaskName(String cleanTaskName) {
    this.cleanTaskName = cleanTaskName;
  }

  public Set<File> getTaskNames() {
    return taskNames;
  }

  public void setTaskNames(Set<File> taskNames) {
    this.taskNames = taskNames;
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

  public Set<File> getCompileClasspath() {
    return compileClasspath;
  }

  public void setCompileClasspath(Set<File> compileClasspath) {
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

  public boolean isHasTests() {
    return hasTests;
  }

  public void setHasTests(boolean hasTests) {
    this.hasTests = hasTests;
  }

  public Map<String, LanguageExtension> getExtensions() {
    return extensions;
  }

  public void setExtensions(Map<String, LanguageExtension> extensions) {
    this.extensions = extensions;
  }

}
