// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Represents a source set in a Gradle project.
 */
public interface GradleSourceSet extends Serializable {

  /**
   * A unique name for this project/sourceSet combination.
   */
  public String getDisplayName();

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
   * The name of the classes task.
   */
  public String getClassesTaskName();

  /**
   * The name of the clean task.
   */
  public String getCleanTaskName();

  /**
   * All the tasks relevant to compiling this source set.
   */
  public Set<String> getTaskNames();

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
   * The compile classpath for this source set.
   */
  public List<File> getCompileClasspath();

  /**
   * JDK home file location.
   */
  public File getJavaHome();

  /**
   * The java version this target is supposed to use.
   */
  public String getJavaVersion();

  /**
   * The Gradle version of the project.
   */
  public String getGradleVersion();

  /**
   * The source compatibility of the source set.
   */
  public String getSourceCompatibility();

  /**
   * The target compatibility of the source set.
   */
  public String getTargetCompatibility();

  /**
   * The list of compiler arguments.
   */
  public List<String> getCompilerArgs();

  /**
   * Module dependencies.
   */
  public Set<GradleModuleDependency> getModuleDependencies();

  /**
   * Project dependencies.
   */
  public Set<BuildTargetDependency> getBuildTargetDependencies();

  /**
   * has tests defined.
   */
  public boolean hasTests();
}
