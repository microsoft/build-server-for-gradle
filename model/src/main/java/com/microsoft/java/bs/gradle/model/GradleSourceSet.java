package com.microsoft.java.bs.gradle.model;

import java.io.File;
import java.util.Set;

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

  /**
   * The source directories of this source set.
   */
  public Set<File> getSourceDirs();

  /**
   * The generated source directories, including the ones generated by annotation processors,
   * and the ones that is inferred as generated source directories.
   */
  public Set<File> getGeneratedSourceDirs();

  /**
   * The resource directories of this source set.
   */
  public Set<File> getResourceDirs();

  /**
   * The output directory of this source set.
   */
  public File getSourceOutputDir();

  /**
   * The resource output directory of this source set.
   */
  public File getResourceOutputDir();

  /**
   * The JDK platform of this source set.
   */
  public JdkPlatform getJdkPlatform();
}
