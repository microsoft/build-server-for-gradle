// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import java.io.File;
import java.util.Objects;

import com.microsoft.java.bs.gradle.model.GradleIncludedBuild;

/**
 * Default implementation of {@link GradleIncludedBuild}.
 */
public class DefaultGradleIncludedBuild implements GradleIncludedBuild {
  private static final long serialVersionUID = 1L;

  private String name;

  private File projectDir;

  public DefaultGradleIncludedBuild() {}

  public DefaultGradleIncludedBuild(String name, File projectDir) {
    this.name = name;
    this.projectDir = projectDir;
  }

  /**
   * Copy constructor.
   *
   * @param gradleIncludedBuild the included build set to copy from.
   */
  public DefaultGradleIncludedBuild(GradleIncludedBuild gradleIncludedBuild) {
    this(gradleIncludedBuild.getName(), gradleIncludedBuild.getProjectDir());
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public File getProjectDir() {
    return projectDir;
  }

  public void setProjectDir(File projectDir) {
    this.projectDir = projectDir;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, projectDir);
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
    DefaultGradleIncludedBuild other = (DefaultGradleIncludedBuild) obj;
    return Objects.equals(name, other.name)
        && Objects.equals(projectDir, other.projectDir);
  }
}
