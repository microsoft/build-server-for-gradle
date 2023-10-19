// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import java.util.Objects;

import com.microsoft.java.bs.gradle.model.GradleProjectDependency;

/**
 * Default implementation of {@link GradleProjectDependency}.
 */
public class DefaultGradleProjectDependency implements GradleProjectDependency {
  private static final long serialVersionUID = 1L;

  private String projectPath;

  private String file;

  public DefaultGradleProjectDependency(String projectPath) {
    this.projectPath = projectPath;
  }

  public DefaultGradleProjectDependency(String projectPath, String file) {
    this.projectPath = projectPath;
    this.file = file;
  }

  /**
   * Copy constructor.
   *
   * @param projectDependency the other instance to copy from.
   */
  public DefaultGradleProjectDependency(GradleProjectDependency projectDependency) {
    this.projectPath = projectDependency.getProjectPath();
  }

  public String getProjectPath() {
    return projectPath;
  }

  public void setProjectPath(String projectPath) {
    this.projectPath = projectPath;
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectPath, file);
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
    DefaultGradleProjectDependency other = (DefaultGradleProjectDependency) obj;
    return Objects.equals(projectPath, other.projectPath) && Objects.equals(file, other.file);
  }
}
