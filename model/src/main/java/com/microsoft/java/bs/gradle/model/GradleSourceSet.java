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
   * Module dependencies.
   */
  public Set<GradleModuleDependency> getModuleDependencies();

  /**
   * Project dependencies.
   */
  public Set<GradleProjectDependency> getProjectDependencies();

  /**
   * has tests defined.
   */
  public boolean hasTests();

  /**
   * Is the source set language Java.
   */
  public boolean isJava();

  /**
   * The list of Java compiler arguments.
   */
  public List<String> getJavaCompilerArgs();

  /**
   * Is the source set language Kotlin.
   */
  public boolean isKotlin();

  /**
   * The list of Kotlin compiler options.
   */
  public List<String> getKotlincOptions();
  
  /**
   * The Kotlin language version.
   */
  public String getKotlinLanguageVersion();
  
  /**
   * The Kotlin API version.
   */
  public String getKotlinApiVersion();

  /**
   * The list of Kotlin associates.
   */
  public List<String> getKotlinAssociates();
}
