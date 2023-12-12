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

  private String projectPath;

  private String sourceSetName;

  public DefaultBuildTargetDependency(String projectPath, String sourceSetName) {
    this.projectPath = projectPath;
    this.sourceSetName = sourceSetName;
  }

  public DefaultBuildTargetDependency(GradleSourceSet sourceSet) {
    this(sourceSet.getProjectPath(), sourceSet.getSourceSetName());
  }


  /**
   * Copy constructor.
   *
   * @param buildTargetDependency the other instance to copy from.
   */
  public DefaultBuildTargetDependency(BuildTargetDependency buildTargetDependency) {
    this.projectPath = buildTargetDependency.getProjectPath();
    this.sourceSetName = buildTargetDependency.getSourceSetName();
  }

  public String getProjectPath() {
    return projectPath;
  }

  public void setProjectPath(String projectPath) {
    this.projectPath = projectPath;
  }

  public String getSourceSetName() {
    return sourceSetName;
  }

  public void setSourceSetName(String sourceSetName) {
    this.sourceSetName = sourceSetName;
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectPath, sourceSetName);
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
    return Objects.equals(projectPath, other.projectPath)
        && Objects.equals(sourceSetName, other.sourceSetName);
  }
}
