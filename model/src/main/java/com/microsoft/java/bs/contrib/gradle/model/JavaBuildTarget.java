package com.microsoft.java.bs.contrib.gradle.model;

import java.io.File;
import java.util.Set;

/**
 * Represents a Java build target.
 */
public interface JavaBuildTarget {
  public String getProjectName();

  public String getModulePath();

  public File getProjectDir();

  public File getRootDir();

  public Set<File> getSourceDirs();

  public File getSourceOutputDir();

  public Set<File> getResourceDirs();

  public File getResourceOutputDirs();

  public File getApGeneratedDir();

  public Set<File> getOptionalSourceDirs();

  public Set<ModuleDependency> getModuleDependencies();

  public Set<ProjectDependency> getProjectDependencies();

  public String getSourceSetName();

  public JdkPlatform getJdkPlatform();
}
