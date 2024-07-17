// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import java.util.Objects;

import com.microsoft.java.bs.gradle.model.BuildTargetDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;

/**
 * Default implementation of {@link BuildTargetDependency}.
 */
public class DefaultBuildTargetDependency implements BuildTargetDependency {
  private static final long serialVersionUID = 1L;

  private String projectDir;

  private String sourceSetName;

  public DefaultBuildTargetDependency(String projectDir, String sourceSetName) {
    this.projectDir = projectDir;
    this.sourceSetName = sourceSetName;
  }

  public DefaultBuildTargetDependency(GradleSourceSet sourceSet) {
    this(sourceSet.getProjectDir().getAbsolutePath(), sourceSet.getSourceSetName());
  }

  /**
   * Copy constructor.
   *
   * @param buildTargetDependency the other instance to copy from.
   */
  public DefaultBuildTargetDependency(BuildTargetDependency buildTargetDependency) {
    this.projectDir = buildTargetDependency.getProjectDir();
    this.sourceSetName = buildTargetDependency.getSourceSetName();
  }

  @Override
  public String getProjectDir() {
    return projectDir;
  }

  public void setProjectDir(String projectDir) {
    this.projectDir = projectDir;
  }

  @Override
  public String getSourceSetName() {
    return sourceSetName;
  }

  public void setSourceSetName(String sourceSetName) {
    this.sourceSetName = sourceSetName;
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectDir, sourceSetName);
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
    DefaultBuildTargetDependency other = (DefaultBuildTargetDependency) obj;
    return Objects.equals(projectDir, other.projectDir)
        && Objects.equals(sourceSetName, other.sourceSetName);
  }
}
