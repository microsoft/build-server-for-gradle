// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.microsoft.java.bs.gradle.model.GradleIncludedBuild;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

/**
 * Default implementation of {@link GradleSourceSets}.
 */
public class DefaultGradleSourceSets implements GradleSourceSets {
  private static final long serialVersionUID = 1L;

  private List<GradleIncludedBuild> gradleIncludedBuilds;

  private List<GradleSourceSet> gradleSourceSets;

  public DefaultGradleSourceSets(List<GradleIncludedBuild> gradleIncludedBuilds,
      List<GradleSourceSet> gradleSourceSets) {
    this.gradleIncludedBuilds = gradleIncludedBuilds;
    this.gradleSourceSets = gradleSourceSets;
  }

  /**
   * Copy constructor.
   */
  public DefaultGradleSourceSets(GradleSourceSets sourceSets) {
    this(sourceSets.getGradleIncludedBuilds().stream()
        .map(DefaultGradleIncludedBuild::new).collect(Collectors.toList()),
         sourceSets.getGradleSourceSets().stream()
        .map(DefaultGradleSourceSet::new).collect(Collectors.toList()));
  }

  @Override
  public List<GradleIncludedBuild> getGradleIncludedBuilds() {
    return gradleIncludedBuilds;
  }

  public void setGradleIncludedBuilds(List<GradleIncludedBuild> gradleIncludedBuilds) {
    this.gradleIncludedBuilds = gradleIncludedBuilds;
  }

  @Override
  public List<GradleSourceSet> getGradleSourceSets() {
    return gradleSourceSets;
  }

  public void setGradleSourceSets(List<GradleSourceSet> gradleSourceSets) {
    this.gradleSourceSets = gradleSourceSets;
  }

  @Override
  public int hashCode() {
    return Objects.hash(gradleIncludedBuilds, gradleSourceSets);
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
    DefaultGradleSourceSets other = (DefaultGradleSourceSets) obj;
    return Objects.equals(gradleIncludedBuilds, other.gradleIncludedBuilds)
        && Objects.equals(gradleSourceSets, other.gradleSourceSets);
  }
}
