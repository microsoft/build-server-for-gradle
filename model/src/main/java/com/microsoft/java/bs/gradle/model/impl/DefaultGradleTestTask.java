// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.microsoft.java.bs.gradle.model.GradleTestTask;

/**
 * Contains test information relating to a Gradle Test task.
 */
public class DefaultGradleTestTask implements GradleTestTask {

  private static final long serialVersionUID = 1L;

  private final String taskPath;
  private final List<File> classpath;
  private final List<String> jvmOptions;
  private final File workingDirectory;
  private final Map<String, String> environmentVariables;

  /**
   * Initialize the test information.
   */
  public DefaultGradleTestTask(String taskPath, List<File> classpath,
      List<String> jvmOptions, File workingDirectory,
      Map<String, String> environmentVariables) {
    this.taskPath = taskPath;
    this.classpath = classpath;
    this.jvmOptions = jvmOptions;
    this.workingDirectory = workingDirectory;
    this.environmentVariables = environmentVariables;
  }

  /**
   * copy constructor.
   */
  public DefaultGradleTestTask(GradleTestTask gradleTestTask) {
    this.taskPath = gradleTestTask.getTaskPath();
    this.classpath = gradleTestTask.getClasspath();
    this.jvmOptions = gradleTestTask.getJvmOptions();
    this.workingDirectory = gradleTestTask.getWorkingDirectory();
    this.environmentVariables = gradleTestTask.getEnvironmentVariables();
  }

  @Override
  public String getTaskPath() {
    return taskPath;
  }

  @Override
  public List<File> getClasspath() {
    return classpath;
  }

  @Override
  public List<String> getJvmOptions() {
    return jvmOptions;
  }

  @Override
  public File getWorkingDirectory() {
    return workingDirectory;
  }

  @Override
  public Map<String, String> getEnvironmentVariables() {
    return environmentVariables;
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(taskPath, classpath, jvmOptions, workingDirectory,
        environmentVariables);
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
    DefaultGradleTestTask other = (DefaultGradleTestTask) obj;
    return Objects.equals(taskPath, other.taskPath)
        && Objects.equals(classpath, other.classpath)
        && Objects.equals(jvmOptions, other.jvmOptions)
        && Objects.equals(workingDirectory, other.workingDirectory)
        && Objects.equals(environmentVariables, other.environmentVariables);
  }
}
