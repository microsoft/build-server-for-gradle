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

  private String buildTreePath;

  private String sourceSetName;

  public DefaultBuildTargetDependency(String buildTreePath, String sourceSetName) {
    this.buildTreePath = buildTreePath;
    this.sourceSetName = sourceSetName;
  }

  public DefaultBuildTargetDependency(GradleSourceSet sourceSet) {
    this(sourceSet.getBuildTreePath(), sourceSet.getSourceSetName());
  }

  /**
   * Copy constructor.
   *
   * @param buildTargetDependency the other instance to copy from.
   */
  public DefaultBuildTargetDependency(BuildTargetDependency buildTargetDependency) {
    this.buildTreePath = buildTargetDependency.getBuildTreePath();
    this.sourceSetName = buildTargetDependency.getSourceSetName();
  }

  public String getBuildTreePath() {
    return buildTreePath;
  }

  public void setBuildTreePath(String buildTreePath) {
    this.buildTreePath = buildTreePath;
  }

  public String getSourceSetName() {
    return sourceSetName;
  }

  public void setSourceSetName(String sourceSetName) {
    this.sourceSetName = sourceSetName;
  }

  @Override
  public int hashCode() {
    return Objects.hash(buildTreePath, sourceSetName);
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
    return Objects.equals(buildTreePath, other.buildTreePath)
        && Objects.equals(sourceSetName, other.sourceSetName);
  }
}
