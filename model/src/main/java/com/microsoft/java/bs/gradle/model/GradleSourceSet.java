package com.microsoft.java.bs.gradle.model;

import java.io.File;

/**
 * Represents a source set in a Gradle project.
 */
public interface GradleSourceSet {

  /**
   * Equivalent to {@code org.gradle.api.Project.getName()}. 
   */
  public String getProjectName();

  /**
   * Equivalent to {@code org.gradle.api.Project.getPath()}.
   */
  public String getProjectPath();

  /**
   * Equivalent to {@code org.gradle.api.Project.getProjectDir()}.
   */
  public File getProjectDir();

  /**
   * Equivalent to {@code org.gradle.api.Project.getRootDir()}.
   */
  public File getRootDir();

  /**
   * Equivalent to {@code org.gradle.api.tasks.SourceSet.getName()}.
   */
  public String getSourceSetName();
}
